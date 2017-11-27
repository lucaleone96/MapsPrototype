package lucaleone.mapsprototype;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;

public class GPSTracking extends Service implements LocationListener {
    private ArrayList<Location> locations;
    private IBinder binder = new MyBinder();
    private LocationListener locationListener;
    private LocationManager locationManager;

    // SERVICE OVERRIDE METHOD
    @Override
    public void onCreate() {
        Log.i("SERVICE", "Create");
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = this;
        locations = new ArrayList<>();

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0, locationListener);
        } catch (SecurityException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        locationManager.removeUpdates(locationListener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    //SERVICE METHOD
    public ArrayList<Location> getLocations() {
        return locations;
    }

    // LOCATIONLISTENER IMPLEMENT METHOD
    @Override
    public void onLocationChanged(Location location) {
        locations.add(location);
        Log.i("NEWLOCATION", location.toString());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    // INNER CLASS
    public class MyBinder extends Binder {
        // Other activity now can call public method of the service
        public GPSTracking getService() {
            return GPSTracking.this;
        }
    }
}
