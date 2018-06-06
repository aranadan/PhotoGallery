package com.fox.andrey.photogallery;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class PollJobService extends JobService {
    private PollTask mCurrentTask;
    private static final String TAG = "PollJobService";
    boolean schedulerOn = false;
    JobScheduler scheduler;

    final static int JOB_ID = 1;


    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        mCurrentTask = new PollTask();
        mCurrentTask.execute(jobParameters);

        //это значит, что мы сообщили JobScheduler'у что выполнение задачи продолжается где-то в побочном потоке.
        return true;
    }


    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        if (mCurrentTask != null) {
            mCurrentTask.cancel(true);
        }
        jobFinished(jobParameters, schedulerOn);
        Log.d("PollJobService", "Служба остановлена");

        //true говорит планировщику что задача перепланируется и запускается заново.
        return true;
    }


    public static void startScheduler(Context context, boolean turnOn) {


        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (turnOn) {
            JobInfo jobInfo = new JobInfo.Builder(JOB_ID, new ComponentName(context, PollJobService.class)).setPeriodic(1000 * 30).setPersisted(true).build();
            scheduler.schedule(jobInfo);
            Log.d("PollJobService", "Планировщик запущен");
        } else {
            scheduler.cancelAll();
        }



    }


    public static boolean isSchedulerOn(Context context) {

        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        boolean isOn = QueryPreferences.isAlarmOn(context);
        //ищу по ID свою службу
        for (JobInfo job : scheduler.getAllPendingJobs()) {

            //ищу свою службу в списке всех служб и отмечаю что она запущена
            if (job.getId() == JOB_ID) {
                isOn = true;
            } else {
                isOn = false;
            }
        }
        Log.d("PollJobService", "isSchedulerOn = : " + isOn);
        //Запись настройки для хранения состояния сигнала
        QueryPreferences.setAlarmOn(context, isOn);
        //Log.d("PollJobService", "startScheduler / isOn / writing : " + isOn);

        return isOn;
    }

    private JobScheduler getSchedulerObject(Context context) {

        if (scheduler == null)
            scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        return scheduler;
    }

    public void stopScheduler() {
        // JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        for (JobInfo job : scheduler.getAllPendingJobs()) {
            if (job.getId() == JOB_ID) {
                scheduler.cancel(job.getId());
                schedulerOn = false;
            }
        }
    }


    private class PollTask extends AsyncTask<JobParameters, Void, List<GalleryItem>> {

        private Context mContext;

        public PollTask() {
            mContext = PollJobService.this;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d("PollJobService", "Подготовка к загрузке");

        }

        @Override
        protected List<GalleryItem> doInBackground(JobParameters... params) {
            JobParameters jobParams = params[0];


            // Проверка новых изображений на Flickr
            String query = QueryPreferences.getStoredQuery(mContext);

            List<GalleryItem> items;
            if (query == null) {
                items = new FlickrFetchr().fetchRecentPhotos();
            } else {
                items = new FlickrFetchr().searchPhotos(query);
            }

            //передачи планировщику информации о завершении задачи
            jobFinished(jobParams, false);
            return items;
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            Log.d(TAG, "Items count is: " + items.size());
            super.onPostExecute(items);

            if (items.size() == 0) {
                return;
            }

            String lastResultId = QueryPreferences.getLastResultId(mContext);
            String resultId = items.get(0).getId();
            if (resultId.equals(lastResultId)) {
                Log.i(TAG, "Got an old result: " + resultId);
            } else {
                Log.i(TAG, "Got a new result: " + resultId);

                //Добавление оповещения
                // Объект PendingIntent, передаваемый setContentIntent(PendingIntent), будет запускаться при нажатии пользователем на вашем оповещении на выдвижной панели.
                Resources resources = getResources();
                Intent i = PhotoGalleryActivity.newIntent(mContext);
                PendingIntent pi = PendingIntent.getActivity(mContext, 0, i, 0);

                Notification notification = new NotificationCompat.Builder(mContext).setTicker(resources.getString(R.string.new_pictures_title)).setSmallIcon(android.R.drawable.ic_menu_report_image).setContentTitle(resources.getString(R.string.new_pictures_title)).setContentText(resources.getString(R.string.new_pictures_text)).setContentIntent(pi)
                        //с этим вызовом оповещение при нажатии также будет удаляться с выдвижной панели оповещений
                        .setAutoCancel(true).build();

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);
                notificationManager.notify(0, notification);
                //конец оповещения
            }
            QueryPreferences.setLastResultId(mContext, resultId);


        }


    }

}
