package com.fox.andrey.photogallery;


import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlickrFetchr {
    public static Map<Long, GalleryItem> itemMap;
    private static final String TAG = "FlickrFetchr";
    private static final String API_KEY = "75ec99e963baeaa6cd8f389f92a545cb";
    private static final String FETCH_RECENTS_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";


    private Uri getEndpointUri (){
        Uri endpoint = Uri.parse("https://api.flickr.com/services/rest")
                .buildUpon()
                .appendQueryParameter("api_key",API_KEY)
                .appendQueryParameter("page", PhotoGalleryFragment.getPAGE())
                .appendQueryParameter("format","json")
                .appendQueryParameter("nojsoncallback", "1")
                .appendQueryParameter("extras","url_s")
                .build();
        return endpoint;
    }



    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try{
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            if (connection.getResponseCode()!= HttpURLConnection.HTTP_OK){
                throw new IOException(connection.getResponseMessage() + "   : with " + urlSpec);
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0){
                out.write(buffer,0,bytesRead);
            }
            out.close();
            return out.toByteArray();

        }finally {
            connection.disconnect();
            }
    }

    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    public List<GalleryItem> fetchRecentPhotos(){

        String url = buildUrl(FETCH_RECENTS_METHOD,null);
        return downloadGalleryItem(url);
    }

    public List<GalleryItem> searchPhotos(String query){
        String url = buildUrl(SEARCH_METHOD,query);
        return downloadGalleryItem(url);
    }

    public List<GalleryItem> downloadGalleryItem(String url){
        List<GalleryItem> items = new ArrayList<>();
        Log.d(TAG, url);

        try{
            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: " + jsonString);
            JSONObject jsonBody = new JSONObject(jsonString);
            //parseItems(items,jsonBody);
            parseItemGson(items,jsonBody);
        } catch (IOException e) {
            Log.e(TAG, "Failed to fetch  items " + e);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse JSON " + e);
        }
        return items;
    }

    private String buildUrl(String method, String query){
        Uri.Builder uriBuilder = getEndpointUri().buildUpon()
                .appendQueryParameter("method", method);

        if (method.equals(SEARCH_METHOD)){
            uriBuilder.appendQueryParameter("text",query);
        }
        return uriBuilder.build().toString();
    }

    private void parseItemGson(List<GalleryItem> items, JSONObject jsonBody) throws JSONException {
        Gson gson = new Gson();

        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        String photoJsonArray = photosJsonObject.getJSONArray("photo").toString();

        //создаю тип данных для преобразования из json в list<GalleryItem>
        Type type = new TypeToken<ArrayList<GalleryItem>>(){}.getType();
        List<GalleryItem> galleryItems = gson.fromJson(photoJsonArray,type);
        itemMap = new HashMap<>();

        items.addAll(galleryItems);
    }


}

