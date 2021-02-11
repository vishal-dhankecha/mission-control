package com.example.vishal_pc.receiver;

import org.json.JSONObject;

public class WebRTCResponseModel {
    String type;
    String name;
    MessageModel answer;
    MessageModel candidate;
    MessageModel offer;
    public class MessageModel
    {
        String sdpMid;
        String type;
        String sdp;
        String candidate;
        int sdpMLineIndex;
    }
}
