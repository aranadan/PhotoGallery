package com.fox.andrey.photogallery;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.support.v7.widget.SearchView;
import android.widget.ProgressBar;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;


public class PhotoGalleryFragment extends VisibleFragment {
    private static final String TAG = "PhotoGalleryFragment";
    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
    //private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private ProgressBar progressBar;
    private boolean loadingAvailable = true;



    private static int PAGE = 1;

    public static void setPage(int page) {
        PhotoGalleryFragment.PAGE = page;
    }



    public static PhotoGalleryFragment newInstance() {
        return newInstance();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate Fragment");

        //hold fragment
        setRetainInstance(true);

        //menu get callback
        setHasOptionsMenu(true);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);


        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "QueryTextSubmit " + query);
                QueryPreferences.setStoredQuery(getActivity(), query);

                searchView.setIconified(true);
                searchView.clearFocus();

                //включаю показ процеса загрузки
                //progressBar.setVisibility(ProgressBar.VISIBLE);

                //удаляю данные по старому запросу
                resetItemsAndPage();
                updateItems();

                //оповещаю адаптер
                mPhotoRecyclerView.getAdapter().notifyDataSetChanged();


                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //Log.d(TAG, "QueryTextChange: " + newText);
                return false;
            }
        });

        searchView.setOnSearchClickListener(v -> {
            String query = QueryPreferences.getStoredQuery(getActivity());
            searchView.setQuery(query, false);


        });


        //Переключение текста элемента меню
        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);


            if (PollService.isServiceAlarmOn(getActivity())) {
                toggleItem.setTitle(R.string.stop_polling);
            } else {
                toggleItem.setTitle(R.string.start_polling);
            }

    }

    private void resetItemsAndPage() {
        mItems.clear();
        PAGE = 1;
    }

    /*Каждый раз, когда пользователь выбирает элемент Clear Search в дополнительном
    меню, стираем сохраненный запрос (присваиванием ему null)*/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                resetItemsAndPage();
                updateItems();
                return true;
            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());

                // запускаю службу планировщика  AlarmManager

                    PollService.setServiceAlarm(getActivity(), shouldStartAlarm);


                // используется, чтобы сказать Android, что содержимое меню изменилось, и меню нужно перерисовать
                getActivity().invalidateOptionsMenu();
                Log.d("PollJobService", "Обновление интерфейса");

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateItems() {
        Log.d(TAG, "updateItems");

        String query = QueryPreferences.getStoredQuery(getActivity());

        new FetchItemsTask(query).execute();


    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        progressBar = v.findViewById(R.id.progress_bar);


        //get data in background
        updateItems();

        Log.d(TAG, "onCreateView");
        mPhotoRecyclerView = v.findViewById(R.id.photo_recycler_view);
        //динамическое обновление кол-ва колонок в зависимости от ширины дисплея
        mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int spanCount = 2;
                int width = getView().getWidth();
                if (width > 1000) spanCount = 3;
                if (width > 1600) spanCount = 4;
                mPhotoRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), spanCount));
            }
        });

        //прослушка для добавления новой партии фотографий при достижении последнего пункта списка
        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                GridLayoutManager layoutManager = ((GridLayoutManager) mPhotoRecyclerView.getLayoutManager());
                super.onScrolled(recyclerView, dx, dy);
                //если сумма последней видимой позиции и всех видимых позиций больше общего количества позиций, то гружу следующую страницу
                if (layoutManager.findLastVisibleItemPosition() + layoutManager.getChildCount() >= layoutManager.getItemCount() && loadingAvailable) {
                    loadingAvailable = false;
                    //Log.d(TAG, "Last item of list");
                    PAGE++;
                    Log.d(TAG, "Page is " + PAGE);
                    //Do fetch new data
                    updateItems();
                }
            }

        });

        setupAdapter();

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //mThumbnailDownloader.clearQueue();
    }


    private void setupAdapter() {
        //если фрагмент присоединен к активности
        if (isAdded()) {
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }


    private class PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        private ImageView mImageView;
        private GalleryItem mGalleryItem;

        public PhotoHolder(View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.item_image_view);
            itemView.setOnClickListener(this);
        }

        public void bindDrawable(String uri) {
            Picasso.get().load(mGalleryItem.getURL()).into(mImageView);
        }

        public void bindGalleryItem(GalleryItem galleryItem) {
            mGalleryItem = galleryItem;
        }
        @Override
        public void onClick(View v) {
           /* Intent i = new Intent(Intent.ACTION_VIEW, mGalleryItem.
                    getPhotoPageUri());*/
            Intent i = PhotoPageActivity
                    .newIntent(getActivity(), mGalleryItem.getPhotoPageUri());
            Log.d(TAG, mGalleryItem.getPhotoPageUri().toString());
            startActivity(i);
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

            GalleryItem item = mGalleryItem.get(position);
            holder.bindGalleryItem(item);
            Log.d(TAG, "Position is: " + position + " and ID is: " + item.getId());

            //установаливаю картинку
            holder.bindDrawable(item.getURL());
        }

        @Override
        public int getItemCount() {
            return mGalleryItem.size();
        }
    }

    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>> {

        private String mQuery;


        public FetchItemsTask(String query) {
            mQuery = query;
            Log.d(TAG, "Fetch ItemTask constructor");
        }

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }


        @Override
        protected List<GalleryItem> doInBackground(Void... voids) {

            Log.d(TAG, "Fetch ItemTask doInBackground");

            if (mQuery == null) {
                return new FlickrFetchr().fetchRecentPhotos();
            } else {
                return new FlickrFetchr().searchPhotos(mQuery);
            }
        }

        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) {

            Log.d(TAG, "Fetch ItemTask onPostExecute");

            //mItems.clear();
            //mItems.addAll(galleryItems);
            //setupAdapter();
           for (GalleryItem item : galleryItems){

               mItems.add(item);
               mPhotoRecyclerView.getAdapter().notifyItemChanged(mItems.size() - 1);
           }

            loadingAvailable = true;
            progressBar.setVisibility(View.GONE);


        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        //mThumbnailDownloader.quit();
        Log.d(TAG, "Background thread destroyed");
    }

    public static String getPAGE() {
        return String.valueOf(PAGE);
    }

    /*private void downloadToCache(String url) {
        try {
            if (url == null) {
                return;
            }
            if (Cache.getInstance().getLru().get(url) == null) {
                byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
                Cache.getInstance().getLru().put(url, bitmap);
                Log.d(TAG, "Download to cache");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/
}
