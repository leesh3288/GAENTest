package com.kaist.gaenclient;

import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class GeneralLogEntry implements IJsonConvertible {
    private String testId;
    private String myId;
    private long time;
    private String msg;

    public static GeneralLogEntry createLog(String testId, String myId, String msg) {
        GeneralLogEntry entry = new GeneralLogEntry();

        entry.testId = testId;
        entry.myId = myId;
        entry.time = System.currentTimeMillis(); //TODO: is this consistent with the scanlog time?
        entry.msg = msg;

        return entry;
    }

    public JSONObject getJSONObject() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("testId", testId);
            obj.put("myId", myId);
            obj.put("time", time);
            obj.put("msg", msg);
            return obj;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    // DEBUG
    public static GeneralLogEntry test() {
        GeneralLogEntry entry = new GeneralLogEntry();

        entry.testId = "generatedTest";
        entry.myId = "myDevice";
        entry.time = System.currentTimeMillis(); //TODO: is this consistent with the scanlog time?
        entry.msg = "This is a meaningless message.";

        return entry;
    }

    @NonNull
    @Override
    public String toString() {
        return "ScanLogEntry{" +
                "myId=" + myId +
                ", time=" + time +
                ", msg=" + msg +
                '}';
    }
}
