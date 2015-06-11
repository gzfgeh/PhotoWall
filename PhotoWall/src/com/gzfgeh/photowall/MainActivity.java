package com.gzfgeh.photowall;

import com.gzfgeh.customview.MyGridViewAdapter;

import android.app.Activity;
import android.os.Bundle;
import android.widget.GridView;

public class MainActivity extends Activity {
	
	private GridView gridView;
	private MyGridViewAdapter myGridViewAdapter;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gridView = (GridView) findViewById(R.id.photo_wall);
        myGridViewAdapter = new MyGridViewAdapter(this, 0, com.gzfgeh.data.Images.imageThumbUrls, gridView);
        gridView.setAdapter(myGridViewAdapter);
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
		myGridViewAdapter.cancelAllTasks();
	}
    
    
}
