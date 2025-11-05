package com.example.onfire_project;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;


public class RunningActivity extends AppCompatActivity implements SensorEventListener {

    private static final int MIN_LOCATION_UPDATE_MS = 1000;
    private static final float MIN_LOCATION_UPDATE_M = 1f;

    // Constantes para cálculo baseado em passos
    private static final double AVERAGE_STRIDE_LENGTH_METERS = 0.78;
    private static final double CALORIES_PER_STEP = 0.04;

    private MapView map;
    private MyLocationNewOverlay myLocationOverlay;
    private IMapController mapController;

    private LocationManager locationManager;
    private Location latestLocation;

    private LinearLayout cancelBtn;

    private double totalDistanceMeters = 0.0;

    // Timer bookkeeping
    private long startTimeMillis = 0L;
    private long pausedOffsetMillis = 0L;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    // UI
    private TextView statusGPS;
    private TextView timeTxt;
    private TextView paceTxt;
    private TextView kilTxt;
    private LinearLayout toggleActivity;
    private ImageView toggleIcon;
    private LinearLayout statusGpsContainer;
    private LinearLayout stopBtn;

    // Run state
    private boolean isRunning = false;
    private boolean sessionStarted = false;

    // Sensors (steps)
    private SensorManager sensorManager;
    private Sensor stepDetector;
    private Sensor stepCounter;
    private boolean useStepDetector = false;
    private long stepBaseline = -1L;
    private int stepsSinceStart = 0;

    // Variáveis calculadas
    private int stepsStored = 0;
    private double caloriesStored = 0.0;
    private double speedKmhStored = 0.0;

    // GPS status (apenas para UI)
    private boolean gpsObtained = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // osmdroid user agent
        Configuration.getInstance().setUserAgentValue(getApplicationContext().getPackageName());

        setContentView(R.layout.activity_running);

        initializeViews();
        initializeMap();
        initializeLocationManager();
        initializeSensors();
        initializeTimer();
        setupClickListeners();

