package com.github.matt.williams.blook8r;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.FloatMath;

// TODO: Not yet a service - would be good to make it so!
public class Blook8rService implements LeScanCallback {
    private static final String TAG = "Blook8rService";
    private BluetoothAdapter mBluetoothAdapter;
    private Location mLastLocation = null;
    private final List<RSSIReading> mReadings = new ArrayList<RSSIReading>();
    private final Map<String,Beacon> mBeacons = new HashMap<String,Beacon>();
    private Listener mListener;
    private static final int MIN_BEACONS = 1; // Minimum number of beacons for position TODO: Increase this post testing.
    private static final float LOCATION_UPDATE_ALPHA = 0.1f;
    private static final long EXPIRY_TIME_MILLIS = 5000; // Expire readings after 5s.
    {
        // TODO: Load this dynamically
        addBeacon("StickNFind 1", "EB:36:B8:95:B3:75", new Location(-7.0f, -7.0f), -56);
        addBeacon("StickNFind 2", "CF:BF:5E:21:65:B8", new Location(7.0f, 7.0f), -56);
        addBeacon("nRF LE 1", "00:18:AA:C0:FF:EF", new Location(20.0f, 20.0f), -56);
        addBeacon("nRF LE 2", "01:18:AA:C0:FF:EF", new Location(20.0f, 10.0f), -56);
    }

    public static class Location {
        public float x;
        public float y;

