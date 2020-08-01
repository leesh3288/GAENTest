package com.kaist.gaenclient;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

public class SocketManager {
    private Socket mSocket;
    private String deviceName;
    private MainActivity mainActivity;

    public SocketManager(String deviceName, MainActivity mainActivity) {
        try {
            mSocket = IO.socket("http://"+Config.serverUrl);
            mSocket.connect();
        } catch(URISyntaxException e) {
            e.printStackTrace();
        }
        this.deviceName = deviceName;
        this.mainActivity = mainActivity;

        // Socket event handler
        System.out.println(mSocket);
        mSocket.on("client-type", args -> {
            Log.i("SocketManager","client-type called");
            JSONObject data = new JSONObject();
            try {
                data.put("deviceName", deviceName);
                mSocket.emit("type-device", data);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        mSocket.on("init-device", args -> mainActivity.log("Successfully connected to server"));
        mSocket.on("refuse-device", args -> mainActivity.logError("Failed to connect to server. DeviceName already taken. Please choose another name."));
        mSocket.on("start", args -> {
            mainActivity.log("Experiment started.");
            try {
                Log.i("Socket","start called");
                mainActivity.fetchConfig();
                mainActivity.setAdvertise(true);
                mainActivity.setScan(true);
                mainActivity.log("Start experiment.");
                mSocket.emit("start-success");
            } catch (InterruptedException e) {
                e.printStackTrace();
                mSocket.emit("start-fail");
            }
        });
        mSocket.on("stop", args -> {
            Log.i("Socket","stop called");
            mainActivity.setAdvertise(false);
            mainActivity.setScan(false);
            mainActivity.uploadServer();
            mSocket.emit("stop-success");
            mainActivity.log("Stop experiment.");
            mainActivity.log("Experiment stopped.");
        });
    }
}
