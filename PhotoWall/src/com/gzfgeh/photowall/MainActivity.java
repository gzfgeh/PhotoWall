package com.gzfgeh.photowall;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewTreeObserver;
import android.widget.GridView;

import com.gzfgeh.customview.MyGridViewAdapter;

public class MainActivity extends Activity {
	
	private GridView gridView;
	private MyGridViewAdapter myGridViewAdapter;
	private int mImageThumbSize;  
    private int mImageThumbSpacing; 
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        gridView = (GridView) findViewById(R.id.photo_wall);
        myGridViewAdapter = new MyGridViewAdapter(this, 0, com.gzfgeh.data.Images.imageThumbUrls, gridView);
        gridView.setAdapter(myGridViewAdapter);
        mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);  
        mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing); 
        
        gridView.getViewTreeObserver().addOnGlobalLayoutListener(  
                new ViewTreeObserver.OnGlobalLayoutListener() {  
                      
                    @SuppressWarnings("deprecation")
					@Override  
                    public void onGlobalLayout() {  
                        final int numColumns = (int) Math.floor(gridView.getWidth()  
                                / (mImageThumbSize + mImageThumbSpacing));  
                        
                        if (numColumns > 0) { 
                            int columnWidth = (gridView.getWidth() / numColumns)- mImageThumbSpacing;  
                            myGridViewAdapter.setItemHeight(columnWidth);  
                            gridView.getViewTreeObserver().removeGlobalOnLayoutListener(this);  
                        }  
                    }  
                });  
    }

    @Override  
    protected void onPause() {  
        super.onPause();  
        myGridViewAdapter.fluchCache();  
    }
    
	@Override
	protected void onDestroy() {
		super.onDestroy();
		myGridViewAdapter.cancelAllTasks();
	}
    
    
}
