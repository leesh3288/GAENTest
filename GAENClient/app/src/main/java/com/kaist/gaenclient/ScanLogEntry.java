package com.kaist.gaenclient;

import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.UUID;

public class ScanLogEntry {
    private String testId;
    private String myId;
    private long time;
    private int logType;
    private String otherId;
    private int rssi;
    private int tx;
    private int attenuation;
    private byte rssiCorrection;

    public static ScanLogEntry fromScanResult(ScanResult result, int SERVICE_UUID, int PROTOCOL_VER, String myId, byte rssiCorrection, String testId) {
        ScanRecord scanRecord = result.getScanRecord();
        if (scanRecord == null)
            return null;

        ///// Check payload format.
        byte[] data = scanRecord.getBytes();
        if (data.length < 31)
            return null;

        byte[] payloadFormat = {0x02, 0x01, 0x1A,
                0x03, 0x03, (byte) (SERVICE_UUID & 0xff), (byte) ((SERVICE_UUID >> 8) & 0xff),
                0x17, 0x16, (byte) (SERVICE_UUID & 0xff), (byte) ((SERVICE_UUID >> 8) & 0xff)
        };

        for (int i = 0; i < payloadFormat.length; i++)
            if (data[i] != payloadFormat[i])
                return null;

        // versioning check
        if (data[0x1b] != PROTOCOL_VER)
            return null;
        /////

        ScanLogEntry entry = new ScanLogEntry();

        entry.testId = testId;
        entry.myId = myId;
        entry.time = System.currentTimeMillis() -
                SystemClock.elapsedRealtime() +
                result.getTimestampNanos() / 1000000;
        entry.logType = 0;  // TODO: field useful or not?
        entry.rssi = result.getRssi();
        entry.tx = data[0x1c];
        entry.attenuation = entry.tx - (entry.rssi + rssiCorrection);
        entry.rssiCorrection = rssiCorrection;
        entry.otherId = (new String(Arrays.copyOfRange(data, 0xb, 0x1b))).replaceAll("\u0000.*", "");

        return entry;
    }

    public JSONObject getJSONObject() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("testId", testId);
            obj.put("myId", myId);
            obj.put("time", time);
            obj.put("logType", logType);
            obj.put("otherId", otherId);
            obj.put("rssi", rssi);
            obj.put("rssiCorrection", rssiCorrection);
            obj.put("tx", tx);
            obj.put("attenuation", attenuation);
            return obj;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "ScanLogEntry{" +
                "myId=" + myId +
                ", time=" + time +
                ", logType=" + logType +
                ", otherId=" + otherId +
                ", rssi=" + rssi +
                ", tx=" + tx +
                ", attenuation=" + attenuation +
                '}';
    }

    public String getOtherId() {
        return otherId;
    }

    public String getTestId() {
        return testId;
    }
}
