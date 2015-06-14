package com.gzfgeh.customview;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

import libcore.io.DiskLruCache;
import libcore.io.DiskLruCache.Snapshot;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import com.gzfgeh.photowall.R;
import com.gzfgeh.tools.CommonTool;

public class MyGridViewAdapter extends ArrayAdapter<String>{
	private static final int DISK_CACHE_SIZE = 10 * 1024 *1024;
	private Set<BitmapWorkerTask> taskCollection; 
	private GridView gridView;
	private LruCache<String, Bitmap> lruCache;
	private DiskLruCache mDiskLruCache;
	private int mItemHeight = 0;
	
	public MyGridViewAdapter(Context context, int resource, String[] objects, GridView gridView) {
		super(context, resource, objects);
		
		this.gridView = gridView;
		taskCollection = new HashSet<MyGridViewAdapter.BitmapWorkerTask>();
		int maxMemory = (int) Runtime.getRuntime().maxMemory();  
        int cacheSize = maxMemory / 10; 
        lruCache = new LruCache<String, Bitmap>(cacheSize){
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				return bitmap.getByteCount();
			}
        };
        
        try { 
            File cacheDir = CommonTool.getDiskCacheDir(context, "thumb");  
            if (!cacheDir.exists()) {  
                cacheDir.mkdirs();  
            }  
            // 创建DiskLruCache实例，初始化缓存数据  
            mDiskLruCache = DiskLruCache.open(cacheDir, CommonTool.getAppVersion(context), 1, DISK_CACHE_SIZE);  
        } catch (IOException e) {  
            e.printStackTrace();  
        } 
        
	}
	
	@SuppressLint("InflateParams") 
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final String url = getItem(position);  
        View view;  
        if (convertView == null) {  
            view = LayoutInflater.from(getContext()).inflate(R.layout.gird_view_item, null);  
        } else {  
            view = convertView;  
        }  
        
        final ImageView photo = (ImageView) view.findViewById(R.id.photo);
        if (photo.getLayoutParams().height != mItemHeight) {  
        	photo.getLayoutParams().height = mItemHeight;  
        } 
        photo.setTag(url);
        photo.setImageResource(R.drawable.ic_launcher);
        setImageView(url, photo);  
        return view;  
	}

	private void setImageView(String imageUrl, ImageView imageView){
		Bitmap bitmap = getBitmapFromMemoryCache(imageUrl);
		
		if (bitmap != null){
			imageView.setImageBitmap(bitmap);
		}else {
			BitmapWorkerTask task = new BitmapWorkerTask();
			taskCollection.add(task);
			task.execute(imageUrl);
		}
	}
	
	public void setItemHeight(int height) {  
        if (height == mItemHeight) {  
            return;  
        }  
        mItemHeight = height;  
        notifyDataSetChanged();  
    } 
	
	
	/** 
     * 使用MD5算法对传入的key进行加密并返回。 
     */  
    public String hashKeyForDisk(String key) {  
        String cacheKey;  
        try {  
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");  
            mDigest.update(key.getBytes());  
            cacheKey = bytesToHexString(mDigest.digest());  
        } catch (NoSuchAlgorithmException e) {  
            cacheKey = String.valueOf(key.hashCode());  
        }  
        return cacheKey;  
    }  
      
    /** 
     * 将缓存记录同步到journal文件中。 
     */  
    public void fluchCache() {  
        if (mDiskLruCache != null) {  
            try {  
                mDiskLruCache.flush();  
            } catch (IOException e) {  
                e.printStackTrace();  
            }  
        }  
    }  
  
    private String bytesToHexString(byte[] bytes) {  
        StringBuilder sb = new StringBuilder();  
        for (int i = 0; i < bytes.length; i++) {  
            String hex = Integer.toHexString(0xFF & bytes[i]);  
            if (hex.length() == 1) {  
                sb.append('0');  
            }  
            sb.append(hex);  
        }  
        return sb.toString();  
    }

	private void addBitmapToMemoryCache(String key, Bitmap bitmap){
		if (getBitmapFromMemoryCache(key) == null)
			lruCache.put(key, bitmap);
	}
	
	private Bitmap getBitmapFromMemoryCache(String key){
		return lruCache.get(key);
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
			
			FileDescriptor fileDescriptor = null;  
            FileInputStream fileInputStream = null;  
            Snapshot snapShot = null;  
            try {  
                // 生成图片URL对应的key  
                final String key = hashKeyForDisk(imageUrl);  
                // 查找key对应的缓存  
                snapShot = mDiskLruCache.get(key);  	
                if (snapShot == null) {  
                    // 如果没有找到对应的缓存，则准备从网络上请求数据，并写入缓存  
                    DiskLruCache.Editor editor = mDiskLruCache.edit(key);  
                    if (editor != null) {  
                        OutputStream outputStream = editor.newOutputStream(0);  
                        if (downloadUrlToStream(imageUrl, outputStream)) {  
                            editor.commit();  
                        } else {  
                            editor.abort();  
                        }  
                    }  
                    // 缓存被写入后，再次查找key对应的缓存  
                    snapShot = mDiskLruCache.get(key);  
                }  
                if (snapShot != null) {  
                    fileInputStream = (FileInputStream) snapShot.getInputStream(0);  
                    fileDescriptor = fileInputStream.getFD();  
                }  
                // 将缓存数据解析成Bitmap对象  
                Bitmap bitmap = null;  
                if (fileDescriptor != null) {  
                    bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);  
                }  
                if (bitmap != null) {  
                    // 将Bitmap对象添加到内存缓存当中  
                    addBitmapToMemoryCache(params[0], bitmap);  
                }  
                return bitmap;  
            } catch (IOException e) {  
                e.printStackTrace();  
            } finally {  
                if (fileDescriptor == null && fileInputStream != null) {  
                    try {  
                        fileInputStream.close();  
                    } catch (IOException e) {  
                    }  
                }  
            }  
            return null;  
		}



		@Override
		protected void onPostExecute(Bitmap bitmap) {
			super.onPostExecute(bitmap);
			ImageView imageView = (ImageView) gridView.findViewWithTag(imageUrl);
			
			if (imageView != null && bitmap != null){
				imageView.setImageBitmap(bitmap);
				imageView.setVisibility(View.VISIBLE);
			}
			taskCollection.remove(this);
		}



		private boolean downloadUrlToStream(String imageUrl, OutputStream outputStream){
			HttpURLConnection connection = null;
			BufferedOutputStream out = null;  
            BufferedInputStream in = null;
			try {
				URL url = new URL(imageUrl);
				connection = (HttpURLConnection) url.openConnection();
				connection.setConnectTimeout(5000);
				connection.setReadTimeout(10000);
				connection.setDoOutput(false);
				connection .setRequestProperty("Accept-Encoding", "identity");
				InputStream inputStream = connection.getInputStream();
				in = new BufferedInputStream(inputStream, 1024*8);
				out = new BufferedOutputStream(outputStream, 1024*8);
		        int len;
		        while ((len = in.read()) != -1){
		            out.write(len);
		        }
		        inputStream.close();
		        return true;
			} catch (Exception e) {
				e.printStackTrace();
			}finally{
				if (connection != null)
					connection.disconnect();
				try {  
                    if (out != null) {  
                        out.close();  
                    }  
                    if (in != null) {  
                        in.close();  
                    }  
                } catch (final IOException e) {  
                    e.printStackTrace();  
                }  
			}
			return false;
		}
	}
}
