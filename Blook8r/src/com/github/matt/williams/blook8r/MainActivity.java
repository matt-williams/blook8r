package com.github.matt.williams.blook8r;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

public class MainActivity extends Activity {
	Double mLatitude;
	Double mLongitude;
	String mDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        
        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri != null)
        {
        	if (uri.getScheme() == "target")
        	{
        		// It's of the form target://<latitude>,<longitude><;optional description>
               	String path = uri.toString().substring("target://".length());
               	
               	int semi = path.indexOf(";");
               	if (semi != -1)
               	{
               		mDescription = path.substring(semi + 1);
               		path = path.substring(0,semi);
               	}
            	
            	int comma = path.indexOf(",");
            	if (comma != -1)
            	{
            		String latString = path.substring(0,comma);
            		String lonString = path.substring(comma + 1);
            		mLatitude = Double.valueOf(latString);
            		mLongitude = Double.valueOf(lonString);
            	}
        	}
        	else
        	{
        		// We assume it's a real QR code we scanned, so use hard coded values
        		mLatitude = 51.5051040211653d;
        	    mLongitude = -0.01970499652471314d;
        	    
        	    mDescription = "Christopher Ward";
        	    
        	    showMap(null);
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
    
    public void scanBarcode(View view)
    {
    	Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://zxing.appspot.com/scan"));      
    	startActivity(intent);
    }
    
    void startViewer(Intent intent)
    {
    	if (mLatitude != null)
    	{
    		intent.putExtra("latitude", mLatitude);
    		intent.putExtra("longitude", mLongitude);
    	}
    	if (mDescription != null)
    	{
    		intent.putExtra("description", mDescription);
    	}
    	startActivity(intent);
    }
}
