package com.example.joseph.maps;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Image;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

//222test commit
public class MainActivity extends FragmentActivity {

    private GoogleMap mMap;
    private GroundOverlayOptions overlay;
    private LocationManager locationManager;
    private Geocoder gc;
    private LocationListener locationListener;
    private Location lastLocation;
    private double lastLatitude;
    private double lastLongitude;
    private PolylineOptions polylineOptions;
    private List<LatLng> points;
    private TextView locationTextView;
    private TextView speedTextView;
    public long currentTime;
    public long lastTime;
    public double speed;
    public double GOOD_WALK_SPEED = 3.1;
    public double DECENT_WALK_SPEED = 2.00;
    public double SLOW_WALK_SPEED = 1.00;
    public double MPS_CONVERSION_MPH = 0.44704;
    public WifiManager wifiManager;
    public WifiReceiver wifiReceiver;
    public ImageView wifiIcon;
    public FragmentManager fragMngr = getFragmentManager();
    public ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TermOfUseDialogFragment term = new TermOfUseDialogFragment();
        term.show(fragMngr,"term of service");

        speedTextView = (TextView) findViewById(R.id.speed);
        wifiIcon = (ImageView) findViewById(R.id.wifi_icon);
        locationTextView = (TextView) findViewById(R.id.location);
        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        mMap.setMyLocationEnabled(true);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        wifiReceiver = new WifiReceiver();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        //debugging
        checkWifi();

        gc = new Geocoder(this, Locale.getDefault());

