package com.fox.andrey.photogallery;


import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FlickrFetchr {
    private static final String TAG = "FlickrFetchr";
    private static final String API_KEY = "75ec99e963baeaa6cd8f389f92a545cb";

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

    public List<GalleryItem> fetchItems(){
        List<GalleryItem> items = new ArrayList<>();

        try{
            String url = Uri.parse("https://api.flickr.com/services/rest")
                    .buildUpon()
                    .appendQueryParameter("method","flickr.photos.getRecent")
                    .appendQueryParameter("api_key",API_KEY)
                    .appendQueryParameter("format","json")
                    .appendQueryParameter("nojsoncallback","1")
                    .appendQueryParameter("extras","url_s")
                    .build().toString();
            String jsonString = getUrlString(url);
            Log.i(TAG, "Recieved JSON: " + jsonString);
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

    private void parseItems(List<GalleryItem> items, JSONObject jsonBody) throws JSONException {
        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photojsonArray = photosJsonObject.getJSONArray("photo");

        for (int i = 0; i < photojsonArray.length(); i++){
            JSONObject photoJsonObject = photojsonArray.getJSONObject(i);

            GalleryItem item = new GalleryItem();
            item.setmId(photoJsonObject.getString("id"));
            item.setmCaption(photoJsonObject.getString("title"));

            if(!photoJsonObject.has("url_s")){
                continue;
            }
            item.setmURL(photoJsonObject.getString("url_s"));
            items.add(item);
        }
    }

    private void parseItemGson(List<GalleryItem> items, JSONObject jsonBody) throws JSONException {
        Gson gson = new Gson();

        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        String photojsonArray = photosJsonObject.getJSONArray("photo").toString();
        //создаю тип данных дре преобразования из json в list<GalleryItem>
        Type type = new TypeToken<ArrayList<GalleryItem>>(){}.getType();
        List<GalleryItem> galleryItems = gson.fromJson(photojsonArray,type);
        items.addAll(galleryItems);
        }
    }

