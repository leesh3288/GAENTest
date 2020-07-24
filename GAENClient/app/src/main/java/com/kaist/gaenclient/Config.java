package com.kaist.gaenclient;

import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.ScanSettings;
import android.os.ParcelUuid;

public class Config {
    public static long SCAN_PERIOD = 5*1000; //5 * 60 * 1000;  // 5 minutes
    public static long SCAN_DURATION = 3*1000; // 8 * 1000;     // 8 seconds
//    public static long ADVERTISE_PERIOD = 5*1000; //5 * 60 * 1000;  // 5 minutes
//    public static long ADVERTISE_DURATION = 3*1000; // 8 * 1000;     // 8 seconds
    public static ParcelUuid SERVICE_UUID = Utils.parseUuidFrom(new byte[] { (byte)0x6f, (byte)0xfd });

    // Can be ADVERTISE_MODE_BALANCED/LOW_LATENCY/LOW_POWER
    public static int advertiseMode = AdvertiseSettings.ADVERTISE_MODE_LOW_POWER;
    // Can be ADVERTISE_TX_POWER_HIGH/MEDIUM/LOW/ULTRA_LOW
    public static int advertiseTxPower = AdvertiseSettings.ADVERTISE_TX_POWER_HIGH;  // High, since high Tx power would give more precise results
    // Can be SCAN_MODE_BALANCED/LOW_LATENCY/LOW_POWER/OPPERTUNISTIC
    public static int scanMode = ScanSettings.SCAN_MODE_LOW_POWER;

    // Server

}
