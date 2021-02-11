package com.example.vishal_pc.receiver;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

public class WebsocketServer extends WebSocketServer
{
    ServerResponseModel responseModel;
    private LocalBroadcastManager mLocalBroadcastManager;

    public WebsocketServer(InetSocketAddress address, LocalBroadcastManager localBroadcastManager) {
        super(address);
        responseModel = new ServerResponseModel();
        mLocalBroadcastManager = localBroadcastManager;
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onClose(WebSocket arg0, int arg1, String arg2, boolean arg3) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onError(WebSocket arg0, Exception arg1) {
        // TODO Auto-generated method stub
        System.out.println(arg1.getStackTrace());

    }

    @Override
    public void onMessage(WebSocket arg0, String arg1) {
        // TODO Auto-generated method stub
//        System.out.println("new message " + arg1 );
        try{
            responseModel = new Gson().fromJson(arg1, ServerResponseModel.class);
            updateControlData();
        }
        catch (Exception e)
        {
            Log.d("ServerResponse",e.getMessage());
        }

    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake arg1) {
        // TODO Auto-generated method stub
        conn.send("Welcome to the server");
        System.out.println("new connection to " + conn.getRemoteSocketAddress());

    }

    public void updateNotification()
    {
        Intent intent = new Intent(MainService.ACTION);
        intent.putExtra("command", "update-notification");
        mLocalBroadcastManager.sendBroadcast(intent);
    }
    public void updateControlData()
    {
        Intent intent = new Intent(MainService.ACTION);
        intent.putExtra("command", "update-controls");
        mLocalBroadcastManager.sendBroadcast(intent);
    }

}