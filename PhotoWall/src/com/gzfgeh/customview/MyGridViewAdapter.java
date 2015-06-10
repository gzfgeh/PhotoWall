package com.gzfgeh.customview;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.gzfgeh.data.Images;
import com.gzfgeh.photowall.R;

public class MyGridViewAdapter extends ArrayAdapter<String> implements OnScrollListener{

	private Set<BitmapWorkerTask> taskCollection; 
	private GridView gridView;
	private LruCache<String, Bitmap> lruCache;
	private int firstVisibleItem, visibleItemCount;
	private boolean isFirstEnter = true;
	
	public MyGridViewAdapter(Context context, int resource, String[] objects, GridView gridView) {
		super(context, resource, objects);
		
		this.gridView = gridView;
		taskCollection = new HashSet<MyGridViewAdapter.BitmapWorkerTask>();
		int maxMemory = (int) Runtime.getRuntime().maxMemory();  
        int cacheSize = maxMemory / 8; 
        lruCache = new LruCache<String, Bitmap>(cacheSize){

			@Override
			protected int sizeOf(String key, Bitmap value) {
				return value.getByteCount();
			}
        	
        };
        gridView.setOnScrollListener(this);
	}

	
	
	@SuppressLint("InflateParams") @Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final String url = getItem(position);  
        View view;  
        if (convertView == null) {  
            view = LayoutInflater.from(getContext()).inflate(R.layout.gird_view_item, null);  
        } else {  
            view = convertView;  
        }  
        final ImageView photo = (ImageView) view.findViewById(R.id.photo);  
        // 给ImageView设置一个Tag，保证异步加载图片时不会乱序  
        photo.setTag(url);  
        setImageView(url, photo);  
        return view;  
	}



	private void setImageView(String imageUri, ImageView imageView){
		Bitmap bitmap = getBitmapFromMemoryCache(imageUri);
		if (bitmap != null)
			imageView.setImageBitmap(bitmap);
		else 
			imageView.setImageResource(R.drawable.ic_launcher);
	}
	
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		this.firstVisibleItem = firstVisibleItem;
		this.visibleItemCount = visibleItemCount;
		
		if (isFirstEnter && visibleItemCount > 0){
			addInvisibleImageToCache(firstVisibleItem, visibleItemCount);
			isFirstEnter = false;
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (scrollState == SCROLL_STATE_IDLE)
			addInvisibleImageToCache(firstVisibleItem, visibleItemCount);
		else
			cancelAllTasks();
	}

	private void addBitmapToMemoryCache(String key, Bitmap bitmap){
		if (getBitmapFromMemoryCache(key) == null)
			lruCache.put(key, bitmap);
	}
	
	private Bitmap getBitmapFromMemoryCache(String key){
		return lruCache.get(key);
	}
	
	private void addInvisibleImageToCache(int firstVisibleItem, int visibleItemCount){
		for (int i = firstVisibleItem; i < firstVisibleItem + visibleItemCount; i++) {
			String imageUri = Images.imageThumbUrls[i];
			Bitmap bitmap = getBitmapFromMemoryCache(imageUri);
			if (bitmap == null){
				BitmapWorkerTask task = new BitmapWorkerTask();
				taskCollection.add(task);
				task.execute(imageUri);
			}else {
				ImageView imageView = (ImageView) gridView.findViewWithTag(imageUri);
				if (imageView != null && bitmap != null)
					imageView.setImageBitmap(bitmap);
			}
		}
	}
	
	public void cancelAllTasks() {  
        if (taskCollection != null) {  
            for (BitmapWorkerTask task : taskCollection) {  
                task.cancel(false);  
            }  
        }  
    } 
	
	class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
		
		private String imageUrl;
		
		@Override
		protected Bitmap doInBackground(String... params) {
			imageUrl = params[0];
			Bitmap bitmap = downloadBitmap(imageUrl);
			if (bitmap != null)
				addBitmapToMemoryCache(imageUrl, bitmap);
			
			return bitmap;
		}
		
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			super.onPostExecute(bitmap);
			
			ImageView imageView = (ImageView) gridView.findViewWithTag(imageUrl);
			if (imageView != null && bitmap != null)
				imageView.setImageBitmap(bitmap);
			
			taskCollection.remove(this);
		}



		private Bitmap downloadBitmap(String imageUrl){
			Bitmap bitmap = null;
			HttpURLConnection connection = null;
			try {
				URL url = new URL(imageUrl);
				connection = (HttpURLConnection) url.openConnection();
				connection.setConnectTimeout(5000);
				connection.setReadTimeout(10000);
				connection.setDoOutput(false);
				bitmap = BitmapFactory.decodeStream(connection.getInputStream());
			} catch (Exception e) {
				e.printStackTrace();
			}finally{
				if (connection != null)
					connection.disconnect();
			}
			return bitmap;
			
		}
	}
}
