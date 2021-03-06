package com.kaist.gaenclient;

import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.ScanSettings;

import java.util.UUID;

public class Config {
    public static long SCAN_PERIOD = 5 * 60 * 1000;     // 5 minutes
    public static long SCAN_DURATION = 4 * 1000;        // 4 seconds
    public static long MAX_JITTER = 90 * 1000;          // 0 ~ 1.5 min jitter
    public static long UPLOAD_PERIOD = 10 * 60 * 1000;  // 10 minutes

    public static int SERVICE_UUID = 0xfd6f;

    public static byte PROTOCOL_VER = 0b01000000;  // Major 01, Minor 00, reserved 0000

    /* Can be ADVERTISE_MODE_BALANCED/LOW_LATENCY/LOW_POWER
     * Each translates into 250/100/1000ms advertising interval
     * Reference: https://www.sciencedirect.com/science/article/pii/S1877050919309238
     */
    public static int advertiseMode = AdvertiseSettings.ADVERTISE_MODE_BALANCED;
    /* Can be ADVERTISE_TX_POWER_HIGH/MEDIUM/LOW/ULTRA_LOW
     * GAEN framework and its corresponding calibration data is based on ADVERTISE_TX_POWER_LOW
     */
    public static int advertiseTxPower = AdvertiseSettings.ADVERTISE_TX_POWER_LOW;
    // Can be SCAN_MODE_BALANCED/LOW_LATENCY/LOW_POWER/OPPORTUNISTIC
    public static int scanMode = ScanSettings.SCAN_MODE_LOW_LATENCY;
    public static boolean initJitter = true;

    public static final UUID NAMESPACE_GAEN = Utils.HashUuidCreator.getSha1Uuid("NAMESPACE_GAEN");

    /* Precise control over advertisement (interval, address anonymity) works only on
     * API level 26 or above, and thus is omitted.
     */

    // Server
    public static String serverUrl = "110.76.78.76:54545";
}
