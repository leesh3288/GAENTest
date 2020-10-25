package com.kaist.gaenclient;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ScanInstance implements IJsonConvertible {
    private String testId;
    private String myId;
    private long time;
    private String otherId;
    private int typicalAttenuation;         // Attenuation averaged in dB domain (like in GAEN)
    private int typicalPowerAttenuation;    // Attenuation averaged in power domain
    private int minAttenuation;
    private int secondsSinceLastScan;

    // Takes all ScanLogEntries from that scan, and return the list of ScanResults
    public static List<ScanInstance> fromScanResults(List<ScanLogEntry> results, int secondsSinceLastScan) {
        HashMap<String,List<ScanLogEntry>> sort = new HashMap<>();
        List<ScanInstance> res = new ArrayList<>();
        for (ScanLogEntry entry: results) {
            String otherId = entry.getOtherId();
            if (sort.containsKey(otherId)) {
                sort.get(otherId).add(entry);
            } else {
                List<ScanLogEntry> newList = new ArrayList<>();
                newList.add(entry);
                sort.put(otherId,newList);
            }
        }
        for (List<ScanLogEntry> perOtherId: sort.values()) {
            res.add(aggregate(perOtherId, secondsSinceLastScan));
        }
        return res;
    }

    public static ScanInstance aggregate(List<ScanLogEntry> results, int secondsSinceLastScan) {
        ScanInstance scanInstance = new ScanInstance();
        ScanLogEntry entry1 = results.get(0);
        scanInstance.myId = entry1.getMyId();
        scanInstance.otherId = entry1.getOtherId();
        scanInstance.testId = entry1.getTestId();
        scanInstance.time = entry1.getTime();
        scanInstance.secondsSinceLastScan = secondsSinceLastScan;
        List<Integer> attenuations = new ArrayList<>();
        for (ScanLogEntry entry: results) {
            attenuations.add(entry.getAttenuation());
        }
        scanInstance.minAttenuation = Collections.min(attenuations);
        scanInstance.typicalAttenuation = dBMean(attenuations);
        scanInstance.typicalPowerAttenuation = powerMean(attenuations);
        return scanInstance;
    }

    public static int dBMean(List<Integer> attenuations) {
        double sum = 0;
        for (int a: attenuations) {
            sum += a;
        }
        return (int)Math.round(sum/attenuations.size());
    }

    public static int powerMean(List<Integer> attenuations) {
        double sum = 0;
        for (int a: attenuations) {
            sum += Math.pow(10,(double)a/10);
        }
        return (int)(10*Math.log10(sum/attenuations.size()));
    }

    public JSONObject getJSONObject() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("testId", testId);
            obj.put("myId", myId);
            obj.put("time", time);
            obj.put("otherId", otherId);
            obj.put("typicalAttenuation", typicalAttenuation);
            obj.put("typicalPowerAttenuation", typicalPowerAttenuation);
            obj.put("minAttenuation", minAttenuation);
            obj.put("secondsSinceLastScan", secondsSinceLastScan);
            return obj;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    // DEBUG
    public static ScanInstance test() {
        ScanInstance scanInstance = new ScanInstance();
        scanInstance.myId = "myDevice";
        scanInstance.otherId = "otherDevice";
        scanInstance.testId = "generatedTest";
        scanInstance.time = System.currentTimeMillis();
        scanInstance.secondsSinceLastScan = 100;
        scanInstance.minAttenuation = 30;
        scanInstance.typicalAttenuation = 30;
        scanInstance.typicalPowerAttenuation = 30;
        return scanInstance;
    }

    @NonNull
    @Override
    public String toString() {
        return "ScanInstance{" +
                "myId=" + myId +
                ", time=" + time +
                ", otherId=" + otherId +
                ", typicalAttenuation=" + typicalAttenuation +
                ", typicalPowerAttenuation=" + typicalPowerAttenuation +
                ", minAttenuation=" + minAttenuation +
                ", secondsSinceLastScan=" + secondsSinceLastScan +
                '}';
    }
}
