package lucaleone.mapsprototype;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.TravelMode;

import org.joda.time.DateTime;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilderFactory;

import es.atrapandocucarachas.gpxparser.model.Gpx;
import es.atrapandocucarachas.gpxparser.model.Trkpt;
import es.atrapandocucarachas.gpxparser.parser.GpxParser;


public class GoogleMapsActivity extends FragmentActivity implements OnMapReadyCallback/*, LocationListener*/ {
    private final int GPS_PERMISSION = 0;
    private GoogleMap mMap;
    private ArrayList<Location> tracking = new ArrayList<>();
    private LocationManager locationManager;
    private GPSTracking gpsService;
    private boolean bound = false;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            GPSTracking.MyBinder binder = (GPSTracking.MyBinder) service;
            gpsService = binder.getService();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
        }
    };

    //ACTIVITY OVERRIDE METHOD
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_maps);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // SupportMapFragment provide the space to put the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        // Take control of GUI component
        Button stopTracking = findViewById(R.id.stopTracking);
        stopTracking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bound) {
                    tracking.addAll(gpsService.getLocations());
                    unbindService(serviceConnection);
                }
                DateTime now = new DateTime();
                File file = new File(getExternalFilesDir(null),"track" + now.toString().substring(0, 10) + "-" + now.toString().substring(11,19) + ".gpx");
                try {
                    ArrayList<List<Location>> listOf = new ArrayList<>();
                    listOf.add(tracking);
                    GPXWriter.writePathLocation(file, "pippo", listOf);
                } catch(IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        Button startTracking = findViewById(R.id.startTracking);
        startTracking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(GoogleMapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(GoogleMapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, GPS_PERMISSION);
                } else {
                    Intent service = new Intent(GoogleMapsActivity.this, GPSTracking.class);
                    bindService(service, serviceConnection, Context.BIND_AUTO_CREATE);
                }

            }
        });
    }

    protected void onDestroy() {
        if(bound) {
            unbindService(serviceConnection);
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case GPS_PERMISSION: {
                // If request is cancelled, the result arrays are empty
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent service = new Intent(GoogleMapsActivity.this, GPSTracking.class);
                    bindService(service, serviceConnection, Context.BIND_AUTO_CREATE);
                } else {
                    Toast.makeText(this, "GPS necessario per usare l'app", Toast.LENGTH_LONG).show();
                    System.exit(1);
                }
            }
        }
    }

    //OTHER METHOD
    private void requestLocationUpdate() {
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
                alertDialog.setTitle("GPS non abilitato");
                alertDialog.setMessage("Il gps non è abilitato ed è necessario per usufruire dell'applicazione. Scegli Impostazioni per abilitarlo o Esci per chiudere l'applicazione");
                // On pressing Impostazioni
                alertDialog.setPositiveButton("Impostazioni", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                });
                // On pressing Esci
                alertDialog.setNegativeButton("Esci", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        System.exit(0);
                    }
                });
                // Show Alert
                alertDialog.create().show();

        }
        // Check the permission to use gps to track (API >= 23 with prompt, else the permission is always granted installing the app)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, GPS_PERMISSION);
        } else {
        }
    }

   /*
   VERSIONE DI MERGE CHE GENERA UN FILE GPX CONTENENTE TUTTI I TRACCIATI
   private void mergeAllGPXAndVisualize() {

        try {
            //Foreach single track file get all the points
            File[] files = getExternalFilesDir(null).listFiles();
            //HashSet<LatLng> uniquePoints = new HashSet<>(); // It contains a set of unique point
            Hashtable<LatLng, Integer> tableOfPoint = new Hashtable<>();
            ArrayList<List<LatLng>> listOf = new ArrayList<>(); // It contains a list of track
            ArrayList<Trkpt> trackPoint;
            ArrayList<LatLng> points;
            for(File file : files) {
                if(file.getName().contains("track")) {
                    InputStream is = new FileInputStream(getExternalFilesDir(null) + "/" + file.getName());
                    GpxParser parser = new GpxParser(is);
                    Gpx gpxFile = parser.parse();
                    trackPoint = gpxFile.getTrks().get(0).getTrkseg();
                    points = new ArrayList<>();
                    for(Trkpt point : trackPoint) {
                        Integer hit;
                        if((hit = tableOfPoint.get(point.getLatLon())) == null) {
                            tableOfPoint.put(point.getLatLon(), 1);
                        } else {
                            tableOfPoint.put(point.getLatLon(), hit+1);
                        }
                        //uniquePoints.add(point.getLatLon());
                        points.add(point.getLatLon());
                    }
                    listOf.add(points);
                    is.close();
                }
            }

            //With all the points create the gpx merged file
            File file = new File(getExternalFilesDir(null),"MergedTrack.gpx");
            GPXWriter.writePathLatLng(file, "merged", listOf);

            //MAPPA
            for(List<LatLng> list : listOf) {
                mMap.addPolyline(new PolylineOptions().addAll(list));
            }
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(listOf.get(0).get(0), 13));
            Toast.makeText(this, "Track loaded", Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }*/

    private void mergeAllGPXAndVisualize() {
        try {
            //Foreach single track file get all the points
            File[] files = getExternalFilesDir(null).listFiles();
            ArrayList<Trkpt> trackPoint;
            ArrayList<List<String>> listOf = new ArrayList<>();
            ArrayList<String> streets;

            for(File file : files) {
                if (file.getName().contains("track")) { // Foreach track file
                    // Parse file to have a list of point
                    InputStream is = new FileInputStream(getExternalFilesDir(null) + "/" + file.getName());
                    GpxParser parser = new GpxParser(is);
                    Gpx gpxFile = parser.parse();
                    trackPoint = gpxFile.getTrks().get(0).getTrkseg();
                    // Analyze each point and get the list of street of the path
                    String precedentStreet;
                    streets = new ArrayList<>();
                    Geocoder geocoder = new Geocoder(this, Locale.ITALY);
                    Trkpt point =  trackPoint.get(0);
                    List<Address> addresses = geocoder.getFromLocation(point.getLat(), point.getLon(), 1);
                    // First point
                    precedentStreet = addresses.get(0).getAddressLine(0).split(",")[0];
                    streets.add(precedentStreet + ";" + point.getLat() + ";" + point.getLon());
                    for (int i = 1; i < trackPoint.size()-1; i++) { // Other points but last
                        point =  trackPoint.get(i);
                        addresses = geocoder.getFromLocation(point.getLat(), point.getLon(), 1);
                        if (!precedentStreet.equals(addresses.get(0).getAddressLine(0).split(",")[0])) {
                            streets.add(precedentStreet + ";" + trackPoint.get(i-1).getLat() + ";" + trackPoint.get(i-1).getLon());
                            precedentStreet = addresses.get(0).getAddressLine(0).split(",")[0];
                            streets.add(precedentStreet + ";" + point.getLat() + ";" + point.getLon());
                        }
                    }
                    // Last point
                    point =  trackPoint.get(trackPoint.size() - 1);
                    precedentStreet = addresses.get(0).getAddressLine(0).split(",")[0];
                    streets.add(precedentStreet + ";" + point.getLat() + ";" + point.getLon());

                    listOf.add(streets);
                    is.close();
                }
            }
            //TODO: Creazione del file e invio al server
            // Remove single point and create the list of street with hit counter
            for(List<String> path : listOf) {
                // If the path contains only a street
                for (int i = 0; i < path.size() - 2; i++) {
                    if (path.get(i).equals(path.get(i + 1))) {
                        path.remove(i);
                        path.remove(i);
                        if ((i > 0 && i < path.size()) && path.get(i).split(";")[0].equals(path.get(i - 1).split(";")[0])) {
                            path.remove(i);
                            path.remove(i - 1);
                            i = i - 1;
                        }
                    }
                }
            }
            System.out.println();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void getXMLTrafficDataAndVisualize() {
        try {
            DownLoadTrafficDataThread downloadFeed = new DownLoadTrafficDataThread();
            downloadFeed.start();
            downloadFeed.join();
            ArrayList<LatLng> trafficCam = new ArrayList<>();
            ArrayList<Float> speeds = new ArrayList<>();
            Document doc = downloadFeed.getFile();
            Element root = doc.getDocumentElement();
            NodeList fdt = root.getElementsByTagName("FDT_data");
            for(int i = 0; i < fdt.getLength(); i++) {
                Node current = fdt.item(i);
                double lat = Double.valueOf(current.getAttributes().item(5).getNodeValue());
                double lng = Double.valueOf(current.getAttributes().item(6).getNodeValue());
                trafficCam.add(new LatLng(lat, lng));
                Node speedflow = current.getChildNodes().item(1);
                float speed = Float.valueOf(speedflow.getAttributes().item(0).getNodeValue());
                speeds.add(speed/60);
            }
            int i = 0;
            for(LatLng latlong : trafficCam) {
                mMap.addMarker(new MarkerOptions().position(latlong).title("" + speeds.get(i)));
                i++;
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void getGPXFromAssetAndReadIt() {
        try {
            File[] files = getExternalFilesDir(null).listFiles();
            ArrayList<File> gpxFiles = new ArrayList<>();
            for(File file : files) {
                if(file.getName().contains("track")) {
                    gpxFiles.add(file);
                }
            }
            InputStream is = new FileInputStream(getExternalFilesDir(null) + "/" + gpxFiles.get(0).getName());
            GpxParser parser = new GpxParser(is);
            Gpx file = parser.parse();
            ArrayList<LatLng> path = new ArrayList<>();
            ArrayList<Trkpt> points = file.getTrks().get(0).getTrkseg();
            for(Trkpt point : points) {
                LatLng pos = new LatLng(point.getLat(), point.getLon());
                path.add(pos);
            }
            is.close();
            //MAPPA
            mMap.addPolyline(new PolylineOptions().addAll(path));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(path.get(0), 15));
            Toast.makeText(this, "Track loaded", Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private void routingFromAToBAndDrawTrack() {
        // GeoApiContext set the Google Direction API and other restrictions
        GeoApiContext directionAPISetting = new GeoApiContext();
        directionAPISetting.setQueryRateLimit(3)
                .setApiKey(getString(R.string.google_maps_key))
                .setConnectTimeout(1, TimeUnit.SECONDS)
                .setReadTimeout(1, TimeUnit.SECONDS)
                .setWriteTimeout(1, TimeUnit.SECONDS);
        // Ask Google the route between point A and B (point can be passed as string address or LatLong)
        String pointA = "Corso Svizzera, 91, Torino";
        String pointB = "Corso Regina Margherita, 360, Torino";
        DateTime now = new DateTime();
        try {
            DirectionsResult result = DirectionsApi.newRequest(directionAPISetting)
                    .mode(TravelMode.WALKING)
                    .origin(pointA)
                    .destination(pointB)
                    .departureTime(now)
                    .await();
            // Extract the path from the result
            List<LatLng> path = PolyUtil.decode(result.routes[0].overviewPolyline.getEncodedPath());
            mMap.addPolyline(new PolylineOptions().addAll(path));
            // Set marker and move camera near the start path
            LatLng start = new LatLng(result.routes[0].legs[0].startLocation.lat, result.routes[0].legs[0].startLocation.lng);
            mMap.addMarker(new MarkerOptions().position(start));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(start, 15));
        } catch (Exception ex) {
            Toast.makeText(this, "Error loading path", Toast.LENGTH_LONG).show();
        }
    }
    //ONMAPREADYCALLBACK METHOD IMPLEMENT
    /**
     * This method is called when the map is loaded
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //requestLocationUpdate();
        //routingFromAToBAndDrawTrack();
        //getGPXFromAssetAndReadIt();
        //getXMLTrafficDataAndVisualize();
        mergeAllGPXAndVisualize();
        //mMap.setTrafficEnabled(true); // It visualizes google traffic's info
    }

    //INNER CLASS
    private class DownLoadTrafficDataThread extends Thread {
        Document doc;

        @Override
        public void run() {
            try {
                URL url = new URL("http://opendata.5t.torino.it/get_fdt");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(60000); // Timeout in ms
                connection.connect();
                InputStream in = connection.getInputStream();
                doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
                connection.disconnect();
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }

        public Document getFile() {
            return doc;
        }
    }
}