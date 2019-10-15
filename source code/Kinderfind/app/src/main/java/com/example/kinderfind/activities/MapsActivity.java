package com.example.kinderfind.activities;


import com.example.kinderfind.adapters.KindergartenAdapter;
import com.example.kinderfind.adapters.LocalStorage;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kinderfind.models.Kindergarten;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.location.Location;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kinderfind.R;
import com.example.kinderfind.utils.UnitConversionUtil;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;

import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.maps.model.Marker;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.FirebaseAuth;
import com.arlib.floatingsearchview.FloatingSearchView;

public class MapsActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener {

    //google map declarations
    private GoogleMap mMap;
    private boolean mLocationPermissionGranted;
    private static final String TAG = MapsActivity.class.getSimpleName();
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";
    private static final int DEFAULT_ZOOM = 14;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private final LatLng mDefaultLocation = new LatLng(1.3553794, 103.8677444);
    private CameraPosition mCameraPosition;
    private Location mCurrentLocation;
    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationProviderClient;
    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;

    private RecyclerView recyclerView;

    private KindergartenAdapter kindergartenAdapter;

    //local storage
    private LocalStorage localStorage;
    //kindergarten arraylist
    private ArrayList<Kindergarten> kindergartenArrayList = new ArrayList<>();
    //search view
    private FloatingSearchView pSearchView;

    //for bottom sheet
    private BottomSheetBehavior bottomSheetBehavior;
    private TextView title;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //declaration of variable
        ImageButton profileBtn = findViewById(R.id.mainProfileBtn);
        localStorage = new LocalStorage(getApplicationContext());
        pSearchView = findViewById(R.id.floating_search_view);

        //Scroll View
        View scrollView = findViewById(R.id.kindergarten_sv);
        bottomSheetBehavior = BottomSheetBehavior.from(scrollView);

        //Kindergarten Recycler View
        title = findViewById(R.id.title_tv);
        recyclerView = findViewById(R.id.kindergarten_rv);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        //------start calling functions--------

        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int i) {
                if (i == BottomSheetBehavior.STATE_HIDDEN)
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }

            @Override
            public void onSlide(@NonNull View view, float v) {

            }
        });
        //start async task
        startAsyncTask();

        //recyclerview
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        //kindergartenAdapter = new KindergartenAdapter(kindergartenArrayList, this);
        //recyclerView.setAdapter(kindergartenAdapter);

        //googlemap
        if (savedInstanceState != null) {
            mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }
        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mapFragment.getMapAsync(this);

        //set google map locate me button
        View locationButton = ((View) mapFragment.getView().findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
        rlp.setMargins(0, 200, 50, 0);

        //profile button
        profileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(MapsActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
                Log.d(TAG, "onClick: profilebtn");
                ;
            }
        });

        // Search bar query
        pSearchView.setOnQueryChangeListener(new FloatingSearchView.OnQueryChangeListener() {
            @Override
            public void onSearchTextChanged(String oldQuery, final String newQuery) {

                kindergartenAdapter.getFilter().filter(newQuery.trim());

                if (kindergartenAdapter.getItemCount() == 0) {
                    bottomSheetBehavior.setPeekHeight(UnitConversionUtil.convertDpToPx(100));

                    title.setText("No Results Available");
                } else {

                    bottomSheetBehavior.setPeekHeight(UnitConversionUtil.convertDpToPx(300));

                    title.setText("Hawker Centres Nearby");
                }
            }
        });
    }

    /**
     * called whenever user returns back to page
     */
    @Override
    public void onResume(){
        super.onResume();

        //call for device location
        getLocationPermission();
        getDeviceLocation();

    }

    public void startAsyncTask(){
        loadData task = new loadData(this);
        task.execute();
    }

    private static class loadData extends AsyncTask<String, Void, String>{
        private WeakReference<MapsActivity> activityWeakReference;

        loadData(MapsActivity activity){
            activityWeakReference = new WeakReference<MapsActivity>(activity);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            MapsActivity activity = activityWeakReference.get();
            if(activity == null || activity.isFinishing()){
                return;
            }
        }

        @Override
        protected String doInBackground(String... strings) {
            MapsActivity activity = activityWeakReference.get();
            if(activity == null || activity.isFinishing()){
                return "Null or Empty";
            }
            return "Loaded Data";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            MapsActivity activity = activityWeakReference.get();
            if(activity == null || activity.isFinishing()){
                return;
            }

            activity.kindergartenArrayList = activity.localStorage.getFromSharedPreferences();
            activity.kindergartenAdapter = new KindergartenAdapter(activity.kindergartenArrayList, activity);
            activity.recyclerView.setAdapter(activity.kindergartenAdapter);
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

    /**
     * when map is done loading calls this, call through onmapreadycallback
     */
    public void onMapReady(GoogleMap map) {
        mMap = map;
        dropMarkers();
        // Prompt the user for permission.
        getLocationPermission();
        // Get the current location of the device and set the position of the map.
        getDeviceLocation();
        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();
    }

    /**
     * Update google buttons
     */
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
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }
    /**
     * get device location / get users location and calling sortDetails();
     */
    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));

                            sortDetails();
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }

                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    /**
     * Drop markers into google map
     */
    public void dropMarkers() {
        if (mMap == null) {
            return;
        }
        try {

            kindergartenArrayList = localStorage.getFromSharedPreferences();
            if (kindergartenArrayList.size() != 0) {
                for (Kindergarten k : kindergartenArrayList) {
                    mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(k.getLatitude(), k.getLongtitude()))
                            .title(k.getCentre_name())).setTag(k.getCenter_code());
                    // Set a listener for marker click.
                    mMap.setOnMarkerClickListener((GoogleMap.OnMarkerClickListener) this);
                }
            } else
                Log.d(TAG, "dropMarkers: empty kindergartenArraylist");
        } catch (SecurityException e) {
            Log.d(TAG, "dropMarkers: unable to store markers");
        }
    }
    /**
     * Called when the user clicks a marker.
     */
    @Override
    public boolean onMarkerClick(final Marker marker) {

        // Retrieve the centrecode from the marker.
        String centreName = (String) marker.getTitle();

        if (centreName != null) {
            pSearchView.setSearchText(centreName);
            kindergartenAdapter.getFilter().filter(centreName);
        }

        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }

    /**
     * Sort details of kindergarten arraylist by distance with current location
     */
    private void sortDetails() {
        if (kindergartenArrayList != null && mLastKnownLocation != null) {
            Location currLocation = new Location("");
            currLocation.setLatitude(mLastKnownLocation.getLatitude());
            currLocation.setLongitude(mLastKnownLocation.getLongitude());

            for (Kindergarten k : kindergartenArrayList) {
                Location location = new Location("");
                location.setLatitude(k.getLatitude());
                location.setLongitude(k.getLongtitude());
                k.setDistance(currLocation.distanceTo(location));
            }

            Collections.sort(kindergartenArrayList);

            if (kindergartenAdapter != null) {
                kindergartenAdapter.setKindergartens(kindergartenArrayList);
                kindergartenAdapter.notifyDataSetChanged();
            }
        }
    }

}
