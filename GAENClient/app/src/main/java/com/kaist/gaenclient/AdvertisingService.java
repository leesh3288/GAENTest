package com.kaist.gaenclient;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.Nullable;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import static com.kaist.gaenclient.Utils.parseUuidFrom;

public class AdvertisingService extends Service {
    protected static final String TAG = "AdvertisingActivity";
//    private static final int ADVERTISE_MODE_BALANCED = 1;  // ~250ms
    private BeaconTransmitter beaconTransmitter = null;
    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartFailure(int errorCode) {
            Log.e(TAG, "Advertisement start failed with code: " + errorCode);
        }
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "Advertisement start succeeded.");
        }
    };

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        // Check if bluetooth is enabled
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            // Request user to enable it
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            stopSelf();
            return;
        }

        // Check low energy support
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            // Get a newer device
            Log.d(TAG,"No LE Support.");
            stopSelf();
            return;
        }

        // Check advertising
        if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            // Unable to run the server on this device, get a better device
            Log.d(TAG,"No Advertising Support.");
            stopSelf();
            return;
        }

        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();

//        Context ctx = getApplicationContext();
//        SharedPreferences sharedPref = ctx.getSharedPreferences("com.kaist.gaenclient.prefs",
//                Context.MODE_PRIVATE);
//        String RPIUuid = sharedPref.getString("RPIUuid", "");
//        if (Objects.equals(RPIUuid, "")) {
//            RPIUuid = UUID.randomUUID().toString();
//            SharedPreferences.Editor editor = sharedPref.edit();
//            editor.putString("RPIUuid", RPIUuid);
//            editor.apply();
//        }
//
//        Log.i(TAG, "Advertising with RPIUuid: " + RPIUuid);
//
//        Beacon beacon = new Beacon.Builder()
//                .setId1(RPIUuid).setDataFields(Arrays.asList(0x13371337L)).build();
//        BeaconParser beaconParser = new BeaconParser()
//                .setBeaconLayout("s:0-1=fd6f,p:-:-59,i:2-17,d:18-21");
//        beaconTransmitter = new BeaconTransmitter(ctx, beaconParser);
//        beaconTransmitter.setAdvertiseMode(ADVERTISE_MODE_BALANCED);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        beaconTransmitter.stopAdvertising();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startAdvertising() {
        if (mBluetoothLeAdvertiser == null) {
            return;
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(Config.advertiseMode)
                .setConnectable(true)   // NOTE: This is set to true because we want connection (thus adds a flag)
                .setTimeout(0)
                .setTxPowerLevel(Config.advertiseTxPower)
                .build();

        // Service data payload, as in GAEN protocol
        byte[] RPI = new byte[16], AEM = new byte[4];
        byte[] advertisingBytes = new byte[20];
        // Exposure Notification Service UUID: 0xfd6f
        byte[] serviceUuidBytes = new byte[] { (byte)0x6f, (byte)0xfd };

        // TEST: RPI as "01020304-0506-0708-090a-0b0c0d0e0f10"
        // TEST: AEM as 0xc001caf3
        for (int i = 0; i < RPI.length; i++)
            RPI[i] = (byte)(i + 1);
        AEM[0] = (byte)0xc0; AEM[1] = (byte)0x01;
        AEM[2] = (byte)0xca; AEM[3] = (byte)0xf3;

        for (int i = 0; i < RPI.length; i++)
            advertisingBytes[i] = RPI[i];
        for (int i = 0; i < AEM.length; i++)
            advertisingBytes[i + 16] = AEM[i];

        ParcelUuid parcelUuid = parseUuidFrom(serviceUuidBytes);
        AdvertiseData data = new AdvertiseData.Builder()
                .addServiceData(parcelUuid, advertisingBytes)
                .addServiceUuid(parcelUuid)
                .setIncludeTxPowerLevel(false)  // txPower in AEM
                .setIncludeDeviceName(false)
                .build();

        Log.i(TAG,"This is the advertising data");
        Log.i(TAG, data.getServiceData().toString());
        Log.i(TAG, data.toString());

        mBluetoothLeAdvertiser.startAdvertising(settings, data, advertiseCallback);
    }

    private void stopAdvertising() {
        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
        }
    }
}
