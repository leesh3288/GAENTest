package org.altbeacon.beaconreference;

import android.app.Service;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;

import java.util.Arrays;

public class AdvertisingService extends Service {
    protected static final String TAG = "AdvertisingActivity";
    private BeaconTransmitter beaconTransmitter = null;
    String uuidString = "01020304-0506-0708-090a-0b0c0d0e0f10";
    Beacon beacon = new Beacon.Builder()
            .setId1(uuidString).setDataFields(Arrays.asList(0x13371337L)).build();
    BeaconParser beaconParser = new BeaconParser()
            .setBeaconLayout("s:0-1=fd6f,p:-:-59,i:2-17,d:18-21");

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        beaconTransmitter = new BeaconTransmitter(getApplicationContext(), beaconParser);
        beaconTransmitter.startAdvertising(beacon, new AdvertiseCallback() {
            @Override
            public void onStartFailure(int errorCode) {
                Log.e(TAG, "Advertisement start failed with code: " + errorCode);
            }
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.i(TAG, "Advertisement start succeeded.");
            }
        });
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
}
