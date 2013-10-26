/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.matt.williams.blook8r;

import java.util.Map;

import com.github.matt.williams.blook8r.Blook8rService.Beacon;
import com.github.matt.williams.blook8r.Blook8rService.Location;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

/**
 * This shows how to add a ground overlay to a map.
 */
public class MapActivity extends FragmentActivity 
  implements OnSeekBarChangeListener,
             OnMapLongClickListener,
             Blook8rService.Listener
{

    private static final int TRANSPARENCY_MAX = 100;
    private static final LatLng PLACE = new LatLng(
    		51.50492954737005,
    		-0.01932796037294348);
    
    private GoogleMap mMap;
    private GroundOverlay mGroundOverlay;
    private SeekBar mTransparencyBar;

    private static final int REQUEST_ENABLE_BT = 1;
    private final Blook8rService blook8r = new Blook8rService();
   

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ground_overlay_demo);


        mTransparencyBar = (SeekBar) findViewById(R.id.transparencySeekBar);
        mTransparencyBar.setMax(TRANSPARENCY_MAX);
        mTransparencyBar.setProgress(0);
        
        mTransparencyBar.setVisibility(View.GONE);
        findViewById(R.id.transparencyTitle).setVisibility(mTransparencyBar.getVisibility());

        setUpMapIfNeeded();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if ((resultCode != RESULT_OK) ||
                (!blook8r.start(this, this))) {
                finish();
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
         
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (!blook8r.start(this, this)) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }
    
    @Override
    public void onPause() {
        blook8r.stop();
        super.onPause();
    }

    private void setUpMapIfNeeded() {
        if (mMap == null) {
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(PLACE, 18));

        mGroundOverlay = mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.floorplan39)).anchor(0.5f, 0.5f)
                .position(PLACE, 60f, 60f)
                .bearing(10f));
        
        mMap.setOnMapLongClickListener(this);

        mTransparencyBar.setOnSeekBarChangeListener(this);
        
        Map<String, Beacon> beacons = blook8r.getBeacons();
        for (Beacon beacon : beacons.values())
        {
        	LatLng point = new LatLng(beacon.location.y, beacon.location.x);
            mMap.addMarker(new MarkerOptions()
            .position(point)
            .title(beacon.name));
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (mGroundOverlay != null) {
            mGroundOverlay.setTransparency((float) progress / (float) TRANSPARENCY_MAX);
        }
    }
    
    @Override
    public void onMapLongClick(LatLng point) {
        
        // Creates a draggable marker. Long press to drag.
        mMap.addMarker(new MarkerOptions()
                .position(point)
                .title("Beacon")
//                .snippet("Population: 4,137,400")
                .draggable(true));
    }
    
    @Override
    public void onLocationChanged(final Location location, final float error) 
    {
      runOnUiThread(new Runnable()
      {
    	    public void run()
    	    {
    	    	didLocationChanged(location, error);
 
        }
      });
    }
    
    void didLocationChanged(Location location, float error)
    {
    	LatLng point = new LatLng(location.y, location.x);
        mMap.addMarker(new MarkerOptions()
        .position(point)
        .title("Beacon")
        .snippet("" + location.x + "," + location.y)
        .draggable(true));
        android.util.Log.e("Main", "Got Location lat:" + point.latitude + " lon:" + point.longitude);
    }
}
