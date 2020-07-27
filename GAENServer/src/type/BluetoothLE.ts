export class AdvertiseSettings {
    public static ADVERTISE_MODE_BALANCED = 1;
    public static ADVERTISE_MODE_LOW_LATENCY = 2;
    public static ADVERTISE_MODE_LOW_POWER = 0;
    public static ADVERTISE_TX_POWER_HIGH = 3;
    public static ADVERTISE_TX_POWER_LOW = 1;
    public static ADVERTISE_TX_POWER_MEDIUM = 2;
    public static ADVERTISE_TX_POWER_ULTRA_LOW = 0;
}

export class ScanSettings {
    public static SCAN_MODE_BALANCED = 1;
    public static SCAN_MODE_LOW_LATENCY = 2;
    public static SCAN_MODE_LOW_POWER = 0;
    public static SCAN_MODE_OPPORTUNISTIC = -1;
}