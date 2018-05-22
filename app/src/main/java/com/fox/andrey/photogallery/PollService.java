package com.fox.andrey.photogallery;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PollService extends IntentService {
    private static final String TAG = "PollService";
    // 60 секунд
    private static final long POLL_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1);

    public PollService() {
        super(TAG);
    }

    public static Intent newIntent(Context context) {
        return new Intent(context, PollService.class);
    }

    /*Как сообщить AlarmManager, какие интенты нужно отправить? При помощи объекта PendingIntent. По сути, в объекте PendingIntent упаковывается пожелание:
    «Я хочу запустить PollService». Затем это пожелание отправляется другим компонентам системы, таким как AlarmManager.
    Включите вPollServiceновый метод с именемsetServiceAlarm(Context,boolean),
    который включает и отключает сигнал за вас. Метод будет объявлен статическим; это делается для того, чтобы код сигнала размещался рядом с другим кодом
    PollService, с которым он связан, но мог вызываться и другими компонентами.
    Обычно включение и отключение должно осуществляться из интерфейсного кода
    фрагмента или из другого контроллера*/
    public static void setServiceAlarm(Context context, boolean isOn) {
        Intent i = PollService.newIntent(context);

        /*Метод получает
четыре параметра: Context для отправки интента; код запроса, по которому этот
объект PendingIntent отличается от других; отправляемый объект Intent и, нако554 Глава 28. Фоновые службы
нец, набор флагов, управляющий процессом создания PendingIntent (вскоре мы
используем один из них)*/
        PendingIntent pi = PendingIntent.getService(context, 0, i, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (isOn) {
            //AlarmManager.ELAPSED_REALTIME задает начальное время запуска относительно прошедшего реального времени:
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), POLL_INTERVAL_MS, pi);
        } else {
            alarmManager.cancel(pi);
            pi.cancel();
        }
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
        }
        QueryPreferences.setLastResultId(this, resultId);

    }

    private boolean isNetworkAvailableAndConnected() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
        boolean isNetworkConnected = isNetworkAvailable &&
                cm.getActiveNetworkInfo().isConnected();
        return isNetworkConnected;
    }
}
