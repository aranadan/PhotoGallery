package com.fox.andrey.photogallery;

import android.app.Activity;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

/*Широковещательный интент часто будет
отправляться непосредственно перед прекращением существования PollService.
Это означает, что и приемник широковещательного интента может быть мертв.
Таким образом, последний приемник должен быть автономным, и вы должны
обеспечить его выполнение после динамически зарегистрированного приемника.*/

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";

    @Override
    public void onReceive(Context c, Intent i) {
        Log.i(TAG, "received result: " + getResultCode());
        if (getResultCode() != Activity.RESULT_OK) {

            // Активность переднего плана отменила рассылку
            return;
        }
        int requestCode = i.getIntExtra(PollService.REQUEST_CODE, 0);
        Notification notification = (Notification)
                i.getParcelableExtra(PollService.NOTIFICATION);
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(c);
        notificationManager.notify(requestCode, notification);
    }
}
