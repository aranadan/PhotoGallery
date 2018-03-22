package com.fox.andrey.photogallery;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;


public class PhotoGalleryFragment extends Fragment {
    private static final String TAG = "PhotoGalleryFragment";
    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private boolean loadingAvailable = true;
    private static int page = 1;



    public static PhotoGalleryFragment newInstance() {
        return newInstance();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Fragment onCreate");
        //удерживаю фрагмент
        setRetainInstance(true);
        //выполняю загрузку данных в фоне
        new FetchItemTask().execute();

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloaderListener(new ThumbnailDownloader.ThumbnailDownloaderListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoHolder target, Bitmap thumbnail) {
                Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
                target.bindDrawable(drawable);
            }
        });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
         final View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mPhotoRecyclerView = v.findViewById(R.id.photo_recycler_view);
        //динамическое обновление кол-ва колонок в зависимости от ширины дисплея
        mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int spanCount = 2;
                int width = getView().getWidth();
                if (width > 1000) spanCount = 3;
                else if (width > 1800) spanCount =4;
                mPhotoRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), spanCount));
            }
        });

        //прослушка для добавления новой партии фотографий при достижении последнего пункта списка
        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                //если последняя строчка списка
                if (!mPhotoRecyclerView.canScrollVertically(1) && loadingAvailable) {
                    loadingAvailable = false;
                    Log.i(TAG, "Last item of list");
                    pageIncrement();
                    //Do fetch new data
                    new FetchItemTask().execute();
                }
            }
        });

        setupAdapter();
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();

    }

    private void setupAdapter() {
        //если фрагмент присоединен к активности
        if (isAdded()) {

            Log.i(TAG, "adapter status is:" + mPhotoRecyclerView.getAdapter());
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
            Log.i(TAG, "new adapter was created");
            //}else mPhotoRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }


    private class PhotoHolder extends RecyclerView.ViewHolder {

        private ImageView mImageView;

        public PhotoHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.item_image_view);
        }

        public void bindDrawable(Drawable drawable) {
            mImageView.setImageDrawable(drawable);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

        private List<GalleryItem> mGalleryItem;

        public PhotoAdapter(List<GalleryItem> mGalleryItem) {
            this.mGalleryItem = mGalleryItem;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, parent, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            Log.i(TAG, "Position is:" + position);
            GalleryItem item = mGalleryItem.get(position);
            Drawable placeHolder = getResources().getDrawable(R.mipmap.no_image);
            holder.bindDrawable(placeHolder);
            mThumbnailDownloader.queueThumbnail(holder, item.getmURL());
        }

        @Override
        public int getItemCount() {
            return mGalleryItem.size();
        }
    }

    private class FetchItemTask extends AsyncTask<Void, Void, List<GalleryItem>> {

        @Override
        protected List<GalleryItem> doInBackground(Void... voids) {
            return new FlickrFetchr().fetchItems(getPage());
        }

        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) {
            mItems.addAll(galleryItems);
            //setupAdapter();
            mPhotoRecyclerView.getAdapter().notifyDataSetChanged();
            loadingAvailable = true;
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");
    }

    private void pageIncrement() {
        page++;
    }

    private String getPage() {
        return String.valueOf(page);
    }
}
