package com.example.joseph.maps;

import android.app.Activity;
import android.graphics.Color;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
public class MainActivity extends Activity {

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
    public Calendar calendar;
    public long currentTime;
    public long lastTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        locationTextView = (TextView) findViewById(R.id.location);

        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        mMap.setMyLocationEnabled(true);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);


        gc = new Geocoder(this, Locale.getDefault());

        /*Location Listener*/
        /*================================================================*/
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if(locationManager.isProviderEnabled(locationManager.GPS_PROVIDER))	{
                    locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 0, 0, locationListener);
                    location.setProvider(locationManager.GPS_PROVIDER);
                    displayLocation(location);
                }
                else if(!locationManager.isProviderEnabled(locationManager.GPS_PROVIDER))	{
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
        if(!locationManager.isProviderEnabled(locationManager.GPS_PROVIDER))	{
            locationManager.requestLocationUpdates(locationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            lastLocation = new Location(locationManager.NETWORK_PROVIDER);
            displayLocation(lastLocation);
        }
        else	{
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
    public void displayLocation(Location location)	{

        locationTextView.setText("Latitude: " + location.getLatitude()
                        + " Longitude: " + location.getLongitude());
        if(location.getLongitude() != 0 && location.getLatitude() != 0 && lastLatitude !=0 && lastLongitude != 0) {
            LatLng mapCenter = new LatLng(location.getLatitude(), location.getLongitude());

            lastTime = currentTime;
            currentTime = calendar.getTimeInMillis();
            findSpeed(lastLatitude, lastLongitude, location.getLatitude(), location.getLongitude(),lastTime,currentTime);

            polylineOptions = new PolylineOptions().geodesic(true)
                    .add(new LatLng(location.getLatitude(), location.getLongitude()), new LatLng(lastLatitude, lastLongitude))
                    .width(25)
                    .color(Color.WHITE);


            mMap.addPolyline(polylineOptions);


        }   else    {
            LatLng mapCenter = new LatLng(location.getLatitude(), location.getLongitude());

            currentTime = calendar.getTimeInMillis();
            lastTime = calendar.getTimeInMillis();

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
    public double findSpeed(double lat1, double lon1, double lat2, double lon2, long time1, long time2)   {
        double distance = findDistance(lat1,lon1,lat2,lon2);
        double time = (time2 - time1)/1000.0;
        double speedMPH = distance/time;
        double speedKPH = (speedMPH*3600.0)/1000.0;
        return speedMPH;
    }


}
