package com.example.map;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MapFragment extends AppCompatActivity implements OnMapReadyCallback , GoogleMap.OnPoiClickListener {

    private GoogleMap mMap;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    private Boolean mLocationPermissionsGranted = false;
    private FusedLocationProviderClient mfusedLocationProviderClient;
    private static final String TAG = "MapReport";
    private Toolbar toolbar;
    private LinearLayout legendLayout;  // Add this line
    private List<Crime> crimeData = new ArrayList<>();
    private double currentLat = 0;
    private double currentLng = 0;
    private FirebaseFirestore db;
    private ListenerRegistration reportListener;
    private boolean isShowingNearbyPlaces = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_map);
        FirebaseApp.initializeApp(this);
        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        // Reference to the "report" collection
        CollectionReference reportCollectionRef = db.collection("report");

        // Fetch all documents from the "report" collection
        reportCollectionRef.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // Convert each document to a Report object

                        Report report = document.toObject(Report.class);

                        // Access the fields of the Report object
                        String desc = report.getDesc();
                        GeoPoint location = report.getLocation();
                        Timestamp timestamp = report.getTimestamp();
                        String user = report.getUserId();
                        Log.d(TAG, "desc: " + desc);
                        Log.d(TAG, "location " + location.toString());
                        Log.d(TAG, "timestamp: " + timestamp.toString());
                        Log.d(TAG, "user: " + user);
                        // Use the data as needed
                        // For example, you can update UI or perform other operations
                    }
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "Error: " + e.getMessage());
                });

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

        ImageButton specificButton = findViewById(R.id.btnPolice); // Replace with your button's ID
        specificButton.setOnClickListener(v -> onSpecificButtonClicked());

        ImageButton specificButton2 = findViewById(R.id.btnHospital); // Replace with your button's ID
        specificButton2.setOnClickListener(v -> onSpecificButtonClicked2());

        ImageButton specificButton3 = findViewById(R.id.btnFireDepartment); // Replace with your button's ID
        specificButton3.setOnClickListener(v -> onSpecificButtonClicked3());

        // Start listening for report updates
        startReportListener();

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
                    if (task.isSuccessful() && task.getResult() != null) {
                        Log.d(TAG, "onComplete: found location");
                        Location currentLocation = task.getResult();

                        // Store the current location in global variables
                        currentLat = currentLocation.getLatitude();
                        currentLng = currentLocation.getLongitude();

                        moveCamera(new LatLng(currentLat, currentLng), DEFAULT_ZOOM, "My Location");
                    } else {
                        Log.d(TAG, "onComplete: current location is null");
                        Toast.makeText(MapFragment.this, "Unable to get current location", Toast.LENGTH_SHORT).show();
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
            return BitmapDescriptorFactory.HUE_VIOLET;
        } else if ("Property Crime".equals(crimeType)) {
            Log.d(TAG, "getMarkerColor: Setting color to Blue for Property Crime");
            return BitmapDescriptorFactory.HUE_VIOLET;
        }
        Log.d(TAG, "getMarkerColor: Setting default color (Green)");
        return BitmapDescriptorFactory.HUE_RED; // Default color
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

            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(@NonNull Marker marker) {
                    marker.showInfoWindow();
                    return true;
                }
            });
            // Set custom info window adapter
            mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                @Nullable
                @Override
                public View getInfoContents(@NonNull Marker marker) {
                    return null;
                }

                @Override
                public View getInfoWindow(Marker marker) {
                    LatLng destinationLatLng = marker.getPosition();
                    View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_window, null); // Inflate your custom layout
                    TextView title = infoWindow.findViewById(R.id.infoTitle);
                    TextView lat = infoWindow.findViewById(R.id.Loclat);
                    TextView Long = infoWindow.findViewById(R.id.LocLong);
                    TextView time = infoWindow.findViewById(R.id.timeStamp);

                    // Extract the timestamp from the marker's title
                    String titleText = marker.getTitle();
                    String[] titleParts = titleText.split("\\|"); // Assuming the timestamp is appended to the title using "|"
                    if (titleParts.length > 1) {
                        String timeStamp = titleParts[1].trim();
                        time.setText(timeStamp);
                    } else {
                        time.setVisibility(View.GONE); // Hide the timestamp TextView if not found
                    }
                    title.setText(marker.getTitle()); // Set the crime type as title
                    lat.setText(Double.toString(destinationLatLng.latitude));
                    Long.setText(Double.toString(destinationLatLng.longitude));

                    // Add more information if needed
                    return infoWindow;
                }

            });
            mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(@NonNull Marker marker) {
                    LatLng destinationLatLng = marker.getPosition();

                    // Start Google Maps intent for directions
                    Intent intent = new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("http://maps.google.com/maps?daddr=" + destinationLatLng.latitude + "," + destinationLatLng.longitude)
                    );
                    intent.setPackage("com.google.android.apps.maps");
                    startActivity(intent);
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
    private void parseResult(String data, String placeType) {
        try {
            JSONObject object = new JSONObject(data);

            // Check if the response contains results
            if (object.has("results")) {
                JsonParser jsonParser = new JsonParser();
                List<HashMap<String, String>> mapList = jsonParser.parseResult(object);

                if (mapList != null && !mapList.isEmpty()) {
                    runOnUiThread(() -> {
                        mMap.clear();
                        for (int i = 0; i < mapList.size(); i++) {
                            HashMap<String, String> hashMapList = mapList.get(i);
                            double lat = Double.parseDouble(Objects.requireNonNull(hashMapList.get("lat")));
                            double lng = Double.parseDouble(Objects.requireNonNull(hashMapList.get("lng")));
                            String name = hashMapList.get("name");
                            LatLng latLng = new LatLng(lat, lng);
                            float markerColor = getMarkerColorForPlaceType(placeType);

                            if (i == 0) {
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));
                            }
                            mMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title(name)
                                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor))); // Apply the marker color here
                        }
                    });
                } else {
                    Log.d(TAG, "No places found within the specified radius.");
                }
            } else if (object.has("status") && object.getString("status").equals("ZERO_RESULTS")) {
                Log.d(TAG, "No places found within the specified radius.");
            } else {
                Log.e(TAG, "Error in API response: " + object.toString());
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON response: " + e.getMessage());
            e.printStackTrace(); // Print the stack trace for debugging
        }
    }


    private void getNearbyPlace(String command, String placeType) {
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=" + currentLat + "," + currentLng +
                "&radius=5000" + // specify the radius in meters
                "&keyword=" + command +
                "&sensor=true" +
                "&key=" + getResources().getString(R.string.api_key);
        Log.d(TAG, "Url : " + url);
        Executor executor = Executors.newSingleThreadExecutor();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            CompletableFuture.supplyAsync(() -> {
                        try {
                            return downloadUrl(url);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }, executor).thenAccept(data -> parseResult(data, placeType))
                    .exceptionally(ex -> {
                        Log.e(TAG, "Exception in CompletableFuture: " + ex.getMessage());
                        return null;
                    });
        }
    }
    private String downloadUrl(String urlString) throws IOException {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            try (InputStream stream = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {

                StringBuilder builder = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }

                return builder.toString();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error downloading URL: " + e.getMessage());
            throw e; // Re-throw the exception for the calling method to handle
        }
    }
    // Method to be called when the specific button is clicked
    private void onSpecificButtonClicked() {
        getDeviceLocation();
        if (isShowingNearbyPlaces) {
            showAllNearbyCrimes();
        } else {
            getNearbyPlace("police", "police");
            isShowingNearbyPlaces = true;
        }
    }

    private void onSpecificButtonClicked2() {
        getDeviceLocation();
        if (isShowingNearbyPlaces) {
            showAllNearbyCrimes();
        } else {
            getNearbyPlace("hospital", "hospital");
            isShowingNearbyPlaces = true;
        }
    }

    private void onSpecificButtonClicked3() {
        getDeviceLocation();
        if (isShowingNearbyPlaces) {
            showAllNearbyCrimes();
        } else {
            getNearbyPlace("fire station", "fire_station");
            isShowingNearbyPlaces = true;
        }
    }
    private float getMarkerColorForPlaceType(String placeType) {
        switch (placeType) {
            case "police":
                return BitmapDescriptorFactory.HUE_BLUE;  // Set the color for police markers
            case "hospital":
                return BitmapDescriptorFactory.HUE_GREEN;  // Set the color for hospital markers
            case "fire_station":
                return BitmapDescriptorFactory.HUE_ORANGE;   // Set the color for fire station markers
            default:
                return BitmapDescriptorFactory.HUE_YELLOW; // Default color
        }
    }

    private void showAllNearbyCrimes() {
        // Assuming crime markers are already added to the map during onMapReady
        moveCameraToUserLocation();
        addCrimeMarkers(); // Add crime markers back to the map
        isShowingNearbyPlaces = false;
    }

    private void addCrimeMarkers() {
        for (Crime crime : crimeData) {
            LatLng crimeLocation = new LatLng(crime.getLatitude(), crime.getLongitude());
            float markerColor = getMarkerColor(crime.getCrimeType());

            Log.d(TAG, "Adding marker: " + crime.getCrimeType() + " with color: " + markerColor);
            mMap.addMarker(new MarkerOptions()
                    .position(crimeLocation)
                    .title(crime.getCrimeType())
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
        }
    }
    private void moveCameraToUserLocation() {
        if (currentLat != 0 && currentLng != 0) {
            LatLng userLocation = new LatLng(currentLat, currentLng);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, DEFAULT_ZOOM));
        }
    }

    private void startReportListener() {
        try {
            Log.d(TAG, "startReportListener: Start listening for reports");
            FirebaseFirestore fStore = FirebaseFirestore.getInstance();
            CollectionReference reportsRef = fStore.collection("report");

            reportsRef.get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        Toast.makeText(this, "Obtaining Report location nearby", Toast.LENGTH_LONG).show();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Report report = document.toObject(Report.class);

                            // Log details of the report

                            // Add marker for the report
                            addReportMarker(report);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching reports: ", e);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception in startReportListener: " + e.getMessage());
        }
    }


    private void addReportMarker(Report report) {
        // Access the GeoPoint from the report
        GeoPoint geoPoint = report.getLocation(); // Assuming report now has a GeoPoint field

        // Create a LatLng object from the GeoPoint
        LatLng reportLocation = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
        // Create a purple marker icon
        BitmapDescriptor markerIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET);

        // Add the marker to the map
        mMap.addMarker(new MarkerOptions()
                .position(reportLocation)
                .title(report.getDesc())
                .icon(markerIcon)  // Set the marker icon to the purple color
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop listening for updates when the activity is destroyed
        if (reportListener != null) {
            reportListener.remove();
        }
    }


}