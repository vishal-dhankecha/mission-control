package com.example.vishal_pc.receiver.Models;

public class PingStatusModel {
    public boolean pingSent;
    public boolean pingReceived;
    public int time;

    public PingStatusModel()
    {
        reset();
    }

    public void reset()
    {
        pingSent = false;
        pingReceived = false;
        time = 0;
    }
}
