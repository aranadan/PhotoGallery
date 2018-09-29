package com.fox.andrey.photogallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class StartupReceiver extends BroadcastReceiver {
    private static final String TAG = "StartupReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.

        Log.i(TAG, "Received broadcast intent: " + intent.getAction());

        // Включение сигнала при загрузке
        boolean isOn = QueryPreferences.isAlarmOn(context);
        Log.i(TAG, "isAlarmOn: " + isOn);


            PollService.setServiceAlarm(context, isOn);


    }
}
