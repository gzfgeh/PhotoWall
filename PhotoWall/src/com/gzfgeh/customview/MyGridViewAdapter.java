package com.gzfgeh.customview;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.gzfgeh.data.Images;
import com.gzfgeh.photowall.R;

public class MyGridViewAdapter extends ArrayAdapter<String> implements OnScrollListener{
	private Context context;
	private Set<BitmapWorkerTask> taskCollection; 
	private GridView gridView;
	private LruCache<String, Bitmap> lruCache;
	private int firstVisibleItem, visibleItemCount;
	private boolean isFirstEnter = true;
	
	public MyGridViewAdapter(Context context, int resource, String[] objects, GridView gridView) {
		super(context, resource, objects);
		
		this.context = context;
		this.gridView = gridView;
		taskCollection = new HashSet<MyGridViewAdapter.BitmapWorkerTask>();
		int maxMemory = (int) Runtime.getRuntime().maxMemory();  
        int cacheSize = maxMemory / 6; 
        lruCache = new LruCache<String, Bitmap>(cacheSize){
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				return bitmap.getByteCount();
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
        photo.setVisibility(View.GONE);
        final TextView tvProgress = (TextView) view.findViewById(R.id.tv_progress);
        tvProgress.setText(context.getString(R.string.progress));
        tvProgress.setVisibility(View.VISIBLE);
        photo.setTag(url);
        tvProgress.setTag(url+"-");
        setImageView(url, photo, tvProgress);  
        return view;  
	}

	private void setImageView(String imageUri, ImageView imageView, TextView textView){
		Bitmap bitmap = getBitmapFromMemoryCache(imageUri);
		
		if (bitmap != null){
			imageView.setImageBitmap(bitmap);
			imageView.setVisibility(View.VISIBLE);
			textView.setVisibility(View.GONE);
		}
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
				if (imageView != null && bitmap != null){
					imageView.setImageBitmap(bitmap);
				}
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
	
	class BitmapWorkerTask extends AsyncTask<String, Integer, Bitmap> {
		
		private String imageUrl;
		private TextView tvProgres;
		private int progress = 0;
		private int fileSize = 0;
		
		@Override
		protected Bitmap doInBackground(String... params) {
			imageUrl = params[0];
			Bitmap bitmap = downloadBitmap(imageUrl);
			if (bitmap != null)
				addBitmapToMemoryCache(imageUrl, bitmap);
			
			return bitmap;
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			Log.i("TAG", values[0] + " update");
			if (tvProgres == null)
				tvProgres = (TextView) gridView.findViewWithTag(imageUrl+"-");
			
			float j = values[0]*100/fileSize;
			tvProgres.setText(j + "%");
			super.onProgressUpdate(values);
		}



		@Override
		protected void onPostExecute(Bitmap bitmap) {
			super.onPostExecute(bitmap);
			
			tvProgres.setVisibility(View.GONE);
			ImageView imageView = (ImageView) gridView.findViewWithTag(imageUrl);
			imageView.setVisibility(View.VISIBLE);
			
			if (imageView != null && bitmap != null){
				imageView.setImageBitmap(bitmap);
			}
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
				connection .setRequestProperty("Accept-Encoding", "identity");
				InputStream inputStream = connection.getInputStream();
				fileSize = connection.getContentLength();
				if (connection.getResponseCode() == 200){
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
			        byte[] buffer = new byte[1024];
			        int len;
			        while ((len = inputStream.read(buffer)) != -1){
			            baos.write(buffer, 0, len);
			            progress += len;
			            publishProgress(progress);
			        }
			        baos.close();
			        byte[] data = baos.toByteArray();
			        
					//byte[] data = NetUtil.read(inputStream);
					bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
				}
				
				
				inputStream.close();
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
