package com.example.vishal_pc.receiver;

public class ArduinoResponseModel {
    double voltage, al, ax, ay, az;
    int te,te2, spe;
    String lat, lon;

    public ArduinoResponseModel()
    {
        voltage = 0.0;
    }
}