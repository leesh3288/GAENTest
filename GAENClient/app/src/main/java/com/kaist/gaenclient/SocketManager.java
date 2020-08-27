package com.kaist.gaenclient;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

public class SocketManager {
    private Socket mSocket;
    private MainActivity mainActivity;

    public SocketManager(String deviceName, MainActivity mainActivity) {
        try {
            mSocket = IO.socket("http://"+Config.serverUrl);
            mSocket.connect();
        } catch(URISyntaxException e) {
            e.printStackTrace();
        }

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
                Log.i("Socket",(new JSONObject(args[0].toString())).get("testId").toString());
                mainActivity.setTestId((new JSONObject(args[0].toString())).get("testId").toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.i("Socket","start called");
            mainActivity.clearLog();
//                mainActivity.fetchConfig();
            if(mainActivity.deviceId.length() < 4) {
                mainActivity.setAdvertise(true);
            } else if(!mainActivity.deviceId.substring(0,4).equals("scan")) {
                mainActivity.setAdvertise(true);
            }
            if(mainActivity.deviceId.length() < 3) {
                mainActivity.setScan(true);
            } else if (!mainActivity.deviceId.substring(0,3).equals("adv")) {
                mainActivity.setScan(true);
            }
            mainActivity.log("Start experiment.");
            mSocket.emit("start-success");
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

        this.mainActivity = mainActivity;
    }

    public void disconnectSocket() {
        mSocket.disconnect();
        mainActivity.log("Disconnected to server.");
    }
}
