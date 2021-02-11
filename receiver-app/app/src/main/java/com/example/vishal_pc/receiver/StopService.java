package com.example.vishal_pc.receiver;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class StopService  extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("StopService", "stopping service");
        Intent intent1 = new Intent(this, MainService.class);
        stopService(intent1);
        finish();
    }

}