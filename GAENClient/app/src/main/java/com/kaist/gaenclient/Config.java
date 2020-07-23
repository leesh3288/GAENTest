package com.kaist.gaenclient;

import android.bluetooth.le.AdvertiseSettings;
import android.os.ParcelUuid;

public class Config {
    public static int test = 0;
    public static long SCAN_PERIOD = 5 * 60 * 1000;  // 5 minutes
    public static long SCAN_DURATION = 8 * 1000;     // 8 seconds
    public static ParcelUuid SERVICE_UUID = Utils.parseUuidFrom(new byte[] { (byte)0x6f, (byte)0xfd });

    // Can be ADVERTISE_MODE_BALANCED/LOW_LATENCY/LOW_POWER
    public static int advertiseMode = AdvertiseSettings.ADVERTISE_MODE_BALANCED;
    // Can be ADVERTISE_TX_POWER_HIGH/MEDIUM/LOW/ULTRA_LOW
    public static int advertiseTxPower = AdvertiseSettings.ADVERTISE_TX_POWER_LOW;  // low, since we are using BLE (but can be high) - NOTE: Opentrace set this as HIGH
}
