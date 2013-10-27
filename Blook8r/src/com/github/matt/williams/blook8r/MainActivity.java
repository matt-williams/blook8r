package com.github.matt.williams.blook8r;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

public class MainActivity extends Activity {
	Double latitude;
	Double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        
        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri != null)
        {
        	String path = uri.toString().substring("target://".length());
        	
        	int comma = path.indexOf(",");
        	if (comma != -1)
        	{
        		String latString = path.substring(0,comma);
        		String lonString = path.substring(comma + 1);
        		latitude = Double.valueOf(latString);
        		longitude = Double.valueOf(lonString);
        	}
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    public void showMap(View view)
    {
      startViewer(new Intent(this, MapActivity.class));
    }
    
    public void showGL(View view)
    {
      startViewer(new Intent(this, GLActivity.class));
    }
    
    void startViewer(Intent intent)
    {
    	if (latitude != null)
    	{
    		intent.putExtra("latitude", latitude);
    		intent.putExtra("longitude", longitude);
    	}
    	startActivity(intent);
    }
}
