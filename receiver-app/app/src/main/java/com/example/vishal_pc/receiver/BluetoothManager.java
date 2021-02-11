package com.example.vishal_pc.receiver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

public class BluetoothManager {
    private static final int REQUEST_ENABLE_BT = 1;
    public static String deviceAddress = "00:18:E4:40:00:06";
    BluetoothAdapter bluetoothAdapter;

    ArrayList<BluetoothDevice> pairedDeviceArrayList;
    ArrayAdapter<BluetoothDevice> pairedDeviceAdapter;
    private UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final String UUID_STRING_WELL_KNOWN_SPP =
            "00001101-0000-1000-8000-00805F9B34FB";

    ThreadConnectBTdevice myThreadConnectBTdevice;
    ThreadConnected myThreadConnected;
    private   BluetoothDevice DEVICE;


    public ArduinoDataSendModel arduinoDataSendModel = new ArduinoDataSendModel();
    public ArduinoResponseModel arduinoResponseModel = new ArduinoResponseModel();
    public boolean isConnectedToArduino = false;

    private BluetoothSocket bluetoothSocket = null;
    public String bluetoothConnectionStatus = "Bluetooth: not connected";

    private LocalBroadcastManager mLocalBroadcastManager;

    public BluetoothManager(LocalBroadcastManager localBroadcastManager) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mLocalBroadcastManager = localBroadcastManager;
    }

    MSP msp;


    public void startConnection(){

        msp = new MSP();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            pairedDeviceArrayList = new ArrayList<BluetoothDevice>();


            for (BluetoothDevice device : pairedDevices) {
                pairedDeviceArrayList.add(device);
                Log.d("device", device.getAddress());
                if (device.getAddress().equalsIgnoreCase(deviceAddress))
                {
                    DEVICE = device;
                }
            }
            try
            {
                if (DEVICE != null) {
                    Log.d("device", "Selected" + DEVICE.getName());
                    myThreadConnectBTdevice = new ThreadConnectBTdevice(DEVICE);
                    myThreadConnectBTdevice.start();
                }
            }
            catch (Exception e)
            {
                Log.e("Btconnect", e.getMessage());
            }


        }
    }

    private class ThreadConnectBTdevice extends Thread {
        private final BluetoothDevice bluetoothDevice;


        private ThreadConnectBTdevice(BluetoothDevice device) {
            bluetoothDevice = device;
        }

        @Override
        public void run() {
            Log.d("BTConnect","Started");
            boolean success = true;
            while (true)
            {
                if(bluetoothSocket == null || !bluetoothSocket.isConnected())
                {
                    isConnectedToArduino = false;
                    bluetoothSocket = null;
                    try {
                        bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(myUUID);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        bluetoothConnectionStatus = "Bluetooth: connecting..";
                        updateNotification();
                        bluetoothSocket.connect();
                        Log.d("Connection", "Success");
                        startThreadConnected();
                    } catch (IOException e) {
                        e.printStackTrace();

                        final String eMessage = e.getMessage();
                        Log.e("bluetoothConnect",eMessage);
                    }
                }
                else {
                    try{
                        myThreadConnectBTdevice.sleep(200);
                    }
                    catch (Exception e)
                    {
                        Log.e("ThreadConnectBTdevice", e.getMessage());
                    }

                }
            }
        }
        public void cancel() {

            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    private void startThreadConnected(){

        myThreadConnected = new ThreadConnected();
        myThreadConnected.start();
    }
    private class ThreadConnected extends Thread {
        private final InputStream connectedInputStream;
        private final OutputStream connectedOutputStream;

        public ThreadConnected() {
            InputStream in = null;
            OutputStream out = null;

            try {
                in = bluetoothSocket.getInputStream();
                out = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            connectedInputStream = in;
            connectedOutputStream = out;
            isConnectedToArduino = true;
            bluetoothConnectionStatus = "Bluetooth: connected";
            updateNotification();
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                if(bluetoothSocket == null || !bluetoothSocket.isConnected())
                {
                    break;
                }
                try {
                    if (connectedInputStream.available() > 0) {
                        int size = connectedInputStream.available();
                        while (size != 0){
                            int data = connectedInputStream.read();
                            size--;
                            byte myByte = (byte)data;
                            String str = new String(new byte[]{myByte}, Charset.forName("UTF-8"));
                            msp.decode(data);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    final String msgConnectionLost = "Connection lost:\n"
                            + e.getMessage();
                    bluetoothSocket = null;
                    Log.e("ThreadConnected", e.getMessage());
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                connectedOutputStream.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendToArduino()
    {
        String string = new Gson().toJson(arduinoDataSendModel, ArduinoDataSendModel.class);
        Log.d("senddata" , string);
        if (isConnectedToArduino)
        {
            try {
                myThreadConnected.write(string.getBytes());
            }
            catch (Exception e)
            {
                Log.e("Arduino send:" , e.getMessage());
            }
        }

    }

    public void sendToArduino1(byte[] buffer)
    {
        Log.d("senddata" , "bffer");

        if (isConnectedToArduino)
        {
            try {
                myThreadConnected.write(buffer);
            }
            catch (Exception e)
            {
                Log.e("Arduino send:" , e.getMessage());
            }
        }

    }
    public void updateNotification()
    {
        Log.d("BluetoothManager", "updating notification");
        Intent intent = new Intent(MainService.ACTION);
        intent.putExtra("command", "update-notification");
        mLocalBroadcastManager.sendBroadcast(intent);
    }
}
