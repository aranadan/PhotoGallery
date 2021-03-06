package com.fox.andrey.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


//<T> обобщенный аргумент
public class ThumbnailDownloader<T> extends HandlerThread {

    private static final String TAG = "ThumbnailDownloader";

    //индентификация сообщений как запросов на загрузку
    private static final int MESSAGE_DOWNLOAD = 0;


    private boolean mHasQuit = false;
    private Handler mRequestHandler;
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();
    private Handler mResponseHandler;
    private ThumbnailDownloaderListener<T> mThumbnailDownloaderListener;

    public interface ThumbnailDownloaderListener<T> {
        void onThumbnailDownloaded(T target);
    }

    public void setThumbnailDownloaderListener(ThumbnailDownloaderListener<T> listener) {
        mThumbnailDownloaderListener = listener;
    }

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
    }

    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }


    public void queueThumbnail(T target, String url) {
        Log.d(TAG, "Got a URL: " + url);

        if (url == null) {
            mRequestMap.remove(target);
        } else {
            mRequestMap.put(target, url);
            //извлекаю сообщение из глобального пула сообщений
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget();
        }
    }

    public void clearQueue() {
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
        mRequestMap.clear();
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                if (msg.what == MESSAGE_DOWNLOAD) {
                    T target = (T) msg.obj;
                    //Log.i(TAG, "Got a request for message download: " + mRequestMap.get(target));
                    handleRequest(target);
                }
            }
        };
    }

    private void handleRequest(final T target) {

        final String url = mRequestMap.get(target);
        final Bitmap bitmap;
        if (url == null) {
            return;
        }

            /*if (Cache.getInstance().getLru().get(url) == null) {
                byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
                bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
                Cache.getInstance().getLru().put(url, bitmap);
                Log.i(TAG, "Download from internet");
            }else{
                bitmap = Cache.getInstance().getLru().get(url);
                Log.i(TAG, "Download from cache");
            }*/
        //run new thread in UI because handler create in main thread
        mResponseHandler.post(new Runnable() {
            @Override
            public void run() {

                if (mRequestMap.get(target) != url || mHasQuit) {
                    return;
                }
                mRequestMap.remove(target);
                //send objects to PhotoGalleryFragment
                mThumbnailDownloaderListener.onThumbnailDownloaded(target);
            }
        });
    }
}
