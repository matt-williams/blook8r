package com.github.matt.williams.blook8r;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;

// TODO: Not yet a service - would be good to make it so!
public class Blook8rService implements LeScanCallback {
    private static final String TAG = "Blook8rService";
    private BluetoothAdapter mBluetoothAdapter;
    private final Location mLastLocation = new Location();
    private final List<RSSIReading> mReadings = new ArrayList<RSSIReading>();
    private final Map<String,Beacon> mBeacons = new HashMap<String,Beacon>();
    {
        addBeacon("nRF LE", new Location(0.0f, 0.0f));
        addBeacon("nRF L2", new Location(10.0f, 10.0f));
    }

    public static class Location {
        public float x;
        public float y;

        public Location() {}
        public Location(float x, float y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }

    private static class Beacon {
        private final String name;
        private final Location location;

        public Beacon(String name, Location location) {
            this.name = name;
            this.location = location;
        }

        @Override
        public String toString() {
            return name + " " + location;
        }
    }

    private void addBeacon(String name, Location location) {
        mBeacons.put(name, new Beacon(name, location));
    }

    private static class RSSIReading {
        private final Beacon beacon;
        private float rssi;

        public RSSIReading(Beacon beacon) {
            this.beacon = beacon;
        }

        public void setRSSI(float rssi) {
            this.rssi = rssi;
        }

        public RSSIReading withRSSI(float rssi) {
            setRSSI(rssi);
            return this;
        }

        @Override
        public String toString() {
            return beacon + " (RSSI " + rssi + ")";
        }
    }

    public static interface Listener {
        public void onLocationChanged(Location location, float error);
    }

    public boolean start(Context context, Listener listener) {
        final BluetoothManager bluetoothManager = (BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if ((bluetoothAdapter != null) && bluetoothAdapter.isEnabled()) {
            mBluetoothAdapter = bluetoothAdapter;
            bluetoothAdapter.startLeScan(this);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        android.util.Log.e("Main", "Got Device " + device.getName());
        Beacon beacon = mBeacons.get(device.getName());
        if (beacon != null) {
            boolean needNewReading = true;
            for (RSSIReading reading : mReadings) {
                if (reading.beacon == beacon) {
                    android.util.Log.d(TAG, "Updating RSSIReading " + reading + " with RSSI " + rssi);
                    reading.setRSSI(rssi);
                    needNewReading = false;
                    break;
                }
            }
            if (needNewReading) {
                android.util.Log.d(TAG, "Creating new RSSIReading for beacon " + beacon + " and RSSI " + rssi);
                mReadings.add(new RSSIReading(beacon).withRSSI(rssi));

            }
            recalculateLocation();
        }
    }

    public void recalculateLocation() {
        android.util.Log.d(TAG, "Got readings " + Arrays.toString(mReadings.toArray(new RSSIReading[0])));
        // TODO: Some fun maths
    }
}
