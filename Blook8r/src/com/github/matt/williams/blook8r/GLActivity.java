package com.github.matt.williams.blook8r;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Menu;

import com.github.matt.williams.blook8r.Blook8rService.Location;

public class GLActivity extends Activity implements Blook8rService.Listener, SensorEventListener {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final String TAG = "GLActivity";
    private final Blook8rService blook8r = new Blook8rService();
    private GLView mGlView;
    private float[] mAccelerometerValues;
    private float[] mMagnetometerValues;
    private final float[] mR = new float[9];
    private final float[] mOrientation = new float[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glview);
        mGlView = (GLView)findViewById(R.id.glview);

        Bundle extras = getIntent().getExtras();
        if ((extras != null) &&
            (extras.containsKey("latitude"))) {
            mGlView.setTargetLocation(extras.getDouble("latitude"), extras.getDouble("longitude"));
        }
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
        ((SensorManager)getSystemService(SENSOR_SERVICE)).unregisterListener(this);

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

        SensorManager sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        Sensor magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magnetometer != null) {
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onLocationChanged(Location location, float error) {
        android.util.Log.i(TAG, "Got Location " + location);
        mGlView.setLocation(location.x, location.y);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if (mAccelerometerValues == null) {
                mAccelerometerValues = new float[event.values.length];
            }
            System.arraycopy(event.values, 0, mAccelerometerValues, 0, event.values.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            if (mMagnetometerValues == null) {
                mMagnetometerValues = new float[event.values.length];
            }
            System.arraycopy(event.values, 0, mMagnetometerValues, 0, event.values.length);
        }
        if ((mAccelerometerValues != null) && (mMagnetometerValues != null)) {
            SensorManager.getRotationMatrix(mR, null, mAccelerometerValues, mMagnetometerValues);
            SensorManager.getOrientation(mR, mOrientation);
            mGlView.setBearing(mOrientation[0] * 180 / (float)Math.PI);
        }
    }
}
