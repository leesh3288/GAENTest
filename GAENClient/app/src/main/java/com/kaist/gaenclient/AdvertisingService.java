package com.kaist.gaenclient;

import android.app.Service;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public class AdvertisingService extends Service {
    protected static final String TAG = "AdvertisingActivity";
    private BeaconTransmitter beaconTransmitter = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        Context ctx = getApplicationContext();
        SharedPreferences sharedPref = ctx.getSharedPreferences("com.kaist.gaenclient.prefs",
                Context.MODE_PRIVATE);
        String RPIUuid = sharedPref.getString("RPIUuid", "");
        if (Objects.equals(RPIUuid, "")) {
            RPIUuid = UUID.randomUUID().toString();
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("RPIUuid", RPIUuid);
            editor.apply();
        }

        Log.i(TAG, "Advertising with RPIUuid: " + RPIUuid);

        Beacon beacon = new Beacon.Builder()
                .setId1(RPIUuid).setDataFields(Arrays.asList(0x13371337L)).build();
        BeaconParser beaconParser = new BeaconParser()
                .setBeaconLayout("s:0-1=fd6f,p:-:-59,i:2-17,d:18-21");
        beaconTransmitter = new BeaconTransmitter(ctx, beaconParser);
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
