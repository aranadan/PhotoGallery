package com.fox.andrey.photogallery;


import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

public class Cache {
    private static Cache instance;
    private LruCache<Object, Bitmap> lru;

    private Cache() {

        lru = new LruCache<Object, Bitmap>(10240);

    }

    public static Cache getInstance() {

        if (instance == null) {

            instance = new Cache();
        }

        return instance;

    }

    public LruCache<Object, Bitmap> getLru() {
        return lru;
    }
}
