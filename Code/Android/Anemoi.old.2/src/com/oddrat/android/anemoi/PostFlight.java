package com.oddrat.android.anemoi;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;

public class PostFlight extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setTitle("post-flight stuff");
        
        setContentView(R.layout.activity_postflight);
        
    }
    
    @Override
	protected void onResume() {
		super.onResume();
	}
    
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onStop(){
		super.onStop();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
}