        public Location() {}
        public Location(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public void set(Location location) {
            x = location.x;
            y = location.y;
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }

    private static class Beacon {
        private final String name;
        private final Location location;
        private final int signalStrength;

        public Beacon(String name, Location location, int signalStrength) {
            this.name = name;
            this.location = location;
            this.signalStrength = signalStrength;
        }

        @Override
        public String toString() {
            return name + " " + location;
        }
    }

    private void addBeacon(String name, String macAddress, Location location, int rssi) {
        mBeacons.put(macAddress, new Beacon(name, location, rssi));
    }

    private static class RSSIReading {
        private final Beacon beacon;
        private int rssi;
        private long timestamp;

        public RSSIReading(Beacon beacon) {
            this.beacon = beacon;
        }

        public void setRSSI(int rssi) {
            this.rssi = rssi;
            timestamp = System.currentTimeMillis();
        }

        public RSSIReading withRSSI(int rssi) {
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
        mListener = listener;

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

    public void stop() {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.stopLeScan(this);
            mBluetoothAdapter = null;
        }
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        android.util.Log.e(TAG, "Got Device " + device.getName() + " with MAC " + device.getAddress());
        Beacon beacon = mBeacons.get(device.getAddress());
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

    /// Returns the distance from here to the source of RSSI1 over the distance from here to the source of RSSI2
    public static float rssiToDistanceRatio(int rssi1, int rssi2) {
        // RSSI is in dB, so is 10*log10(power)
        // Hence, power is 10^(rssi/10)
        // Ratio of powers is 10^((rssiA - rssiB) / 10)
        // Assuming that signal strength degrades as 1/(r^2), i.e. no obstructions
        // Ratio of distances is 1/sqrt(10^((rssiA - rssiB) / 10))
        // This is 10^((rssiB - rssiA) / 20)
        return FloatMath.pow(10, (rssi2 - rssi1) / 20.0f);
    }

    public static float ratioToAlpha(float a_over_b) {
        // We have a / b and we want to calculate a / (a + b).
        // TODO: Check for overlow (shouldn't happen as a_over_b is never zero.
        float alpha = 1 / ((1 / a_over_b) + 1);
        android.util.Log.d(TAG, "Translated a_over_b (" + a_over_b + ") to " + alpha);
        return alpha;
    }

    public void updateLocation(float x, float y) {
        // TODO: Smooth based on confidence/time interval since last update.
        float alpha = LOCATION_UPDATE_ALPHA;
        if (mLastLocation == null) {
            mLastLocation = new Location();
            alpha = 1.0f;
        }
        mLastLocation.x = x * alpha + mLastLocation.x * (1 - alpha);
        mLastLocation.y = y * alpha + mLastLocation.y * (1 - alpha);
        mListener.onLocationChanged(mLastLocation, 0.0f);
    }

    public void recalculateLocation() {
        android.util.Log.d(TAG, "Got readings " + Arrays.toString(mReadings.toArray(new RSSIReading[0])));
        // Sometimes I wish I was writing Python or Ruby - remove any out-of-date readings from the list
        Iterator<RSSIReading> it = mReadings.iterator();
        long expiryTimestamp = System.currentTimeMillis() - EXPIRY_TIME_MILLIS;
        while (it.hasNext()) {
            if (it.next().timestamp < expiryTimestamp) {
                it.remove();
            }
        }
        if (mReadings.size() >= MIN_BEACONS) {
            switch (mReadings.size()) {
            case 1:
                // Only one reading - assume at the beacon.
                updateLocation(mReadings.get(0).beacon.location.x, mReadings.get(0).beacon.location.y);
                break;
            case 2:
                // 2 readings - assume between them.
                RSSIReading reading1 = mReadings.get(0);
                RSSIReading reading2 = mReadings.get(1);
                float alpha = ratioToAlpha(rssiToDistanceRatio(reading1.rssi - reading1.beacon.signalStrength,
                                                               reading2.rssi - reading2.beacon.signalStrength));
                Location location1 = reading1.beacon.location;
                Location location2 = reading2.beacon.location;
                updateLocation(location1.x * (1 - alpha) + location2.x * alpha, location1.y * (1 - alpha) + location2.y * alpha);
                break;
            default:
                Collections.sort(mReadings, new Comparator<RSSIReading>() {
                    @Override
                    public int compare(RSSIReading reading1, RSSIReading reading2) {
                        return reading1.rssi - reading2.rssi;
                    }
                });
                // TODO: Should probably calculate ratio between two beacons and then solve resulting ellipses - this would eliminate differences in receiver sensitivity.
                float distance[] = new float[3];
                Location location[] = new Location[3];
                // Calculate distance from 3 strongest beacons.
                for (int index = 0; index < 3; index++) {
                    RSSIReading reading = mReadings.get(index);
                    // Signal strength is at 1 distance unit, so ratio corresponds to actual distance.
                    distance[index] = rssiToDistanceRatio(reading.rssi, reading.beacon.signalStrength);
                    location[index] = reading.beacon.location;
                }
                float a1 = location[0].x; float b1 = location[0].y; float d1 = distance[0];
                float a2 = location[1].x; float b2 = location[1].y; float d2 = distance[1];
                float a3 = location[2].x; float b3 = location[2].y; float d3 = distance[2];
                // Maths borrowed from http://www.ece.ucdavis.edu/~chuah/classes/eec173B/eec173b-s05/students/BluetoothTri_ppt.pdf:
                {
                    float A, B, C, D, E, F, Det, DetX, DetY;
                    A = -2*a1 + 2*a2;
                    B = -2*b1 + 2*b2;
                    C = -2*a2 + 2*a3;
                    D = -2*b2 + 2*b3;
                    E = FloatMath.pow(d1, 2) - FloatMath.pow(d2, 2) - FloatMath.pow(a1, 2) + FloatMath.pow(a2, 2) - FloatMath.pow(b1, 2) + FloatMath.pow(b2, 2);
                    F = FloatMath.pow(d2, 2) - FloatMath.pow(d3, 2) - FloatMath.pow(a2, 2) + FloatMath.pow(a3, 2) - FloatMath.pow(b2, 2) + FloatMath.pow(b3, 2);
                    //Using Cramer’s Rule
                    Det = A*D -B*C;
                    DetX= E*D -B*F;
                    DetY = A*F -E*C;

                    android.util.Log.d(TAG, "Det = " + Det + ", DetX = " + DetX + ", DetY = " + DetY);
                    updateLocation(DetX / Det, DetY / Det);
                }
            }
        }
    }
}
