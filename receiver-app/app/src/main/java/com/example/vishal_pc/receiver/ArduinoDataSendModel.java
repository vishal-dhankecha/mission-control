package com.example.vishal_pc.receiver;

public class ArduinoDataSendModel {

    int rt;
    int ry;
    int rp;
    int rr;
    int r1;
    int r2;
    boolean irc;

    ArduinoDataSendModel()
    {
        rt = 1000;
        ry = 1500;
        rp = 1500;
        rr = 1500;
        r1 = 1000;
        r2 = 1000;
        irc =false;
    }
    public void TriggerFailSafeMode()
    {
        if(r1 >= 1500){
            r1 = 2000;
            r2 = 1500;
            rt = 1500;
            ry = 1500;
            rp = 1500;
            rr = 1500;
        }
    }
}
