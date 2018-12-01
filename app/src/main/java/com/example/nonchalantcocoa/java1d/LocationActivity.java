package com.example.nonchalantcocoa.java1d;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
//import com.google.android.maps.GeoPoint;


public class LocationActivity extends AppCompatActivity implements OnMapReadyCallback {

    MapView mapView;
    private GoogleMap mMap;
    private Marker currentLocationMarker;
    private LatLng location;
    private Map users;
    private double radius;

    private SeekBar radiusBar;
    private Button locationNextButton;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mSessionDatabaseReference;

    public final String TAG = "Logcat";

    // The entry points to the Places API.
    private GeoDataClient mGeoDataClient;
    private PlaceDetectionClient mPlaceDetectionClient;

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationProviderClient;

    public static String hostName;

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;

    private static long idCounter = 100;

    EditText mapSearchBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mSessionDatabaseReference = mFirebaseDatabase.getReference().child("Sessions");

        radiusBar = findViewById(R.id.bar_radius);
        locationNextButton = findViewById(R.id.location_next_button);


        locationNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Let user choose a location (Currently only send the current location to Firebase)

                hostName = MainActivity.mUsername.toUpperCase().replaceAll("\\s+","") + createID();
                // Set global variable hostName as current userName
                Globals g = Globals.getInstance();
                g.setHostName(hostName); //formated hostName
                location = new LatLng(mLastKnownLocation.getLatitude(),mLastKnownLocation.getLongitude());
                users = new HashMap<String,Boolean>();
                users.put(MainActivity.mUsername,true);
                radius = radiusBar.getProgress() / 5.0;

                Host host = new Host(location, users, radius);
                mSessionDatabaseReference.child(g.getHostName()).setValue(host);
                Intent intent = new Intent(LocationActivity.this, HostWaitActivity.class);
                startActivity(intent);
            }
        });

        // Map
        mapView = findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // Construct a GeoDataClient.
        mGeoDataClient = Places.getGeoDataClient(this);

        // Construct a PlaceDetectionClient.
        mPlaceDetectionClient = Places.getPlaceDetectionClient(this);

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        mapSearchBox = (EditText) findViewById(R.id.search);



    }
    public void init(){
        mapSearchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                        actionId == EditorInfo.IME_ACTION_DONE ||
                        actionId == EditorInfo.IME_ACTION_GO ||
                        event.getAction() == KeyEvent.ACTION_DOWN &&
                                event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    Log.e(TAG, "You pressed ENTER");

                    // hide virtual keyboard
                    geoLocate();
                }
                return false;
            }
        });
    }
    public void geoLocate(){
        String searchString=mapSearchBox.getText().toString();
        Geocoder geocoder= new Geocoder(LocationActivity.this);
        List<Address> list= new ArrayList<>();
        try{
            list=geocoder.getFromLocationName(searchString,1);
        }catch(IOException e){
            Log.e(TAG,"geoLocate:IOexception:"+e.getMessage());
        }
        if(list.size()>0){
            Address address=list.get(0);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(address.getLatitude(),
                            address.getLongitude()), DEFAULT_ZOOM));
            hideSoftkeyboard();
        }
    }
public void hideSoftkeyboard(){
    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mapSearchBox.getWindowToken(), 0);
}
    public static synchronized String createID()
    {
        return String.valueOf(idCounter++);
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        setOnCameraMovelisterner(mMap);
        mMap.getUiSettings().isMyLocationButtonEnabled();
        mMap.getUiSettings().isZoomControlsEnabled();
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
            // Get the current location of the device and set the position of the map.
        getDeviceLocation();

    }
    public void setOnCameraMovelisterner(final GoogleMap map){
        map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                if (currentLocationMarker != null) {
                    currentLocationMarker.remove();
                }
                mLastKnownLocation.setLatitude( mMap.getCameraPosition().target.latitude);
                mLastKnownLocation.setLongitude( mMap.getCameraPosition().target.longitude);
                String snippet = String.format(Locale.getDefault(),
                        "Lat: %1$.5f, Long: %2$.5f",
                        mLastKnownLocation.getLatitude(),
                        mLastKnownLocation.getLongitude());
                currentLocationMarker= mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(mLastKnownLocation.getLatitude(),
                                mLastKnownLocation.getLongitude()))
                        .title("HERE?")
                        .snippet(snippet));
            }

        });
//        map.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
//            @Override
//            public void onCameraMoveStarted(int i) {
//                currentLocationMarker.remove();
//                mLastKnownLocation.setLatitude( mMap.getCameraPosition().target.latitude);
//                mLastKnownLocation.setLongitude( mMap.getCameraPosition().target.longitude);
//            }
//        });
//        map.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
//            @Override
//            public void onCameraMove() {
//                mLastKnownLocation.setLatitude( mMap.getCameraPosition().target.latitude);
//                mLastKnownLocation.setLongitude( mMap.getCameraPosition().target.longitude);
//            }
//        });
    }
//    public void setOnMarkerDragListener(final GoogleMap map){
//        map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
//            @Override
//            public void onMarkerDragStart(Marker marker) {
//
//            }
//
//            @Override
//            public void onMarkerDrag(Marker marker) {
//
//            }
//
//            @Override
//            public void onMarkerDragEnd(Marker marker) {
//                marker.getPosition();
//            }
//        });
//    }
//    public void setMapLongClick(final GoogleMap map) {
//        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
//            @Override
//            public void onMapLongClick(LatLng latLng) {
//                String snippet = String.format(Locale.getDefault(),
//                        "Lat: %1$.5f, Long: %2$.5f",
//                        latLng.latitude,
//                        latLng.longitude);
//
//                map.addMarker(new MarkerOptions()
//                        .position(latLng)
//                        .title("hi")
//                        .snippet(snippet));
//            }
//
//        });
//
//    }
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Task locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = (Location)task.getResult();
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            String snippet = String.format(Locale.getDefault(),
                                    "Lat: %1$.5f, Long: %2$.5f",
                                    mLastKnownLocation.getLatitude(),
                                    mLastKnownLocation.getLongitude());
                            currentLocationMarker= mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()))
                                    .title("HERE?")
                                    .snippet(snippet));
                            init();

                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch(SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
