package com.example.vishal_pc.receiver;

public class ServerResponseModel extends ArduinoDataSendModel {
    public String message;
    public String command;
    public String commandPayload;

    ServerResponseModel()
    {
        super();
        message="";
    }
}
