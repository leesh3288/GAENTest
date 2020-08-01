package com.kaist.gaenclient;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

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

        mSocket.on("init-device", args -> {
            this.mainActivity.log("Successfully connected to server");
        });
        mSocket.on("refuse-device", args -> this.mainActivity.logError("Failed to connect to server. DeviceName already taken."));
    }

    public void asdf() {
        System.out.println(deviceName);
    }

}
