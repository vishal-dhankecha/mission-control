package com.example.vishal_pc.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class GlobalBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "GlobalBroadcastReceiver";
    @Override
    public void onReceive(final Context context, Intent intent) {
        StringBuilder sb = new StringBuilder();
        sb.append("Action: " + intent.getAction() + "\n");
        sb.append("URI: " + intent.toUri(Intent.URI_INTENT_SCHEME).toString() + "\n");
        final String log = sb.toString();

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, log);
                Intent intent = new Intent(context, MainService.class);
                context.startService(intent);
            }
        }, 10000);
        Toast.makeText(context, log, Toast.LENGTH_LONG).show();
    }
}

