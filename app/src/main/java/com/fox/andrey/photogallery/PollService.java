package com.fox.andrey.photogallery;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PollService extends IntentService {
    private static final String TAG = "PollService";

    // 15 минут
    private static final long POLL_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1);

    public PollService() {
        super(TAG);
    }

    public static Intent newIntent(Context context) {
        return new Intent(context, PollService.class);
    }

    /*Как сообщить AlarmManager, какие интенты нужно отправить? При помощи объекта PendingIntent. По сути, в объекте PendingIntent упаковывается пожелание:
    «Я хочу запустить PollService». Затем это пожелание отправляется другим компонентам системы, таким как AlarmManager.
    в PollService новый метод с именем setServiceAlarm(Context,boolean),
    который включает и отключает сигнал за вас. Метод будет объявлен статическим; это делается для того, чтобы код сигнала размещался рядом с другим кодом
    PollService, с которым он связан, но мог вызываться и другими компонентами.
    Обычно включение и отключение должно осуществляться из интерфейсного кода
    фрагмента или из другого контроллера*/
    public static void setServiceAlarm(Context context, boolean isOn) {
        Intent i = PollService.newIntent(context);

        /*Метод получает
        четыре параметра: Context для отправки интента; код запроса, по которому этот
        объект PendingIntent отличается от других; отправляемый объект Intent и набор флагов, управляющий процессом создания PendingIntent*/
        PendingIntent pi = PendingIntent.getService(context, 0, i, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (isOn) {
            //AlarmManager.ELAPSED_REALTIME задает начальное время запуска относительно прошедшего реального времени:
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), POLL_INTERVAL_MS, pi);
        } else {
            alarmManager.cancel(pi);
            pi.cancel();
        }

        //Запись настройки для хранения состояния сигнала
        QueryPreferences.setAlarmOn(context, isOn);

    }


    public static boolean isServiceAlarmOn(Context context) {
        Intent i = PollService.newIntent(context);
        PendingIntent pi = PendingIntent
                .getService(context, 0, i, PendingIntent.FLAG_NO_CREATE);  //PendingIntent.FLAG_NO_CREATE) Флаг говорит, что если объект PendingIntent не существует, то вместо его создания следует вернуть null
        return pi != null;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        if (!isNetworkAvailableAndConnected()) {
            return;
        }

        
        //Log.i(TAG, "Received an intent: " + intent);
        String query = QueryPreferences.getStoredQuery(this);
        String lastResultId = QueryPreferences.getLastResultId(this);
        List<GalleryItem> items;
        if (query == null) {
            items = new FlickrFetchr().fetchRecentPhotos();
        } else {
            items = new FlickrFetchr().searchPhotos(query);
        }
        if (items.size() == 0) {
            return;
        }
        String resultId = items.get(0).getId();
        if (resultId.equals(lastResultId)) {
            Log.i(TAG, "Got an old result: " + resultId);
        } else {
            Log.i(TAG, "Got a new result: " + resultId);

            //Добавление оповещения
            // Объект PendingIntent, передаваемый setContentIntent(PendingIntent), будет запускаться при нажатии пользователем на вашем оповещении на выдвижной панели.
            Resources resources = getResources();
            Intent i = PhotoGalleryActivity.newIntent(this);
            PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

            Notification notification = new NotificationCompat.Builder(this)
                    .setTicker(resources.getString(R.string.new_pictures_title))
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    .setContentTitle(resources.getString(R.string.new_pictures_title))
                    .setContentText(resources.getString(R.string.new_pictures_text))
                    .setContentIntent(pi)
                    //с этим вызовом оповещение при нажатии также будет удаляться с выдвижной панели оповещений
                    .setAutoCancel(true)
                    .build();

            NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(this);
            notificationManager.notify(0, notification);
            //конец оповещения
        }
        QueryPreferences.setLastResultId(this, resultId);

    }

    private boolean isNetworkAvailableAndConnected() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
        return isNetworkAvailable &&
                cm.getActiveNetworkInfo().isConnected();
    }
}
