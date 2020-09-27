package com.kaist.gaenclient;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ScanInstance {
    private String testId;
    private String myId;
    private long time;
    private String otherId;
    private int typicalAttenuation;
    private int minAttenuation;
    private int secondsSinceLastScan;

    public static List<ScanInstance> fromScanResults(List<ScanLogEntry> results) {
        HashMap<String,List<ScanLogEntry>> sort = new HashMap<>();
        List<ScanInstance> res = new ArrayList<>();

        return res;
    }

//    private ScanInstance aggregate(List<ScanLogEntry> results) {
//        ScanInstance scanInstance = new ScanInstance();
//
//    }

    public JSONObject getJSONObject() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("testId", testId);
            obj.put("myId", myId);
            obj.put("time", time);
            obj.put("otherId", otherId);
            obj.put("typicalAttenuation", typicalAttenuation);
            obj.put("minAttenuation", minAttenuation);
            obj.put("secondsSinceLastScan", secondsSinceLastScan);
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
                ", otherId=" + otherId +
                ", typicalAttenuation=" + typicalAttenuation +
                ", minAttenuation=" + minAttenuation +
                ", secondsSinceLastScan=" + secondsSinceLastScan +
                '}';
    }
}
