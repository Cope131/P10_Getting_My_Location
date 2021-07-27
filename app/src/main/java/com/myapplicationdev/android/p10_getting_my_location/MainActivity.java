package com.myapplicationdev.android.p10_getting_my_location;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener, OnSuccessListener, OnMapReadyCallback, CompoundButton.OnCheckedChangeListener {

    private final String DEBUG_TAG = MainActivity.class.getSimpleName();
    private final int REQUEST_CODE_ONE_TIME = 1;
    private final int REQUEST_CODE_CONTINUOUS = 2;
    private final int REQUEST_CODE_STORAGE = 3;

    // File Directory to Save Records
    private String folderLocPath;
    private File recordFile;

    // Music
    private Intent serviceIntent;

    // Maps
    private GoogleMap googleMap;

    // Views
    private TextView latTV, lngTV;
    private ToggleButton toggleBtn;

    // Last Location
    private Location lastLocation;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isUpdateEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        serviceIntent = new Intent(this, MyService.class);;
        folderLocPath = getFilesDir().getAbsolutePath() + "/LocationRecords";
        initMap();
        initViews();
        initLocationComp();
        // Get Last Location
        getOneTimeLocation();
    }

    private void initViews() {
        Button startDetectorBtn, stopDetectorBtn, checkRecordsBtn;
        startDetectorBtn = findViewById(R.id.start_detector_button);
        stopDetectorBtn = findViewById(R.id.stop_detector_button);
        checkRecordsBtn = findViewById(R.id.check_records_button);
        startDetectorBtn.setOnClickListener(this);
        stopDetectorBtn.setOnClickListener(this);
        checkRecordsBtn.setOnClickListener(this);
        latTV = findViewById(R.id.latitude_text_view);
        lngTV = findViewById(R.id.longitude_text_view);

        toggleBtn = findViewById(R.id.toggle_button);
        toggleBtn.setOnCheckedChangeListener(this);
    }

    private void initLocationComp() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_detector_button:
                getLocationContinuously();
                break;
            case R.id.stop_detector_button:
                removeLocationUpdates();
                break;
            case R.id.check_records_button:
                checkRecords();
        }
    }

    // --- Permissions ---
    private void askPermission(LocationType locationType) {
        if (!checkPermission()) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    locationType == LocationType.ONE_TIME ? REQUEST_CODE_ONE_TIME : REQUEST_CODE_CONTINUOUS);
            return;
        }
        switch (locationType) {
            case ONE_TIME:
                getOneTimeLocation();
                break;
            case CONTINUOUS:
                getLocationContinuously();
                break;
        }
    }

    private boolean checkPermission() {
        int permissionCheck_Coarse
                = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        int permissionCheck_Fine
                = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionCheck_Coarse == PermissionChecker.PERMISSION_GRANTED ||
                permissionCheck_Fine == PermissionChecker.PERMISSION_GRANTED;
    }

    private void askPermissionStorage() {
        if (!checkPermissionStorage()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE);
            return;
        }
        startService();
    }

    private boolean checkPermissionStorage() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Permissions Granted
        switch (requestCode) {
            case REQUEST_CODE_ONE_TIME:
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission is not granted", Toast.LENGTH_SHORT).show();
                    return;
                }
                fusedLocationClient.getLastLocation().addOnSuccessListener(this, this::onSuccess);
                break;
            case REQUEST_CODE_CONTINUOUS:
                getLocationCont();
                break;
            case REQUEST_CODE_STORAGE:
                startService();
        }
    }

    // --- Get Location Continuously
    private void getLocationCont() {
        if (checkPermission()) {
            getLocationContinuously();
            return;
        }
        askPermission(LocationType.CONTINUOUS);
    }

    private void getLocationContinuously() {
        // Location Request Settings
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)
                .setFastestInterval(5000)
                .setSmallestDisplacement(100);
        // Listener for Location Updates
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    lastLocation = locationResult.getLastLocation();
                    isUpdateEnabled = true;
                    latTV.setText(lastLocation.getLatitude() + "");
                    lngTV.setText(lastLocation.getLongitude() + "");
                    saveToDir();
                }
            }
        };
        // Start Requesting for Location Updates
        if (checkPermission()) {
            Log.d(DEBUG_TAG, "Check Permission Result: " + checkPermission());
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    private void removeLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        Toast.makeText(this, "Location Update Disabled", Toast.LENGTH_SHORT).show();
        isUpdateEnabled = false;
    }

    // --- Get Location Once ---
    private void getOneTimeLocation() {
        if (checkPermission()) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, this::onSuccess);
            return;
        }
        askPermission(LocationType.ONE_TIME);
    }

    @Override
    public void onSuccess(Object o) {
        Log.d(DEBUG_TAG, "onSuccess Location");
        Location location = (Location) o;
        if (location != null) {
            Log.d(DEBUG_TAG, "location is not null");
            latTV.setText(location.getLatitude() + "");
            lngTV.setText(location.getLongitude() + "");
        }
    }

    // Location Type
    enum LocationType {
        ONE_TIME, CONTINUOUS
    }

    // --- Maps ---
    private void initMap() {
        SupportMapFragment mapFragment
                = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.google_map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        // Enable Features
        UiSettings uiSettings = googleMap.getUiSettings();
        uiSettings.setMyLocationButtonEnabled(true);
        uiSettings.setCompassEnabled(true);
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setZoomGesturesEnabled(true);
        // Enable My Location
        if (!checkPermission()) {
            askPermission(LocationType.CONTINUOUS);
            return;
        }
        googleMap.setMyLocationEnabled(true);
    }

    // --- Saving Location ---
    private void saveToDir() {
        createDir();
        writeRecord();
    }

    private void createDir() {
        Log.d(DEBUG_TAG, folderLocPath);
        File folder = new File(folderLocPath);
        if (!folder.exists()) {
            boolean isCreated = folder.mkdir();
            Log.d(DEBUG_TAG, isCreated ? "Folder Created" : "Folder Not Created");
        }
    }
    private void writeRecord() {
        recordFile = new File(folderLocPath, "records.txt");
        Log.d(DEBUG_TAG, folderLocPath + recordFile.getAbsolutePath());
        try {
            FileWriter writer = new FileWriter(recordFile, true);
            String location =lastLocation.getLatitude() + ", " + lastLocation.getLongitude();
            writer.write(location + "\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Toast.makeText(this, "Failed to write!", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void checkRecords() {
        Intent intent = new Intent(this, MainActivity2.class);
        intent.putExtra("folderPath", folderLocPath);
        intent.putExtra("fileName", "records.txt");
        startActivity(intent);
    }

    // --- Music Player ---
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.d(DEBUG_TAG, "isChecked: " + isChecked);
        if (isChecked) {
            askPermissionStorage();
            return;
        }
        stopService();
    }

    private void stopService() {
        stopService(serviceIntent);
    }

    private void startService() {
        startService(serviceIntent);
    }

}