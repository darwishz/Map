package com.example.map;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.gms.common.api.Status;
//import com.google.android.libraries.places.widget.model.AutocompletePrediction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback , GoogleMap.OnPoiClickListener {

    private GoogleMap mMap;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    private Boolean mLocationPermissionsGranted = false;
    private FusedLocationProviderClient mfusedLocationProviderClient;
    private static final String TAG = "MainActivity";
    private Toolbar toolbar;
    private LinearLayout legendLayout;  // Add this line
    private ImageButton legendButton;   // Add this line
    private List<Crime> crimeData = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Places.initialize(getApplicationContext(), "AIzaSyBBuX8J5JTjAMb11N1OEQizDix42NYf1VU");

        crimeData.add(new Crime(37.4219999, -122.0840575, "Violent Crime"));
        crimeData.add(new Crime(37.422, -122.083, "Property Crime"));

        getLocationPermission();

        // Initialize the legend
        initLegend();

        // Initialize the legend button
        initLegendButton();  // Make sure to add this line

    }
    private void initLegend() {
        // Reference to the legend layout
        legendLayout = findViewById(R.id.legend_layout);

        // Example data structure for your legend items
        Map<String, Integer> legendItems = new HashMap<>();

        // Dynamically add items to the legend
        for (Map.Entry<String, Integer> item : legendItems.entrySet()) {
            // Create a horizontal layout for each item
            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            itemLayout.setLayoutParams(layoutParams);

            // Add icon
            ImageView icon = new ImageView(this);
            icon.setImageResource(item.getValue());
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                    convertDpToPx(24), // icon size in pixels
                    convertDpToPx(24)
            );
            icon.setLayoutParams(iconParams);
            itemLayout.addView(icon);

            // Add description
            TextView description = new TextView(this);
            description.setText(item.getKey());
            LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            descriptionParams.setMargins(convertDpToPx(8), 0, 0, 0);
            description.setLayoutParams(descriptionParams);
            itemLayout.addView(description);

            // Add the item layout to the legend layout
            legendLayout.addView(itemLayout);
        }
    }

    private void initLegendButton() {
        ImageButton legendButton = findViewById(R.id.legendButton);

        legendButton.setOnClickListener(view -> {
            LinearLayout legendLayout = findViewById(R.id.legend_layout);

            // Toggle legend visibility
            if (legendLayout.getVisibility() == View.VISIBLE) {
                legendLayout.setVisibility(View.GONE);
            } else {
                legendLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    // Utility method to convert dp to pixels
    private int convertDpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void init() {
        Log.d(TAG, "init: initializing");

        // Set up the adapter for the AutoCompleteTextView
        AutocompleteSupportFragment autocompleteFragment =
                (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS));
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                Log.d(TAG, "Place: " + place.getName() + ", " + place.getId());

                // Move the camera to the selected place
                moveCamera(place.getLatLng(), DEFAULT_ZOOM, place.getName());
            }

            @Override
            public void onError(@NonNull Status status) {
                Log.e(TAG, "onError: AutocompletePrediction: " + status.getStatusMessage());
            }
        });
    }

    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the device's current location");

        mfusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (mLocationPermissionsGranted) {
                Task<Location> location = mfusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "onComplete: found location");
                        Location currentLocation = task.getResult();

                        if (currentLocation != null) {
                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM, "My Location");
                        } else {
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MainActivity.this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "onComplete: current location task unsuccessful", task.getException());
                        Toast.makeText(MainActivity.this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }

    private void moveCamera(LatLng latLng, float zoom, String title) {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        float markerColor = getMarkerColor(title);

        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(markerColor));

        mMap.addMarker(options);
    }

    private float getMarkerColor(String crimeType) {
        Log.d(TAG, "getMarkerColor: Crime Type: " + crimeType);
        if ("Violent Crime".equals(crimeType)) {
            Log.d(TAG, "getMarkerColor: Setting color to Red for Violent Crime");
            return BitmapDescriptorFactory.HUE_RED;
        } else if ("Property Crime".equals(crimeType)) {
            Log.d(TAG, "getMarkerColor: Setting color to Blue for Property Crime");
            return BitmapDescriptorFactory.HUE_BLUE;
        }
        Log.d(TAG, "getMarkerColor: Setting default color (Green)");
        return BitmapDescriptorFactory.HUE_GREEN; // Default color
    }

    private void iniMap() {
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    public void getLocationPermission() {
        Log.d(TAG, "getLocationPermission: getting location permission");
        String[] permissions = {FINE_LOCATION, COURSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true;
                iniMap();
                init();
            } else {
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: map is ready");
        Toast.makeText(this, "Map is ready", Toast.LENGTH_SHORT).show();
        mMap = googleMap;

        if (mLocationPermissionsGranted) {
            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this, FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, COURSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.getUiSettings().setCompassEnabled(true);


            // Set custom info window adapter
            mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                @Nullable
                @Override
                public View getInfoContents(@NonNull Marker marker) {
                    return null;
                }

                @Override
                public View getInfoWindow(Marker marker) {
                    View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_window, null); // Inflate your custom layout
                    TextView title = infoWindow.findViewById(R.id.infoTitle);
                    title.setText(marker.getTitle()); // Set the crime type as title
                    // Add more information if needed
                    return infoWindow;
                }

            });

            for (Crime crime : crimeData) {
                Log.d(TAG, "Crime: " + crime.toString()); // This will print the details of each crime
                LatLng crimeLocation = new LatLng(crime.getLatitude(), crime.getLongitude());
                float markerColor = getMarkerColor(crime.getCrimeType());

                Log.d(TAG, "Adding marker: " + crime.getCrimeType() + " with color: " + markerColor);
                mMap.addMarker(new MarkerOptions()
                        .position(crimeLocation)
                        .title(crime.getCrimeType())
                        .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
            }
            // Set the OnPoiClickListener here
            mMap.setOnPoiClickListener(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mLocationPermissionsGranted = false;

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionsGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    mLocationPermissionsGranted = true;
                    // initialize our map
                    iniMap();
                    init();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.mapNone) {
            setMapTypeWithDelay(GoogleMap.MAP_TYPE_NONE);
        } else if (id == R.id.mapNormal) {
            setMapTypeWithDelay(GoogleMap.MAP_TYPE_NORMAL);
        } else if (id == R.id.mapSatellite) {
            setMapTypeWithDelay(GoogleMap.MAP_TYPE_SATELLITE);
        } else if (id == R.id.mapHybrid) {
            setMapTypeWithDelay(GoogleMap.MAP_TYPE_HYBRID);
        } else if (id == R.id.mapTerrain) {
            setMapTypeWithDelay(GoogleMap.MAP_TYPE_TERRAIN);
        }

        return super.onOptionsItemSelected(item);
    }

    private void setMapTypeWithDelay(final int mapType) {
        new android.os.Handler().postDelayed(
                () -> {
                    if (mMap != null) {
                        try {
                            mMap.setMapType(mapType);
                        } catch (Exception e) {
                            Log.e(TAG, "Error changing map type: " + e.getMessage());
                        }
                    }
                },
                1000 // 1 second delay, you can adjust this
        );
    }
    @Override
    public void onPoiClick(PointOfInterest poi) {
        try {
            Toast.makeText(this, "Clicked: " +
                            poi.name + "\nPlace ID:" + poi.placeId +
                            "\nLatitude:" + poi.latLng.latitude +
                            " Longitude:" + poi.latLng.longitude,
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error handling POI click: " + e.getMessage());
        }
    }
}