        /*Location Listener*/
        /*================================================================*/
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if(locationManager.isProviderEnabled(locationManager.GPS_PROVIDER))    {
                    locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 0, 0, locationListener);
                    location.setProvider(locationManager.GPS_PROVIDER);
                    displayLocation(location);
                }
                else if(!locationManager.isProviderEnabled(locationManager.GPS_PROVIDER))    {
                    locationManager.requestLocationUpdates(locationManager.NETWORK_PROVIDER, 0, 0, locationListener);
                    location.setProvider(locationManager.NETWORK_PROVIDER);
                    displayLocation(location);
                }

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };
        if(!locationManager.isProviderEnabled(locationManager.GPS_PROVIDER))    {
            locationManager.requestLocationUpdates(locationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            lastLocation = new Location(locationManager.NETWORK_PROVIDER);
            displayLocation(lastLocation);
        }
        else    {
            locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 0, 0, locationListener);
            lastLocation = new Location(locationManager.GPS_PROVIDER);
            displayLocation(lastLocation);
        }
    }

    public void onRadioButtonClicked(View view) {

        boolean checked = ((RadioButton) view).isChecked();

        switch(view.getId()) {
            case R.id.normal_map:
                if (checked)    {
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                }
                break;
            case R.id.satellite_map:
                if (checked)    {
                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                }
                break;
            case R.id.hybrid_map:
                if (checked)    {
                    mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                }
                break;
        }
    }

    public void displayLocation(Location location)    {
        checkWifi();
        locationTextView.setText("Latitude: " + location.getLatitude()
                + "\nLongitude: " + location.getLongitude());
        if(location.getLongitude() != 0 && location.getLatitude() != 0 && lastLatitude !=0 && lastLongitude != 0) {
            LatLng mapCenter = new LatLng(location.getLatitude(), location.getLongitude());

            lastTime = currentTime;
            currentTime = location.getTime();

            findSpeed(location.getLatitude(),location.getLongitude(), lastLatitude, lastLongitude,lastTime,currentTime);
            speedTextView.setText("Speed: "+(Math.round(speed*100.0)/100.0)+" MPH");

            polylineOptions = new PolylineOptions().geodesic(true)
                    .add(new LatLng(location.getLatitude(), location.getLongitude()), new LatLng(lastLatitude, lastLongitude))
                    .width(25)
                    .color(setPolyLineColor(speed));


            mMap.addPolyline(polylineOptions);
        }   else    {
            LatLng mapCenter = new LatLng(location.getLatitude(), location.getLongitude());

            currentTime = location.getTime();
            lastTime = location.getTime();

            polylineOptions = new PolylineOptions().geodesic(true)
                    .add(new LatLng(location.getLatitude(), location.getLongitude()))
                    .width(25)
                    .color(Color.RED);

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mapCenter, 50));
            mMap.addPolyline(polylineOptions);
        }
        lastLatitude = location.getLatitude();
        lastLongitude = location.getLongitude();
    }
    /*
        Function: Finds the distance between the two points based on Lat and Long
        Parameters:
            double first latitude, first longitude, second latitude, second longitude
        Functionality:
            1. Converts the lats and longs to Radians
            2. Set r to the radius of the earth
            3. Finds the vertices of the two points P and Q
            4. Finds the Dot Product
            5. Find the new found distances in Meters
         Returns:
            double distance between two points
         Additional Information:
            This code/equation can be found:
                http://www.ridgesolutions.ie/index.php/2013/11/14/algorithm-to-calculate-speed-from-two-gps-latitude-and-longitude-points-and-time-difference/
     */
    public double findDistance(double lat1, double lon1, double lat2, double lon2)  {

        lat1 = lat1 * Math.PI / 180.00;
        lon1 = lon1 * Math.PI / 180.00;

        lat2 = lat2 * Math.PI / 180.00;
        lon2 = lon2 * Math.PI / 180.00;

        double r = 6378100;

        double rho1 = r * Math.cos(lat1);
        double z1 = r * Math.sin(lat1);
        double x1 = rho1 * Math.cos(lon1);
        double y1 = rho1 * Math.sin(lon1);

        // Q
        double rho2 = r * Math.cos(lat2);
        double z2 = r * Math.sin(lat2);
        double x2 = rho2 * Math.cos(lon2);
        double y2 = rho2 * Math.sin(lon2);

        // Dot product
        double dot = (x1 * x2 + y1 * y2 + z1 * z2);
        double cos_theta = dot / (r * r);

        double theta = Math.acos(cos_theta);

        // Distance in Metres
        return r * theta;
    }
    /*
        Function: Find thee user speed in Mile Per Hour
        Parameters:
            double first latitude, first longitude, second latitude, second longitude,
                first "timestamp", second "timestamp"
        Functionality:
            1. Finds the difference between the two distances(lat and long)
            2. Find the difference between the two times
            3. Find the speed based on meters per second
            4. *Optional* finds the kilometer per hour
            5. Sets the global "speed" by the conversion into mile per hour
         Returns:
            VOID
     */
    public void findSpeed(double lat1, double lon1, double lat2, double lon2, long time1, long time2)   {
        double distance = findDistance(lat1,lon1,lat2,lon2);
        //find the difference between the two times found
        double time = (time2 - time1)/1000.00;
        // divides distance and the time previously found in order

        double speedMPS = distance/time; //meters per second
        double speedKPH = (speedMPS*3600.00)/1000.00;
        speed = speedMPS/MPS_CONVERSION_MPH;
    }
    /*
        Function: Set the polyline color based on the speed of the user
        Parameters:
            double s which is the speed
        Functionality:
            1. If: speed is less than 2.0 (2 MPH) then the polyline will stay RED
            2. Else if: The speed is in between 2 and 3 then the polyline will return YELLOW
            3. Else: Return GREEN meaning the user is going a good speed based on Human average walking speed
         Returns:
            int Color
         Additional Information:
            The average walking speed of a human is 3.1 MPH this is based on:
                "the average human walking speed is about 5.0 kilometres per hour (km/h), or about 3.1 miles per hour (mph)"
                http://en.wikipedia.org/wiki/Walking
     */
    public int setPolyLineColor(double s)  {
        if(s < DECENT_WALK_SPEED)
            return Color.RED;
        else if(s > DECENT_WALK_SPEED && s < GOOD_WALK_SPEED)
            return Color.YELLOW;
        else
            return Color.GREEN;
    }


    public void checkWifi()  {
        List<ScanResult> scanResults = wifiManager.getScanResults();
        if(scanResults != null || scanResults.size() != 0) {
            wifiIcon.setImageResource(R.drawable.full_service_wifi);
        }
        else
            wifiIcon.setImageResource(R.drawable.full_wifi);

        if(!wifiManager.isWifiEnabled())   {
            wifiIcon.setImageResource(R.drawable.full_wifi);
        }

    }

    class WifiReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {
        }
    }
}
