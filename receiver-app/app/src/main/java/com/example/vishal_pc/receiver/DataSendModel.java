package com.example.vishal_pc.receiver;

import java.util.Date;

public class DataSendModel {
    public String bluetoothConnectionStatus;
    public String telemetryData= "No Data";
    public ArduinoResponseModel arduinoResponseModel;
    public Date timeStamp;

    public DataSendModel(){
        bluetoothConnectionStatus = "";
        arduinoResponseModel = new ArduinoResponseModel();
        timeStamp = new Date();
    }
}