        // UI inicial
        updateInitialUI();
    }

    private void initializeViews() {
        // MapView + overlay
        map = findViewById(R.id.map);

        // UI elements
        statusGpsContainer = findViewById(R.id.linearLayout);
        statusGPS = findViewById(R.id.statusGPS);
        timeTxt = findViewById(R.id.timeTxt);
        paceTxt = findViewById(R.id.paceTxt);
        kilTxt = findViewById(R.id.kilTxt);
        toggleActivity = findViewById(R.id.toggleActivity);
        toggleIcon = findViewById(R.id.imageView10);
        stopBtn = findViewById(R.id.stopActivity);
        cancelBtn = findViewById(R.id.cancelActivity);

        OnBackPressedCallback callback =
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        showCancelConfirmationDialog();
                    }
                };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void initializeMap() {
        if (map != null) {
            map.setTileSource(TileSourceFactory.MAPNIK);
            map.setMultiTouchControls(true);
            mapController = map.getController();
            if (mapController != null) mapController.setZoom(17.0);

            // MyLocation overlay (osmdroid) - apenas visual
            myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
            myLocationOverlay.enableFollowLocation();
            map.getOverlays().add(myLocationOverlay);

            // Verificar se já tem localização
            myLocationOverlay.runOnFirstFix(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateGpsStatus(true);
                        }
                    });
                }
            });
        }
    }

    private void initializeLocationManager() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    private void initializeSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            useStepDetector = (stepDetector != null);
        }
    }

    private void initializeTimer() {
        // Runnable do timer (atualiza UI a cada segundo)
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = 0L;
                if (sessionStarted) {
                    if (isRunning) {
                        elapsed = pausedOffsetMillis + (SystemClock.elapsedRealtime() - startTimeMillis);
                    } else {
                        elapsed = pausedOffsetMillis;
                    }
                } else {
                    elapsed = 0L;
                }

                // Atualiza tempo
                long totalSeconds = elapsed / 1000;
                long minutes = totalSeconds / 60;
                long seconds = totalSeconds % 60;
                if (timeTxt != null) timeTxt.setText(String.format("%02d:%02d", minutes, seconds));

                // Calcula distância baseada em passos
                totalDistanceMeters = stepsSinceStart * AVERAGE_STRIDE_LENGTH_METERS;
                stepsStored = stepsSinceStart;
                caloriesStored = stepsSinceStart * CALORIES_PER_STEP;

                // Pace (min/km)
                if (sessionStarted && totalDistanceMeters >= 1.0 && elapsed > 0) {
                    double timeSecs = elapsed / 1000.0;
                    double paceSecPerKm = timeSecs / (totalDistanceMeters / 1000.0);
                    int paceMin = (int) (paceSecPerKm / 60.0);
                    int paceSec = (int) (paceSecPerKm % 60.0);
                    if (paceTxt != null) paceTxt.setText(String.format("%d:%02d", paceMin, paceSec));
                } else {
                    if (paceTxt != null) paceTxt.setText("--:--");
                }

                // Distância em km
                double km = totalDistanceMeters / 1000.0;
                if (kilTxt != null) kilTxt.setText(String.format("%.2f km", km));

                // Velocidade média (km/h)
                if (sessionStarted && elapsed > 0) {
                    double speedKmh = (totalDistanceMeters / (elapsed / 1000.0)) * 3.6;
                    speedKmhStored = speedKmh;
                }

                timerHandler.postDelayed(this, 1000);
            }
        };
    }

    private void setupClickListeners() {
        // Toggle (play/pause/resume)
        if (toggleActivity != null) {
            toggleActivity.setOnClickListener(v -> {
                if (!isRunning) {
                    if (!sessionStarted) startNewSession();
                    else resumeRun();
                } else {
                    pauseRun();
                }
            });
        }

        // Stop button
        if (stopBtn != null) {
            stopBtn.setOnClickListener(v -> showStopConfirmationDialog());
        }

        // Cancel button
        if (cancelBtn != null) {
            cancelBtn.setOnClickListener(v -> showCancelConfirmationDialog());
        }
    }

    private void showCancelConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Activity")
                .setMessage("Do you pretend to cancel your activity? This activity will not be stored in your history.")
                .setPositiveButton("CONFIRM", (dialog, which) -> {
                    // Confirmar cancelamento - voltar para Dashboard
                    returnToDashboard();
                })
                .setNegativeButton("CANCEL", (dialog, which) -> {
                    // Não faz nada, simplesmente fecha o diálogo
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    private void showStopConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Finish Activity")
                .setMessage("Do you pretend to finish your activity? This activity will be stored in your history.")
                .setPositiveButton("CONFIRM", (dialog, which) -> {
                    // Confirmar término - guardar dados e voltar para Dashboard
                    saveActivityData();
                    returnToDashboard();
                })
                .setNegativeButton("CANCEL", (dialog, which) -> {
                    // Não faz nada, simplesmente fecha o diálogo
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    private void saveActivityData() {
        // Parar a sessão primeiro para gerar o JSON
        if (sessionStarted) {

            String json = stopSession();

            Log.d("RunningActivity", "Activity data saved: " + json);
        }
    }

    private void updateInitialUI() {
        if (statusGPS != null) statusGPS.setText("Getting GPS Location");
        if (timeTxt != null) timeTxt.setText("00:00");
        if (paceTxt != null) paceTxt.setText("--:--");
        if (kilTxt != null) kilTxt.setText("0.00 km");
    }

    private boolean hasLocationPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void startNewSession() {
        if (!hasActivityRecognitionPermission()) {
            Log.e("RunningActivity", "Missing step counter permission. Finishing activity.");
            finish();
            return;
        }

        pausedOffsetMillis = 0L;
        startTimeMillis = SystemClock.elapsedRealtime();
        totalDistanceMeters = 0.0;
        stepBaseline = -1L;
        stepsSinceStart = 0;
        stepsStored = 0;
        caloriesStored = 0.0;
        speedKmhStored = 0.0;

        sessionStarted = true;
        isRunning = true;

        animateToggleIcon(true);

        startLocationUpdates(); // apenas para visualização
        registerStepSensors();
        if (myLocationOverlay != null) myLocationOverlay.enableMyLocation();
        timerHandler.post(timerRunnable);
    }

    private void resumeRun() {
        if (!hasActivityRecognitionPermission()) {
            Log.e("RunningActivity", "Missing step counter permission. Finishing activity.");
            finish();
            return;
        }

        animateToggleIcon(true);
        isRunning = true;
        startTimeMillis = SystemClock.elapsedRealtime();

        startLocationUpdates();
        registerStepSensors();
        if (myLocationOverlay != null) myLocationOverlay.enableMyLocation();
        timerHandler.post(timerRunnable);
    }

    private void pauseRun() {
        animateToggleIcon(false);
        isRunning = false;
        long now = SystemClock.elapsedRealtime();
        pausedOffsetMillis += (now - startTimeMillis);

        stopLocationUpdates();
        unregisterStepSensors();
        if (myLocationOverlay != null) myLocationOverlay.disableMyLocation();
    }

    private String stopSession() {
        if (!sessionStarted) return "";

        if (isRunning) {
            isRunning = false;
            long now = SystemClock.elapsedRealtime();
            pausedOffsetMillis += (now - startTimeMillis);
            stopLocationUpdates();
            unregisterStepSensors();
            if (myLocationOverlay != null) myLocationOverlay.disableMyLocation();
        }

        long totalElapsedMs = pausedOffsetMillis;
        long totalSeconds = totalElapsedMs / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        String duration = String.format("%02d:%02d:%02d", hours, minutes, seconds);

        double distanceKm = totalDistanceMeters / 1000.0;
        int calories = (int) Math.round(caloriesStored);
        double avgSpeed = speedKmhStored;

        String pace = "--:--";
        if (totalDistanceMeters >= 1.0 && totalElapsedMs > 0) {
            double timeSecs = totalElapsedMs / 1000.0;
            double paceSecPerKm = timeSecs / distanceKm;
            int paceMin = (int) (paceSecPerKm / 60.0);
            int paceSec = (int) (paceSecPerKm % 60.0);
            pace = String.format("%d:%02d", paceMin, paceSec);
        }

        String json = "{\n" +
                "  \"type\": \"running\",\n" +
                "  \"pace\": \"" + pace + "\",\n" +
                "  \"distance\": " + String.format("%.2f", distanceKm) + ",\n" +
                "  \"calories\": " + calories + ",\n" +
                "  \"avg_speed\": " + String.format("%.2f", avgSpeed) + ",\n" +
                "  \"duration\": \"" + duration + "\",\n" +
                "  \"steps\": " + stepsStored + "\n" +
                "}";

        Log.d("RunningActivity", "Session completed:\n" + json);

        // Reset
        sessionStarted = false;
        isRunning = false;
        pausedOffsetMillis = 0L;
        startTimeMillis = 0L;
        totalDistanceMeters = 0.0;
        stepBaseline = -1L;
        stepsSinceStart = 0;
        stepsStored = 0;
        caloriesStored = 0.0;
        speedKmhStored = 0.0;

        if (timeTxt != null) timeTxt.setText("00:00");
        if (paceTxt != null) paceTxt.setText("--:--");
        if (kilTxt != null) kilTxt.setText("0.00 km");

        try {
            if (toggleIcon != null) toggleIcon.setImageResource(R.drawable.ic_play);
        } catch (Exception ignored) {}

        timerHandler.removeCallbacks(timerRunnable);

        Log.i("RunningActivity", "Session stopped and reset successfully.");

        return json;
    }

    private void saveToDatabase(String jsonData) {
        Log.i("Database", "Saving to database: " + jsonData);
    }

    private void returnToDashboard() {
        Intent intent = new Intent(RunningActivity.this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
    private void startLocationUpdates() {
        if (!hasLocationPermissions()) return;

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                // Tentar obter última localização conhecida
                if (locationManager != null) {
                    Location lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (lastKnown == null) {
                        lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }

                    if (lastKnown != null) {
                        latestLocation = lastKnown;
                        updateGpsStatus(true);
                        GeoPoint gp = new GeoPoint(lastKnown.getLatitude(), lastKnown.getLongitude());
                        if (mapController != null) mapController.setCenter(gp);
                    }

                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            MIN_LOCATION_UPDATE_MS,
                            MIN_LOCATION_UPDATE_M,
                            locationListener,
                            Looper.getMainLooper()
                    );
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_LOCATION_UPDATE_MS,
                            MIN_LOCATION_UPDATE_M,
                            locationListener,
                            Looper.getMainLooper()
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateGpsStatus(boolean obtained) {
        gpsObtained = obtained;
        if (statusGPS != null) {
            statusGPS.setText(obtained ? "GPS location obtained" : "Getting GPS Location");
        }
        if (obtained) {
            try {
                if (statusGpsContainer != null)
                    ViewCompat.setBackgroundTintList(statusGpsContainer,
                            ContextCompat.getColorStateList(this, R.color.success));
            } catch (Exception ignored) {}
        }
    }

    private void stopLocationUpdates() {
        try {
            if (locationManager != null) locationManager.removeUpdates(locationListener);
        } catch (SecurityException ignored) {
        }
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            if (location == null) return;

            latestLocation = new Location(location);

            float acc = location.hasAccuracy() ? location.getAccuracy() : Float.MAX_VALUE;
            if (acc > 0 && acc <= 30) {
                updateGpsStatus(true);
            }

            GeoPoint gp = new GeoPoint(location.getLatitude(), location.getLongitude());
            if (mapController != null) mapController.setCenter(gp);
        }

        @Override public void onProviderEnabled(@NonNull String provider) { }
        @Override public void onProviderDisabled(@NonNull String provider) { }
        @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
    };

    private void registerStepSensors() {
        if (!hasActivityRecognitionPermission()) return;
        if (sensorManager == null) return;

        if (useStepDetector) {
            sensorManager.registerListener(this, stepDetector, SensorManager.SENSOR_DELAY_NORMAL);
        } else if (stepCounter != null) {
            sensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void unregisterStepSensors() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isRunning) return;

        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            if (event.values != null && event.values.length > 0) {
                stepsSinceStart += (int) event.values[0];
            }
        } else if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            if (stepBaseline < 0) {
                stepBaseline = (long) event.values[0];
            }
            long totalSinceBoot = (long) event.values[0];
            stepsSinceStart = (int) (totalSinceBoot - stepBaseline);
            if (stepsSinceStart < 0) stepsSinceStart = 0;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) map.onResume();

        if (hasLocationPermissions()) {
            if (myLocationOverlay != null) myLocationOverlay.enableMyLocation();
            startLocationUpdates();
        }

        timerHandler.post(timerRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) map.onPause();

        if (isRunning) {
            pauseRun();
        }

        stopLocationUpdates();
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        unregisterStepSensors();
        timerHandler.removeCallbacksAndMessages(null);

        // limpar e evitar leaks
        if (map != null) {
            map.getOverlays().clear();
            map.onDetach();
            map = null;
        }
        myLocationOverlay = null;
        mapController = null;
    }

    private void animateToggleIcon(final boolean toPause) {
        if (toggleIcon == null) return;
        toggleIcon.animate().rotationBy(360f).setDuration(300).withEndAction(new Runnable() {
            @Override
            public void run() {
                try {
                    if (toPause) {
                        toggleIcon.setImageResource(R.drawable.ic_pause);
                    } else {
                        toggleIcon.setImageResource(R.drawable.ic_play);
                    }
                    toggleIcon.setRotation(0f);
                } catch (Exception e) {
                    // se drawables não existirem, ignora
                }
            }
        }).start();
    }
}