package com.fox.andrey.photogallery;


import com.google.gson.annotations.SerializedName;

public class GalleryItem {
    // использовать аннотации, чтобы помочь библиотеке разобраться с полями класса, если они не совпадают с нужным именем в json.
    @SerializedName("title")
    private String mCaption;
    @SerializedName("id")
    private String mId;
    @SerializedName("url_s")
    private String mURL;

    @Override
    public String toString() {
        return mCaption;
    }

    public String getCaption() {
        return mCaption;
    }

    public void setCaption(String mCaption) {
        this.mCaption = mCaption;
    }

    public String getId() {
        return mId;
    }

    public void setId(String mId) {
        this.mId = mId;
    }

    public String getURL() {
        return mURL;
    }

    public void setURL(String mURL) {
        this.mURL = mURL;
    }
}
