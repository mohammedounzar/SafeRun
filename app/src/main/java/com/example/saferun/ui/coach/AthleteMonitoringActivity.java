package com.example.saferun.ui.coach;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.saferun.R;
import com.example.saferun.data.model.RunSession;
import com.example.saferun.data.model.SensorData;
import com.example.saferun.data.repository.RunSessionRepository;
import com.example.saferun.data.repository.SensorDataRepository;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class AthleteMonitoringActivity extends AppCompatActivity {

    private static final String TAG = "AthleteMonitorActivity";
    private static final String EXTRA_SESSION_ID = "session_id";
    private static final String EXTRA_ATHLETE_ID = "athlete_id";
    private static final String EXTRA_ATHLETE_NAME = "athlete_name";

    private Toolbar toolbar;
    private TextView athleteNameTextView;
    private TextView heartRateTextView;
    private TextView temperatureTextView;
    private TextView speedTextView;
    private TextView alertsCountTextView;
    private MaterialCardView alertCardView;
    private TextView alertMessageTextView;
    private TextView alertSourceTextView;
    private Button dismissAlertButton;
    private Button contactAthleteButton;
    private Button endSessionButton;
    private ImageButton backButton;
    private ProgressBar progressBar;

    // Session summary section
    private MaterialCardView summaryCardView;
    private LineChart heartRateSpeedChart;
    private LineChart temperatureChart;
    private Button returnToSessionsButton;

    private TextView heartRateTitle;
    private TextView temperatureTitle;

    private SimpleDateFormat timeFormat;

    // Data for charts
    private List<Entry> heartRateEntries;
    private List<Entry> speedEntries;
    private List<Entry> temperatureEntries;

    // Line datasets for charts
    private LineDataSet heartRateDataSet;
    private LineDataSet speedDataSet;
    private LineDataSet temperatureDataSet;

    private SensorDataRepository sensorDataRepository;
    private RunSessionRepository runSessionRepository;

    private String sessionId;
    private String athleteId;
    private String athleteName;

    private Timer dataRefreshTimer;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int alertsCount = 0;
    private SensorData lastSensorData;

    // Flag to track if athlete has been contacted for current anomaly
    private boolean athleteContacted = false;

    // Flag to track if the session is active or completed
    private boolean sessionEnded = false;

    // Add some mock data for testing if needed
    private boolean useMockData = false;
    private long lastTimestamp = System.currentTimeMillis();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_athlete_monitoring);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize chart data lists
        heartRateEntries = new ArrayList<>();
        speedEntries = new ArrayList<>();
        temperatureEntries = new ArrayList<>();

        // Get data from intent
        sessionId = getIntent().getStringExtra(EXTRA_SESSION_ID);
        athleteId = getIntent().getStringExtra(EXTRA_ATHLETE_ID);
        athleteName = getIntent().getStringExtra(EXTRA_ATHLETE_NAME);

        if (sessionId == null || athleteId == null) {
            Toast.makeText(this, "Error: Missing required data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize repositories
        sensorDataRepository = SensorDataRepository.getInstance();
        runSessionRepository = RunSessionRepository.getInstance();

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Hide charts initially (will be shown only when session ends)
        hideChartViews();

        // Check if session is already completed
        checkSessionStatus();

        // Add test data if mock data is enabled
        if (useMockData) {
            addMockData();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check session status when returning to the activity
        checkSessionStatus();

        // If session is not ended, restart data refresh
        if (!sessionEnded) {
            startDataRefresh();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop data refresh when activity is not visible
        if (dataRefreshTimer != null) {
            dataRefreshTimer.cancel();
            dataRefreshTimer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Ensure timer is canceled when activity is destroyed
        if (dataRefreshTimer != null) {
            dataRefreshTimer.cancel();
            dataRefreshTimer = null;
        }
    }

    public static void start(AppCompatActivity activity, String sessionId, String athleteId, String athleteName) {
        Intent intent = new Intent(activity, AthleteMonitoringActivity.class);
        intent.putExtra(EXTRA_SESSION_ID, sessionId);
        intent.putExtra(EXTRA_ATHLETE_ID, athleteId);
        intent.putExtra(EXTRA_ATHLETE_NAME, athleteName);
        activity.startActivity(intent);
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        athleteNameTextView = findViewById(R.id.athlete_name);
        heartRateTextView = findViewById(R.id.heart_rate_value);
        temperatureTextView = findViewById(R.id.temperature_value);
        speedTextView = findViewById(R.id.speed_value);
        alertsCountTextView = findViewById(R.id.alerts_count);
        alertCardView = findViewById(R.id.alert_card);
        alertMessageTextView = findViewById(R.id.alert_message);

        // Find charts
        heartRateSpeedChart = findViewById(R.id.heart_rate_speed_chart);
        temperatureChart = findViewById(R.id.temperature_chart);
        heartRateTitle = findViewById(R.id.heart_rate_speed_title);
        temperatureTitle = findViewById(R.id.temperature_title);

        // Find or create summary card and end session button
        summaryCardView = findViewById(R.id.summary_card);
        endSessionButton = findViewById(R.id.end_session_button);
        returnToSessionsButton = findViewById(R.id.return_to_sessions_button);

        // Initialize time format
        timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        // Try to find contact button if it exists
        try {
            contactAthleteButton = findViewById(R.id.contact_athlete_button);
            if (contactAthleteButton != null) {
                contactAthleteButton.setOnClickListener(v -> contactAthlete());
            }
        } catch (Exception e) {
            Log.w(TAG, "Contact button not found in layout: " + e.getMessage());
        }

        // Try to find alert source text view if it exists
        try {
            alertSourceTextView = findViewById(R.id.alert_source);
        } catch (Exception e) {
            Log.w(TAG, "Alert source text view not found: " + e.getMessage());
        }

        dismissAlertButton = findViewById(R.id.dismiss_alert_button);
        backButton = findViewById(R.id.back_button);
        progressBar = findViewById(R.id.progress_bar);

        // Set initial values
        athleteNameTextView.setText(athleteName);
        alertCardView.setVisibility(View.GONE);
        setAlertsCount(0);

        // Set click listeners
        dismissAlertButton.setOnClickListener(v -> dismissAlert());
        backButton.setOnClickListener(v -> onBackPressed());

        // Add listener for end session button
        if (endSessionButton != null) {
            endSessionButton.setOnClickListener(v -> confirmEndSession());
        }

        // Add listener for return to sessions button
        if (returnToSessionsButton != null) {
            returnToSessionsButton.setOnClickListener(v -> finish());
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    private void checkSessionStatus() {
        showProgress(true);

        runSessionRepository.getRunSession(sessionId, new RunSessionRepository.RunSessionCallback() {
            @Override
            public void onSuccess(RunSession session) {
                showProgress(false);

                // Check if the session is completed
                if ("completed".equals(session.getStatus())) {
                    // Session already completed, show summary view
                    sessionEnded = true;

                    // Load historical data for charts
                    loadSessionHistoricalData();

                    // Update UI for completed session
                    if (endSessionButton != null) endSessionButton.setVisibility(View.GONE);
                    if (returnToSessionsButton != null) returnToSessionsButton.setVisibility(View.VISIBLE);
                } else {
                    // Session still active, ensure monitoring view is shown
                    sessionEnded = false;
                    hideChartViews();
                    if (endSessionButton != null) endSessionButton.setVisibility(View.VISIBLE);
                    if (returnToSessionsButton != null) returnToSessionsButton.setVisibility(View.GONE);

                    // Start data refresh for active session
                    startDataRefresh();
                }
            }

            @Override
            public void onError(String errorMessage) {
                showProgress(false);
                Toast.makeText(AthleteMonitoringActivity.this,
                        "Error loading session: " + errorMessage, Toast.LENGTH_SHORT).show();

                // Default to active session view
                sessionEnded = false;
                hideChartViews();
                startDataRefresh();
            }
        });
    }

    private void hideChartViews() {
        // Hide chart components
        if (heartRateSpeedChart != null) heartRateSpeedChart.setVisibility(View.GONE);
        if (temperatureChart != null) temperatureChart.setVisibility(View.GONE);
        if (heartRateTitle != null) heartRateTitle.setVisibility(View.GONE);
        if (temperatureTitle != null) temperatureTitle.setVisibility(View.GONE);

        // Hide summary card if it exists
        if (summaryCardView != null) summaryCardView.setVisibility(View.GONE);

        // Hide return button (only shown when session is ended)
        if (returnToSessionsButton != null) returnToSessionsButton.setVisibility(View.GONE);

        // Make sure end session button is visible
        if (endSessionButton != null) endSessionButton.setVisibility(View.VISIBLE);
    }

    private void loadSessionHistoricalData() {
        showProgress(true);

        // Fetch historical sensor data for this athlete in this session
        sensorDataRepository.getSensorDataHistory(sessionId, athleteId, 1000,
                new SensorDataRepository.SensorDataListCallback() {
                    @Override
                    public void onSuccess(List<SensorData> sensorDataList) {
                        showProgress(false);

                        // Clear previous data
                        heartRateEntries.clear();
                        speedEntries.clear();
                        temperatureEntries.clear();

                        if (sensorDataList != null && !sensorDataList.isEmpty()) {
                            Log.d(TAG, "Retrieved " + sensorDataList.size() + " sensor data points for analysis");

                            // Sort data by timestamp to ensure chronological order
                            Collections.sort(sensorDataList, (data1, data2) ->
                                    Long.compare(data1.getTimestamp(), data2.getTimestamp()));

                            // Process historical data
                            for (SensorData data : sensorDataList) {
                                // Add data points to charts
                                addDataPoint(data.getTimestamp(),
                                        data.getHeartRate(),
                                        (float) data.getSpeed(),
                                        (float) data.getTemperature(),
                                        false);  // Don't update charts yet
                            }

                            // Show session summary with all loaded data
                            showSessionSummary();

                            Log.d(TAG, "Successfully processed " + sensorDataList.size() + " historical data points");
                        } else {
                            Log.w(TAG, "No sensor data found for session " + sessionId + " and athlete " + athleteId);
                            Toast.makeText(AthleteMonitoringActivity.this,
                                    "No sensor data available for this session",
                                    Toast.LENGTH_SHORT).show();

                            // Show empty charts
                            showSessionSummary();
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        showProgress(false);
                        Log.e(TAG, "Error loading sensor data history: " + errorMessage);
                        Toast.makeText(AthleteMonitoringActivity.this,
                                "Error loading historical data: " + errorMessage,
                                Toast.LENGTH_SHORT).show();

                        // Show summary anyway, even if we couldn't load historical data
                        showSessionSummary();
                    }
                });
    }

    private void setupCharts() {
        // Setup heart rate & speed chart
        setupHeartRateSpeedChart();

        // Setup temperature chart
        setupTemperatureChart();

        // Create initial datasets
        createInitialDatasets();

        Log.d(TAG, "Charts initialized successfully");
    }

    private void createInitialDatasets() {
        // Create datasets with initial empty data
        heartRateDataSet = new LineDataSet(new ArrayList<>(), "Heart Rate (bpm)");
        heartRateDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        heartRateDataSet.setColor(Color.RED);
        heartRateDataSet.setCircleColor(Color.RED);
        heartRateDataSet.setLineWidth(2f);
        heartRateDataSet.setCircleRadius(3f);
        heartRateDataSet.setDrawCircles(true);
        heartRateDataSet.setFillAlpha(65);
        heartRateDataSet.setFillColor(Color.RED);
        heartRateDataSet.setHighLightColor(Color.rgb(244, 117, 117));
        heartRateDataSet.setDrawCircleHole(false);
        heartRateDataSet.setDrawValues(false);
        heartRateDataSet.setMode(LineDataSet.Mode.LINEAR);

        speedDataSet = new LineDataSet(new ArrayList<>(), "Speed (km/h)");
        speedDataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        speedDataSet.setColor(Color.BLUE);
        speedDataSet.setCircleColor(Color.BLUE);
        speedDataSet.setLineWidth(2f);
        speedDataSet.setCircleRadius(3f);
        speedDataSet.setDrawCircles(true);
        speedDataSet.setFillAlpha(65);
        speedDataSet.setFillColor(Color.BLUE);
        speedDataSet.setHighLightColor(Color.rgb(117, 117, 244));
        speedDataSet.setDrawCircleHole(false);
        speedDataSet.setDrawValues(false);
        speedDataSet.setMode(LineDataSet.Mode.LINEAR);

        temperatureDataSet = new LineDataSet(new ArrayList<>(), "Temperature (°C)");
        temperatureDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        temperatureDataSet.setColor(Color.GREEN);
        temperatureDataSet.setCircleColor(Color.GREEN);
        temperatureDataSet.setLineWidth(2f);
        temperatureDataSet.setCircleRadius(3f);
        temperatureDataSet.setDrawCircles(true);
        temperatureDataSet.setFillAlpha(65);
        temperatureDataSet.setFillColor(Color.GREEN);
        temperatureDataSet.setHighLightColor(Color.rgb(117, 244, 117));
        temperatureDataSet.setDrawCircleHole(false);
        temperatureDataSet.setDrawValues(false);
        temperatureDataSet.setMode(LineDataSet.Mode.LINEAR);

        // Add datasets to charts
        LineData heartRateSpeedData = new LineData();
        heartRateSpeedData.addDataSet(heartRateDataSet);
        heartRateSpeedData.addDataSet(speedDataSet);
        heartRateSpeedChart.setData(heartRateSpeedData);

        LineData temperatureData = new LineData();
        temperatureData.addDataSet(temperatureDataSet);
        temperatureChart.setData(temperatureData);

        // Refresh charts
        heartRateSpeedChart.invalidate();
        temperatureChart.invalidate();

        Log.d(TAG, "Initial datasets created");
    }

    private void setupHeartRateSpeedChart() {
        // Setup chart
        heartRateSpeedChart.getDescription().setEnabled(false);
        heartRateSpeedChart.setTouchEnabled(true);
        heartRateSpeedChart.setDragEnabled(true);
        heartRateSpeedChart.setScaleEnabled(true);
        heartRateSpeedChart.setPinchZoom(true);
        heartRateSpeedChart.setDrawGridBackground(false);
        heartRateSpeedChart.setBackgroundColor(Color.WHITE);

        // Set no data text
        heartRateSpeedChart.setNoDataText("No sensor data available for this session");
        heartRateSpeedChart.setNoDataTextColor(Color.BLACK);

        // Setup X axis (time)
        XAxis xAxis = heartRateSpeedChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                // Convert timestamp to readable time
                return timeFormat.format(new Date((long) value));
            }
        });

        // Enable auto scaling
        xAxis.setLabelCount(4, true);

        // Setup left Y axis (heart rate)
        YAxis leftAxis = heartRateSpeedChart.getAxisLeft();
        leftAxis.setTextColor(Color.RED);
        leftAxis.setAxisMinimum(40f);
        leftAxis.setAxisMaximum(200f);
        leftAxis.setDrawGridLines(true);

        // Setup right Y axis (speed)
        YAxis rightAxis = heartRateSpeedChart.getAxisRight();
        rightAxis.setTextColor(Color.BLUE);
        rightAxis.setAxisMinimum(0f);
        rightAxis.setAxisMaximum(25f);
        rightAxis.setDrawGridLines(false);

        // Setup legend
        Legend legend = heartRateSpeedChart.getLegend();
        legend.setForm(Legend.LegendForm.LINE);
        legend.setTextSize(12f);
        legend.setTextColor(Color.BLACK);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);

        Log.d(TAG, "Heart rate and speed chart setup complete");
    }

    private void setupTemperatureChart() {
        // Setup chart
        temperatureChart.getDescription().setEnabled(false);
        temperatureChart.setTouchEnabled(true);
        temperatureChart.setDragEnabled(true);
        temperatureChart.setScaleEnabled(true);
        temperatureChart.setPinchZoom(true);
        temperatureChart.setDrawGridBackground(false);
        temperatureChart.setBackgroundColor(Color.WHITE);

        // Set no data text
        temperatureChart.setNoDataText("No temperature data available for this session");
        temperatureChart.setNoDataTextColor(Color.BLACK);

        // Setup X axis (time)
        XAxis xAxis = temperatureChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                // Convert timestamp to readable time
                return timeFormat.format(new Date((long) value));
            }
        });

        // Enable auto scaling
        xAxis.setLabelCount(4, true);

        // Setup Y axis (temperature)
        YAxis leftAxis = temperatureChart.getAxisLeft();
        leftAxis.setTextColor(Color.GREEN);
        leftAxis.setAxisMinimum(34f);
        leftAxis.setAxisMaximum(41f);
        leftAxis.setDrawGridLines(true);

        // Disable right Y axis
        YAxis rightAxis = temperatureChart.getAxisRight();
        rightAxis.setEnabled(false);

        // Setup legend
        Legend legend = temperatureChart.getLegend();
        legend.setForm(Legend.LegendForm.LINE);
        legend.setTextSize(12f);
        legend.setTextColor(Color.BLACK);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);

        Log.d(TAG, "Temperature chart setup complete");
    }

    private void addMockData() {
        // Add some mock data points for testing
        for (int i = 0; i < 5; i++) {
            long time = System.currentTimeMillis() - (5-i) * 1000; // 1 second intervals
            float hr = 70 + i * 2; // Heart rate increasing
            float speed = 5.0f + i * 0.2f; // Speed increasing
            float temp = 36.5f + i * 0.1f; // Temperature increasing

            addDataPoint(time, hr, speed, temp, false);
        }

        Log.d(TAG, "Added mock data points for testing");
    }

    private void addDataPoint(long timestamp, float heartRate, float speed, float temperature, boolean updateUI) {
        // Add to entries lists
        heartRateEntries.add(new Entry(timestamp, heartRate));
        speedEntries.add(new Entry(timestamp, speed));
        temperatureEntries.add(new Entry(timestamp, temperature));

        // Limit data points to keep performance optimal (do this BEFORE updating charts)
        while (heartRateEntries.size() > 100) {
            heartRateEntries.remove(0);
        }
        while (speedEntries.size() > 100) {
            speedEntries.remove(0);
        }
        while (temperatureEntries.size() > 100) {
            temperatureEntries.remove(0);
        }

        // Only update charts if session has ended and we're viewing summary
        if (sessionEnded && updateUI) {
            updateChartsWithCurrentData();
        }

        Log.d(TAG, "Added new data point: time=" + new Date(timestamp) +
                ", HR=" + heartRate + ", speed=" + speed + ", temp=" + temperature);
    }

    private void updateChartsWithCurrentData() {
        if (heartRateDataSet == null || speedDataSet == null || temperatureDataSet == null) {
            // Initialize charts and datasets if needed
            setupCharts();
        }

        // Clear and re-add all entries to datasets
        heartRateDataSet.clear();
        speedDataSet.clear();
        temperatureDataSet.clear();

        for (Entry entry : heartRateEntries) {
            heartRateDataSet.addEntry(entry);
        }

        for (Entry entry : speedEntries) {
            speedDataSet.addEntry(entry);
        }

        for (Entry entry : temperatureEntries) {
            temperatureDataSet.addEntry(entry);
        }

        // Notify datasets of changes
        heartRateDataSet.notifyDataSetChanged();
        speedDataSet.notifyDataSetChanged();
        temperatureDataSet.notifyDataSetChanged();

        // Notify chart data of changes
        heartRateSpeedChart.getData().notifyDataChanged();
        temperatureChart.getData().notifyDataChanged();

        // Update chart axis ranges and visibility
        if (!heartRateEntries.isEmpty()) {
            float minX = heartRateEntries.get(0).getX();
            float maxX = heartRateEntries.get(heartRateEntries.size() - 1).getX();

            // Set visible X range to show all data
            heartRateSpeedChart.getXAxis().setAxisMinimum(minX);
            heartRateSpeedChart.getXAxis().setAxisMaximum(maxX + (maxX - minX) * 0.05f); // Add 5% padding
            temperatureChart.getXAxis().setAxisMinimum(minX);
            temperatureChart.getXAxis().setAxisMaximum(maxX + (maxX - minX) * 0.05f); // Add 5% padding

            // Auto-scale Y axis if needed
            float minHR = Float.MAX_VALUE, maxHR = Float.MIN_VALUE;
            float minSpeed = Float.MAX_VALUE, maxSpeed = Float.MIN_VALUE;
            float minTemp = Float.MAX_VALUE, maxTemp = Float.MIN_VALUE;

            for (Entry entry : heartRateEntries) {
                minHR = Math.min(minHR, entry.getY());
                maxHR = Math.max(maxHR, entry.getY());
            }

            for (Entry entry : speedEntries) {
                minSpeed = Math.min(minSpeed, entry.getY());
                maxSpeed = Math.max(maxSpeed, entry.getY());
            }

            for (Entry entry : temperatureEntries) {
                minTemp = Math.min(minTemp, entry.getY());
                maxTemp = Math.max(maxTemp, entry.getY());
            }

            // Set heart rate Y axis limits with padding
            if (minHR < Float.MAX_VALUE && maxHR > Float.MIN_VALUE) {
                float hrPadding = (maxHR - minHR) * 0.1f;
                heartRateSpeedChart.getAxisLeft().setAxisMinimum(Math.max(0, minHR - hrPadding));
                heartRateSpeedChart.getAxisLeft().setAxisMaximum(maxHR + hrPadding);
            }

            // Set speed Y axis limits with padding
            if (minSpeed < Float.MAX_VALUE && maxSpeed > Float.MIN_VALUE) {
                float speedPadding = (maxSpeed - minSpeed) * 0.1f;
                heartRateSpeedChart.getAxisRight().setAxisMinimum(Math.max(0, minSpeed - speedPadding));
                heartRateSpeedChart.getAxisRight().setAxisMaximum(maxSpeed + speedPadding);
            }

            // Set temperature Y axis limits with padding
            if (minTemp < Float.MAX_VALUE && maxTemp > Float.MIN_VALUE) {
                float tempPadding = (maxTemp - minTemp) * 0.1f;
                temperatureChart.getAxisLeft().setAxisMinimum(Math.max(34, minTemp - tempPadding));
                temperatureChart.getAxisLeft().setAxisMaximum(Math.min(42, maxTemp + tempPadding));
            }

            // Notify charts to update
            heartRateSpeedChart.invalidate();
            temperatureChart.invalidate();

            Log.d(TAG, "Charts updated with " + heartRateEntries.size() + " data points");
        }
    }

    private void startDataRefresh() {
        // Cancel any existing timer
        if (dataRefreshTimer != null) {
            dataRefreshTimer.cancel();
            dataRefreshTimer = null;
        }

        // Create new timer that refreshes data at a consistent rate
        dataRefreshTimer = new Timer();
        dataRefreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!sessionEnded) {
                    refreshData();
                }
            }
        }, 0, 2000); // Initial delay 0, period 2 seconds for more predictable updates

        Log.d(TAG, "Data refresh timer started with 2-second interval");
    }

    private void refreshData() {
        // For testing with mock data
        if (useMockData) {
            lastTimestamp += 1000; // 1 second later
            int hr = 70 + (int)(Math.random() * 20);
            float speed = 5.0f + (float)(Math.random() * 2.0);
            float temp = 36.5f + (float)(Math.random() * 0.5);

            SensorData mockData = new SensorData();
            mockData.setTimestamp(lastTimestamp);
            mockData.setHeartRate(hr);
            mockData.setSpeed(speed);
            mockData.setTemperature(temp);
            mockData.setStatus("active");
            mockData.setAnomalyDetected(false);

            final SensorData finalMockData = mockData;
            mainHandler.post(() -> updateUI(finalMockData));
            return;
        }

        // Real data from repository
        sensorDataRepository.getLatestSensorData(sessionId, athleteId, new SensorDataRepository.SensorDataCallback() {
            @Override
            public void onSuccess(SensorData sensorData) {
                lastSensorData = sensorData; // Store for reference
                mainHandler.post(() -> updateUI(sensorData));
                Log.d(TAG, "Received sensor data: HR=" + sensorData.getHeartRate() +
                        ", Speed=" + sensorData.getSpeed() +
                        ", Temp=" + sensorData.getTemperature());
            }

            @Override
            public void onError(String errorMessage) {
                mainHandler.post(() -> {
                    Log.e(TAG, "Error fetching sensor data: " + errorMessage);
                    // Don't show toast for every failure as it might be annoying
                });
            }
        });
    }

    private void updateUI(SensorData sensorData) {
        if (sensorData == null || isFinishing() || isDestroyed()) {
            return;
        }

        // Update current values displayed in text views
        heartRateTextView.setText(String.format("%d bpm", sensorData.getHeartRate()));
        temperatureTextView.setText(String.format("%.1f°C", sensorData.getTemperature()));
        speedTextView.setText(String.format("%.1f km/h", sensorData.getSpeed()));

        // Add new data point to storage (but don't update charts yet)
        addDataPoint(sensorData.getTimestamp(),
                sensorData.getHeartRate(),
                (float)sensorData.getSpeed(),
                (float)sensorData.getTemperature(),
                sessionEnded); // Only update charts if session has ended

        // Check for anomalies
        if (sensorData.isAnomalyDetected()) {
            showAlert(sensorData);
        }
    }

    private void showAlert(SensorData sensorData) {
        // Increment alerts count
        setAlertsCount(alertsCount + 1);

        // Show alert card if not already visible
        if (alertCardView.getVisibility() != View.VISIBLE) {
            alertCardView.setVisibility(View.VISIBLE);
            // Reset contacted flag for new alert
            athleteContacted = false;
        }

        // Determine what's abnormal and set appropriate message
        StringBuilder message = new StringBuilder("ALERT: ");

        if (sensorData.getHeartRate() > 180) {
            message.append("Heart rate critically high (").append(sensorData.getHeartRate()).append(" bpm). ");
        } else if (sensorData.getHeartRate() < 40) {
            message.append("Heart rate critically low (").append(sensorData.getHeartRate()).append(" bpm). ");
        }

        if (sensorData.getTemperature() > 39) {
            message.append("Temperature critically high (").append(String.format("%.1f°C", sensorData.getTemperature())).append("). ");
        } else if (sensorData.getTemperature() < 35) {
            message.append("Temperature critically low (").append(String.format("%.1f°C", sensorData.getTemperature())).append("). ");
        }

        if (sensorData.getSpeed() == 0 && "active".equals(sensorData.getStatus())) {
            message.append("Athlete has stopped while session is active. ");
        }

        alertMessageTextView.setText(message.toString());

        // Update alert source if the view exists
        if (alertSourceTextView != null) {
            alertSourceTextView.setText("Detected by: ML Prediction API");
            alertSourceTextView.setVisibility(View.VISIBLE);
        }

        // Show contact button if it exists and athlete hasn't been contacted yet
        if (contactAthleteButton != null && !athleteContacted) {
            contactAthleteButton.setVisibility(View.VISIBLE);
        }
    }

    private void dismissAlert() {
        alertCardView.setVisibility(View.GONE);
        if (contactAthleteButton != null) {
            contactAthleteButton.setVisibility(View.GONE);
        }
    }

    private void contactAthlete() {
        // In a real app, this would trigger a notification to the athlete or initiate communication
        new AlertDialog.Builder(this)
                .setTitle("Contact " + athleteName)
                .setMessage("In a real implementation, this would send an emergency notification to the athlete or initiate a voice call.")
                .setPositiveButton("Send Alert", (dialog, which) -> {
                    Toast.makeText(this, "Emergency alert sent to " + athleteName, Toast.LENGTH_SHORT).show();
                    athleteContacted = true;
                    if (contactAthleteButton != null) {
                        contactAthleteButton.setVisibility(View.GONE);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmEndSession() {
        new AlertDialog.Builder(this)
                .setTitle("End Monitoring Session")
                .setMessage("Are you sure you want to end the monitoring session for " + athleteName + "?")
                .setPositiveButton("End Session", (dialog, which) -> {
                    endSession();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void endSession() {
        // Mark session as ended
        sessionEnded = true;

        // Stop data refresh timer
        if (dataRefreshTimer != null) {
            dataRefreshTimer.cancel();
            dataRefreshTimer = null;
        }

        // Hide monitoring UI elements
        if (endSessionButton != null) endSessionButton.setVisibility(View.GONE);
        alertCardView.setVisibility(View.GONE);

        // Show session summary view
        showSessionSummary();

        // Update session status in the database
        runSessionRepository.updateAthleteStatus(sessionId, athleteId, "completed",
                new RunSessionRepository.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Athlete session status updated to completed");
                        Toast.makeText(AthleteMonitoringActivity.this,
                                "Monitoring session ended", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "Failed to update session status: " + errorMessage);
                    }
                });
    }

    private void showSessionSummary() {
        // Show chart views and titles
        if (heartRateSpeedChart != null) heartRateSpeedChart.setVisibility(View.VISIBLE);
        if (temperatureChart != null) temperatureChart.setVisibility(View.VISIBLE);
        if (heartRateTitle != null) heartRateTitle.setVisibility(View.VISIBLE);
        if (temperatureTitle != null) temperatureTitle.setVisibility(View.VISIBLE);

        // Update title to indicate session summary
        if (heartRateTitle != null) heartRateTitle.setText("Session Summary: Heart Rate & Speed");
        if (temperatureTitle != null) temperatureTitle.setText("Session Summary: Body Temperature");

        // Show return to sessions button
        if (returnToSessionsButton != null) returnToSessionsButton.setVisibility(View.VISIBLE);

        // Initialize charts if they haven't been initialized yet
        if (heartRateDataSet == null) {
            setupCharts();
        }

        // Update charts with accumulated data
        updateChartsWithCurrentData();

        // Show session summary card if it exists
        if (summaryCardView != null) {
            summaryCardView.setVisibility(View.VISIBLE);
        }

        Toast.makeText(this, "Session summary loaded", Toast.LENGTH_SHORT).show();
    }

    private void showProgress(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void setAlertsCount(int count) {
        this.alertsCount = count;
        alertsCountTextView.setText(String.valueOf(count));

        // Change text color based on count
        if (count > 0) {
            alertsCountTextView.setTextColor(getResources().getColor(R.color.error_color));
        } else {
            alertsCountTextView.setTextColor(getResources().getColor(R.color.text_secondary));
        }
    }

    @Override
    public void onBackPressed() {
        // Show confirmation dialog if session is active
        if (!sessionEnded) {
            new AlertDialog.Builder(this)
                    .setTitle("Exit Monitoring")
                    .setMessage("Are you sure you want to exit? The monitoring session will continue in the background.")
                    .setPositiveButton("Exit", (dialog, which) -> super.onBackPressed())
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }
}