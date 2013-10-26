package com.github.matt.williams.blook8r;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

import com.github.matt.williams.blook8r.Blook8rService.Location;

public class GLActivity extends Activity implements Blook8rService.Listener {

    private static final int REQUEST_ENABLE_BT = 1;
    private final Blook8rService blook8r = new Blook8rService();
    private GLView mGlView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glview);
        mGlView = (GLView)findViewById(R.id.glview);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onPause() {
        blook8r.stop();
        mGlView.onPause();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mGlView.onResume();
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (!blook8r.start(this, this)) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    public void onLocationChanged(Location location, float error) {
        android.util.Log.e("Main", "Got Location " + location);
        mGlView.setLocation(location.x, location.y);
    }
}
