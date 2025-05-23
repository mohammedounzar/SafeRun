package com.example.saferun.ui.coach;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AthletePerformanceActivity extends AppCompatActivity {
    private static final String TAG = "AthletePerformance";

    private Toolbar toolbar;
    private TextView athleteNameText;
    private TextView performanceScoreText;
    private TextView avgHeartRateText;
    private TextView maxHeartRateText;
    private TextView avgTemperatureText;
    private TextView avgSpeedText;
    private TextView maxSpeedText;
    private TextView noSessionsText;
    private RecyclerView sessionsRecyclerView;
    private LineChart heartRateChart;
    private LineChart speedChart;
    private LineChart temperatureChart;
    private ProgressBar progressBar;

    private String athleteId;
    private String athleteName;
    private RunSessionRepository runSessionRepository;
    private SensorDataRepository sensorDataRepository;
    private RunSessionAdapter sessionAdapter;
    private List<RunSession> sessions = new ArrayList<>();

    // Maps to store sensor data for each session
    private Map<String, List<SensorData>> sessionSensorData = new HashMap<>();
    // Maps to store aggregated data per session for charts
    private Map<String, Double> sessionAvgHeartRate = new HashMap<>();
    private Map<String, Integer> sessionMaxHeartRate = new HashMap<>();
    private Map<String, Double> sessionAvgSpeed = new HashMap<>();
    private Map<String, Double> sessionMaxSpeed = new HashMap<>();
    private Map<String, Double> sessionAvgTemperature = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_athlete_performance);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get intent data
        athleteId = getIntent().getStringExtra("athlete_id");
        athleteName = getIntent().getStringExtra("athlete_name");

        if (athleteId == null) {
            Toast.makeText(this, "Error: No athlete selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Activity created with Athlete ID: " + athleteId + ", Name: " + athleteName);

        // Initialize repositories
        runSessionRepository = RunSessionRepository.getInstance();
        sensorDataRepository = SensorDataRepository.getInstance();

        // Initialize views
        initViews();

        // Set up toolbar
        setupToolbar();

        // Set up charts
        setupCharts();

        // Set up RecyclerView
        setupRecyclerView();

        // Load data
        loadSessions();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        athleteNameText = findViewById(R.id.athlete_name);
        performanceScoreText = findViewById(R.id.performance_score);
        avgHeartRateText = findViewById(R.id.avg_heart_rate);
        maxHeartRateText = findViewById(R.id.max_heart_rate);
        avgTemperatureText = findViewById(R.id.avg_temperature);
        avgSpeedText = findViewById(R.id.avg_speed);
        maxSpeedText = findViewById(R.id.max_speed);
        noSessionsText = findViewById(R.id.no_sessions_text);
        sessionsRecyclerView = findViewById(R.id.sessions_recycler_view);
        heartRateChart = findViewById(R.id.heart_rate_chart);
        speedChart = findViewById(R.id.speed_chart);
        temperatureChart = findViewById(R.id.temperature_chart);
        progressBar = findViewById(R.id.progress_bar);

        // Set athlete name
        athleteNameText.setText(athleteName);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Performance History");

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupCharts() {
        // Setup heart rate chart
        setupHeartRateChart();

        // Setup speed chart
        setupSpeedChart();

        // Setup temperature chart
        setupTemperatureChart();
    }

    private void setupHeartRateChart() {
        heartRateChart.getDescription().setEnabled(false);
        heartRateChart.setTouchEnabled(true);
        heartRateChart.setDragEnabled(true);
        heartRateChart.setScaleEnabled(true);
        heartRateChart.setPinchZoom(true);
        heartRateChart.setDrawGridBackground(false);
        heartRateChart.setBackgroundColor(Color.WHITE);

        XAxis xAxis = heartRateChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(45);
        xAxis.setLabelCount(5, false);

        YAxis leftAxis = heartRateChart.getAxisLeft();
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(200f);
        leftAxis.setDrawGridLines(true);
        heartRateChart.getAxisRight().setEnabled(false);

        Legend legend = heartRateChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(true);
        legend.setForm(Legend.LegendForm.SQUARE);
        legend.setFormSize(9f);
        legend.setTextSize(11f);
        legend.setXEntrySpace(4f);

        heartRateChart.setNoDataText("No heart rate data available");

        // Initial data
        heartRateChart.setData(new LineData());
    }

    private void setupSpeedChart() {
        speedChart.getDescription().setEnabled(false);
        speedChart.setTouchEnabled(true);
        speedChart.setDragEnabled(true);
        speedChart.setScaleEnabled(true);
        speedChart.setPinchZoom(true);
        speedChart.setDrawGridBackground(false);
        speedChart.setBackgroundColor(Color.WHITE);

        XAxis xAxis = speedChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(45);
        xAxis.setLabelCount(5, false);

        YAxis leftAxis = speedChart.getAxisLeft();
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(20f);
        leftAxis.setDrawGridLines(true);
        speedChart.getAxisRight().setEnabled(false);

        Legend legend = speedChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(true);
        legend.setForm(Legend.LegendForm.SQUARE);
        legend.setFormSize(9f);
        legend.setTextSize(11f);
        legend.setXEntrySpace(4f);

        speedChart.setNoDataText("No speed data available");

        // Initial data
        speedChart.setData(new LineData());
    }

    private void setupTemperatureChart() {
        temperatureChart.getDescription().setEnabled(false);
        temperatureChart.setTouchEnabled(true);
        temperatureChart.setDragEnabled(true);
        temperatureChart.setScaleEnabled(true);
        temperatureChart.setPinchZoom(true);
        temperatureChart.setDrawGridBackground(false);
        temperatureChart.setBackgroundColor(Color.WHITE);

        // Setup X axis for sessions
        XAxis xAxis = temperatureChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(45); // Rotate labels for better readability
        xAxis.setLabelCount(5, false);

        // Setup Y axis (temperature)
        YAxis leftAxis = temperatureChart.getAxisLeft();
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setAxisMinimum(34f); // Min reasonable temperature in Celsius
        leftAxis.setAxisMaximum(42f); // Max reasonable temperature in Celsius
        leftAxis.setDrawGridLines(true);

        // Disable right axis
        temperatureChart.getAxisRight().setEnabled(false);

        // Setup legend
        Legend legend = temperatureChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(true);
        legend.setForm(Legend.LegendForm.SQUARE);
        legend.setFormSize(9f);
        legend.setTextSize(11f);
        legend.setXEntrySpace(4f);

        // Empty data initially
        LineData data = new LineData();
        data.setValueTextSize(10f);
        temperatureChart.setData(data);
    }

    private void setupRecyclerView() {
        sessionAdapter = new RunSessionAdapter(this, sessions);
        sessionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        sessionsRecyclerView.setAdapter(sessionAdapter);
    }

    private void loadSessions() {
        showProgress(true);
        Log.d(TAG, "Loading sessions for athlete ID: " + athleteId);

        runSessionRepository.getRunSessionsByAthlete(athleteId, new RunSessionRepository.RunSessionsCallback() {
            @Override
            public void onSuccess(List<RunSession> loadedSessions) {
                Log.d(TAG, "Sessions loaded: " + (loadedSessions != null ? loadedSessions.size() : 0));

                sessions.clear();
                if (loadedSessions != null) {
                    // Log all sessions for debugging
                    for (RunSession session : loadedSessions) {
                        Log.d(TAG, "Session: ID=" + session.getId() +
                                ", Title=" + session.getTitle() +
                                ", Status=" + session.getStatus() +
                                ", Athletes=" + (session.getAthletes() != null ? session.getAthletes().size() : 0));
                    }

                    // Add both completed and active sessions for analysis
                    for (RunSession session : loadedSessions) {
                        if ("completed".equals(session.getStatus()) || "active".equals(session.getStatus())) {
                            sessions.add(session);
                            Log.d(TAG, "Added session: " + session.getId() + " with status: " + session.getStatus());
                        }
                    }
                }

                sessionAdapter.notifyDataSetChanged();

                if (sessions.isEmpty()) {
                    showNoSessions(true);
                    showProgress(false);
                    Log.d(TAG, "No sessions found for athlete " + athleteId);
                } else {
                    showNoSessions(false);
                    // Load sensor data for each session
                    loadSensorDataForSessions();
                }
            }

            @Override
            public void onError(String errorMessage) {
                showProgress(false);
                Toast.makeText(AthletePerformanceActivity.this,
                        "Error loading sessions: " + errorMessage,
                        Toast.LENGTH_SHORT).show();
                showNoSessions(true);
                Log.e(TAG, "Error loading sessions: " + errorMessage);
            }
        });
    }

    private void loadSensorDataForSessions() {
        // Use counter to track when all sensor data has been loaded
        final int[] loadedCount = {0};
        final int totalSessions = sessions.size();

        Log.d(TAG, "Loading sensor data for " + totalSessions + " sessions");

        if (totalSessions == 0) {
            showProgress(false);
            showNoSessions(true);
            return;
        }

        // Clear previous data
        sessionSensorData.clear();
        sessionAvgHeartRate.clear();
        sessionMaxHeartRate.clear();
        sessionAvgSpeed.clear();
        sessionMaxSpeed.clear();
        sessionAvgTemperature.clear();

        for (RunSession session : sessions) {
            String sessionId = session.getId();
            Log.d(TAG, "Loading sensor data for session: " + sessionId +
                    ", Title: " + session.getTitle() +
                    ", Date: " + (session.getDate() != null ? session.getDate().toString() : "null"));

            sensorDataRepository.getSensorDataHistory(sessionId, athleteId, 1000,
                    new SensorDataRepository.SensorDataListCallback() {
                        @Override
                        public void onSuccess(List<SensorData> sensorDataList) {
                            Log.d(TAG, "Sensor data loaded successfully for session " + sessionId +
                                    ": " + (sensorDataList != null ? sensorDataList.size() : 0) + " data points");

                            if (sensorDataList != null && !sensorDataList.isEmpty()) {
                                sessionSensorData.put(sessionId, sensorDataList);

                                // Calculate avg, max values for this session
                                processSessionData(sessionId, sensorDataList);

                                // Log some sample data points for debugging
                                if (sensorDataList.size() > 0) {
                                    SensorData firstPoint = sensorDataList.get(0);
                                    Log.d(TAG, "Sample data point: HR=" + firstPoint.getHeartRate() +
                                            ", Temp=" + firstPoint.getTemperature() +
                                            ", Speed=" + firstPoint.getSpeed());
                                }
                            } else {
                                Log.w(TAG, "No sensor data found for session: " + sessionId);
                                sessionSensorData.put(sessionId, new ArrayList<>());
                            }

                            loadedCount[0]++;
                            checkAllDataLoaded(loadedCount[0], totalSessions);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Log.e(TAG, "Error loading sensor data for session " +
                                    sessionId + ": " + errorMessage);

                            // Still need to track this session attempt even on error
                            sessionSensorData.put(sessionId, new ArrayList<>());
                            loadedCount[0]++;
                            checkAllDataLoaded(loadedCount[0], totalSessions);
                        }
                    });
        }
    }

    private void processSessionData(String sessionId, List<SensorData> dataList) {
        // Calculate per-session statistics
        double sumHeartRate = 0;
        int maxHeartRate = 0;
        int validHeartRateCount = 0;

        double sumSpeed = 0;
        double maxSpeed = 0;
        int validSpeedCount = 0;

        double sumTemperature = 0;
        int validTempCount = 0;

        // Log data points for debugging
        Log.d(TAG, "Processing session data for " + sessionId + " with " + dataList.size() + " points");

        for (SensorData data : dataList) {
            // Process heart rate
            int heartRate = data.getHeartRate();
            if (heartRate > 0) {
                sumHeartRate += heartRate;
                validHeartRateCount++;
                maxHeartRate = Math.max(maxHeartRate, heartRate);
                Log.d(TAG, "Valid heart rate: " + heartRate);
            }

            // Process speed
            double speed = data.getSpeed();
            if (speed >= 0) {
                sumSpeed += speed;
                validSpeedCount++;
                maxSpeed = Math.max(maxSpeed, speed);
                Log.d(TAG, "Valid speed: " + speed);
            }

            // Process temperature
            double temperature = data.getTemperature();
            if (temperature > 0) {
                sumTemperature += temperature;
                validTempCount++;
                Log.d(TAG, "Valid temperature: " + temperature);
            }
        }

        Log.d(TAG, "Session " + sessionId + " has " + validHeartRateCount + " valid HR readings.");
        Log.d(TAG, "All HR values for session " + sessionId + ":");
        for (SensorData d : dataList) {
            Log.d(TAG, "HR = " + d.getHeartRate());
        }

        // Calculate averages and store in maps
        if (validHeartRateCount > 0) {
            double avgHeartRate = sumHeartRate / validHeartRateCount;
            sessionAvgHeartRate.put(sessionId, avgHeartRate);
            sessionMaxHeartRate.put(sessionId, maxHeartRate);
            Log.d(TAG, "Session " + sessionId + " - Avg HR: " + avgHeartRate + ", Max HR: " + maxHeartRate +
                    " (from " + validHeartRateCount + " readings)");
        } else {
            Log.w(TAG, "No valid heart rate readings for session " + sessionId);
        }

        if (validSpeedCount > 0) {
            double avgSpeed = sumSpeed / validSpeedCount;
            sessionAvgSpeed.put(sessionId, avgSpeed);
            sessionMaxSpeed.put(sessionId, maxSpeed);
            Log.d(TAG, "Session " + sessionId + " - Avg Speed: " + avgSpeed + ", Max Speed: " + maxSpeed +
                    " (from " + validSpeedCount + " readings)");
        } else {
            Log.w(TAG, "No valid speed readings for session " + sessionId);
        }

        if (validTempCount > 0) {
            double avgTemperature = sumTemperature / validTempCount;
            sessionAvgTemperature.put(sessionId, avgTemperature);
            Log.d(TAG, "Session " + sessionId + " - Avg Temperature: " + avgTemperature +
                    " (from " + validTempCount + " readings)");
        } else {
            Log.w(TAG, "No valid temperature readings for session " + sessionId);
        }
    }

    private void checkAllDataLoaded(int loadedCount, int totalSessions) {
        if (loadedCount >= totalSessions) {
            if (isAllSensorDataEmpty()) {
                Log.w(TAG, "No sensor data found for any session");

                // Show message to user but still display the session list
                Toast.makeText(this, "No performance data available yet for this athlete",
                        Toast.LENGTH_LONG).show();

                // Zero out all stats
                avgHeartRateText.setText("0 bpm");
                maxHeartRateText.setText("0 bpm");
                avgTemperatureText.setText("0.0°C");
                avgSpeedText.setText("0.0 km/h");
                maxSpeedText.setText("0.0 km/h");
                performanceScoreText.setText("0");

                // Clear charts
                clearCharts();
            } else {
                // We have some data, let's update the UI
                updateChartsAndStats();
            }

            // Always hide progress indicator when loading is complete
            showProgress(false);
        }
    }

    private void clearCharts() {
        heartRateChart.clear();
        heartRateChart.setNoDataText("No heart rate data available");
        heartRateChart.invalidate();

        speedChart.clear();
        speedChart.setNoDataText("No speed data available");
        speedChart.invalidate();

        temperatureChart.clear();
        temperatureChart.setNoDataText("No temperature data available");
        temperatureChart.invalidate();
    }

    private boolean isAllSensorDataEmpty() {
        for (List<SensorData> dataList : sessionSensorData.values()) {
            if (dataList != null && !dataList.isEmpty()) {
                // Check if the list actually has any useful data
                boolean hasRealData = dataList.stream().anyMatch(data ->
                        data.getHeartRate() > 0 ||
                                data.getTemperature() > 0 ||
                                data.getSpeed() > 0);

                if (hasRealData) {
                    return false;
                }
            }
        }
        return true;
    }

    private void updateChartsAndStats() {
        // Initialize totals
        double totalHeartRateSum = 0;
        int totalHeartRateReadings = 0;
        int globalMaxHeartRate = 0;

        double totalSpeedSum = 0;
        int totalSpeedReadings = 0;
        double globalMaxSpeed = 0;

        double totalTempSum = 0;
        int totalTempReadings = 0;

        // Loop through all sensor data to get true global stats
        for (List<SensorData> dataList : sessionSensorData.values()) {
            for (SensorData data : dataList) {
                // Heart Rate
                int hr = data.getHeartRate();
                if (hr > 0) {
                    totalHeartRateSum += hr;
                    totalHeartRateReadings++;
                    globalMaxHeartRate = Math.max(globalMaxHeartRate, hr);
                }

                // Speed
                double speed = data.getSpeed();
                if (speed >= 0) {
                    totalSpeedSum += speed;
                    totalSpeedReadings++;
                    globalMaxSpeed = Math.max(globalMaxSpeed, speed);
                }

                // Temperature
                double temp = data.getTemperature();
                if (temp > 0) {
                    totalTempSum += temp;
                    totalTempReadings++;
                }
            }
        }

        // Calculate averages
        double globalAvgHeartRate = totalHeartRateReadings > 0 ? totalHeartRateSum / totalHeartRateReadings : 0;
        double globalAvgSpeed = totalSpeedReadings > 0 ? totalSpeedSum / totalSpeedReadings : 0;
        double globalAvgTemp = totalTempReadings > 0 ? totalTempSum / totalTempReadings : 0;

        // Calculate performance score
        double performanceScore = calculatePerformanceScore(globalAvgHeartRate, globalMaxHeartRate, globalAvgSpeed, globalMaxSpeed);

        // Update UI
        avgHeartRateText.setText(String.format("%.0f bpm", globalAvgHeartRate));
        maxHeartRateText.setText(String.format("%d bpm", globalMaxHeartRate));
        avgTemperatureText.setText(String.format("%.1f°C", globalAvgTemp));
        avgSpeedText.setText(String.format("%.1f km/h", globalAvgSpeed));
        maxSpeedText.setText(String.format("%.1f km/h", globalMaxSpeed));
        performanceScoreText.setText(String.format("%.0f", performanceScore));

        // Update charts (uses session-based aggregates)
        updateHeartRateChart();
        updateSpeedChart();
        updateTemperatureChart();
    }


    private double calculatePerformanceScore(double avgHeartRate, int maxHeartRate,
                                             double avgSpeed, double maxSpeed) {
        // A simplified performance score calculation
        // Real implementation would be more sophisticated

        // Heart rate efficiency: how well the athlete maintains their heart rate
        double heartRateEfficiency = 0;
        if (avgHeartRate > 0 && maxHeartRate > 0) {
            heartRateEfficiency = (maxHeartRate > 180) ? 80 : 100;
        }

        // Speed performance: based on average and max speed
        double speedPerformance = 0;
        if (avgSpeed > 0) {
            speedPerformance = (avgSpeed * 5) + (maxSpeed * 2);
        }

        // Combined score (simplified)
        double score = (heartRateEfficiency * 0.4) + (speedPerformance * 0.6);

        // Normalize to 0-100 scale
        score = Math.min(100, Math.max(0, score));

        return score;
    }

    private void updateHeartRateChart() {
        List<String> sessionLabels = new ArrayList<>();
        List<Entry> avgHREntries = new ArrayList<>();
        List<Entry> maxHREntries = new ArrayList<>();

        int sessionLimit = Math.min(sessions.size(), 6);
        int index = 0;

        for (int i = Math.max(0, sessions.size() - sessionLimit); i < sessions.size(); i++) {
            RunSession session = sessions.get(i);
            String sessionId = session.getId();
            String label = abbreviateSessionTitle(session.getTitle());
            sessionLabels.add(label);

            if (sessionAvgHeartRate.containsKey(sessionId)) {
                float avgHR = sessionAvgHeartRate.get(sessionId).floatValue();
                avgHREntries.add(new Entry(index, avgHR));
                Log.d(TAG, "AvgHR for session " + label + ": " + avgHR);
            }

            if (sessionMaxHeartRate.containsKey(sessionId)) {
                float maxHR = sessionMaxHeartRate.get(sessionId);
                maxHREntries.add(new Entry(index, maxHR));
                Log.d(TAG, "MaxHR for session " + label + ": " + maxHR);
            }

            index++;
        }

        if (avgHREntries.isEmpty() && maxHREntries.isEmpty()) {
            heartRateChart.clear();
            heartRateChart.setNoDataText("No heart rate data available");
            heartRateChart.invalidate();
            return;
        }

        List<ILineDataSet> dataSets = new ArrayList<>();

        if (!avgHREntries.isEmpty()) {
            LineDataSet avgDataSet = new LineDataSet(avgHREntries, "Avg HR");
            avgDataSet.setColor(Color.rgb(135, 206, 235));
            avgDataSet.setCircleColor(Color.rgb(135, 206, 235));
            avgDataSet.setLineWidth(2f);
            avgDataSet.setCircleRadius(4f);
            avgDataSet.setDrawCircleHole(false);
            avgDataSet.setValueTextSize(10f);
            avgDataSet.setValueTextColor(Color.BLACK);
            dataSets.add(avgDataSet);
        }

        if (!maxHREntries.isEmpty()) {
            LineDataSet maxDataSet = new LineDataSet(maxHREntries, "Max HR");
            maxDataSet.setColor(Color.rgb(255, 99, 71));
            maxDataSet.setCircleColor(Color.rgb(255, 99, 71));
            maxDataSet.setLineWidth(2f);
            maxDataSet.setCircleRadius(4f);
            maxDataSet.setDrawCircleHole(false);
            maxDataSet.setValueTextSize(10f);
            maxDataSet.setValueTextColor(Color.BLACK);
            dataSets.add(maxDataSet);
        }

        LineData lineData = new LineData(dataSets);
        lineData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.0f", value);
            }
        });

        heartRateChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(sessionLabels));
        heartRateChart.setData(lineData);
        heartRateChart.invalidate();
    }

    private void updateSpeedChart() {
        List<String> sessionLabels = new ArrayList<>();
        List<Entry> avgSpeedEntries = new ArrayList<>();
        List<Entry> maxSpeedEntries = new ArrayList<>();

        int sessionLimit = Math.min(sessions.size(), 6);
        int index = 0;

        for (int i = Math.max(0, sessions.size() - sessionLimit); i < sessions.size(); i++) {
            RunSession session = sessions.get(i);
            String sessionId = session.getId();
            String label = abbreviateSessionTitle(session.getTitle());
            sessionLabels.add(label);

            if (sessionAvgSpeed.containsKey(sessionId)) {
                float avgSpeed = sessionAvgSpeed.get(sessionId).floatValue();
                avgSpeedEntries.add(new Entry(index, avgSpeed));
                Log.d(TAG, "AvgSpeed for session " + label + ": " + avgSpeed);
            }

            if (sessionMaxSpeed.containsKey(sessionId)) {
                float maxSpeed = sessionMaxSpeed.get(sessionId).floatValue();
                maxSpeedEntries.add(new Entry(index, maxSpeed));
                Log.d(TAG, "MaxSpeed for session " + label + ": " + maxSpeed);
            }

            index++;
        }

        if (avgSpeedEntries.isEmpty() && maxSpeedEntries.isEmpty()) {
            speedChart.clear();
            speedChart.setNoDataText("No speed data available");
            speedChart.invalidate();
            return;
        }

        List<ILineDataSet> dataSets = new ArrayList<>();

        if (!avgSpeedEntries.isEmpty()) {
            LineDataSet avgDataSet = new LineDataSet(avgSpeedEntries, "Avg Speed");
            avgDataSet.setColor(Color.rgb(144, 238, 144));
            avgDataSet.setCircleColor(Color.rgb(144, 238, 144));
            avgDataSet.setLineWidth(2f);
            avgDataSet.setCircleRadius(4f);
            avgDataSet.setDrawCircleHole(false);
            avgDataSet.setValueTextSize(10f);
            avgDataSet.setValueTextColor(Color.BLACK);
            dataSets.add(avgDataSet);
        }

        if (!maxSpeedEntries.isEmpty()) {
            LineDataSet maxDataSet = new LineDataSet(maxSpeedEntries, "Max Speed");
            maxDataSet.setColor(Color.rgb(34, 139, 34));
            maxDataSet.setCircleColor(Color.rgb(34, 139, 34));
            maxDataSet.setLineWidth(2f);
            maxDataSet.setCircleRadius(4f);
            maxDataSet.setDrawCircleHole(false);
            maxDataSet.setValueTextSize(10f);
            maxDataSet.setValueTextColor(Color.BLACK);
            dataSets.add(maxDataSet);
        }

        LineData lineData = new LineData(dataSets);
        lineData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.1f", value);
            }
        });

        speedChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(sessionLabels));
        speedChart.setData(lineData);
        speedChart.invalidate();
    }

    private void updateTemperatureChart() {
        // Session title abbreviations to use as labels
        List<String> sessionLabels = new ArrayList<>();

        // Create entries for average temperature
        List<Entry> avgEntries = new ArrayList<>();

        // Populate data - limit to last 6 sessions to avoid overcrowding
        int sessionLimit = Math.min(sessions.size(), 6);
        int chartIndex = 0;

        // We'll use the most recent sessions for display
        for (int i = Math.max(0, sessions.size() - sessionLimit); i < sessions.size(); i++) {
            RunSession session = sessions.get(i);
            String sessionId = session.getId();

            // Add session label (create abbreviated version of title)
            String label = abbreviateSessionTitle(session.getTitle());
            sessionLabels.add(label);

            // Add temperature data if available
            if (sessionAvgTemperature.containsKey(sessionId)) {
                double avgTemp = sessionAvgTemperature.get(sessionId);
                // For line charts, we use Entry instead of BarEntry
                avgEntries.add(new Entry(chartIndex, (float)avgTemp));
                chartIndex++;
            }
        }

        // Check if we have data to display
        if (avgEntries.isEmpty()) {
            temperatureChart.clear();
            temperatureChart.setNoDataText("No temperature data available");
            temperatureChart.invalidate();
            return;
        }

        // Create dataset for average temperature - this changes from BarDataSet to LineDataSet
        LineDataSet avgDataSet = new LineDataSet(avgEntries, "Avg Temperature");
        avgDataSet.setColor(Color.rgb(255, 165, 0)); // Orange
        avgDataSet.setLineWidth(2f);
        avgDataSet.setCircleColor(Color.rgb(255, 165, 0));
        avgDataSet.setCircleRadius(4f);
        avgDataSet.setDrawCircleHole(false);
        avgDataSet.setValueTextSize(10f);
        avgDataSet.setValueTextColor(Color.BLACK);
        avgDataSet.setDrawValues(true); // Ensure values are drawn

        // Create line data with the dataset - this changes from BarData to LineData
        LineData lineData = new LineData(avgDataSet);

        // Set value formatter to display one decimal place for temperature
        lineData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.1f", value);
            }
        });

        // Set value text properties
        lineData.setValueTextSize(9f);
        lineData.setValueTextColor(Color.BLACK);

        // Set labels on X axis
        temperatureChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(sessionLabels));

        // Configure chart and set data
        temperatureChart.setData(lineData);

        // Set visible range to show all points
        temperatureChart.setVisibleXRangeMaximum(6); // Show maximum 6 points

        // Refresh the chart
        temperatureChart.invalidate();
        Log.d(TAG, "Temperature chart updated with " + avgEntries.size() + " sessions");
    }

    /**
     * Creates an abbreviated version of the session title for chart labels
     */
    private String abbreviateSessionTitle(String title) {
        if (title == null || title.isEmpty()) {
            return "Session";
        }

        // If title is already short enough, return as is
        if (title.length() <= 8) {
            return title;
        }

        // Extract first word and abbreviate
        String[] words = title.split("\\s+");
        if (words.length > 0) {
            String firstWord = words[0];
            if (firstWord.length() > 6) {
                return firstWord.substring(0, 6) + "...";
            } else {
                return firstWord + "...";
            }
        }

        // Fallback
        return title.substring(0, 6) + "...";
    }

    private void showNoSessions(boolean show) {
        if (show) {
            noSessionsText.setVisibility(View.VISIBLE);
            sessionsRecyclerView.setVisibility(View.GONE);
        } else {
            noSessionsText.setVisibility(View.GONE);
            sessionsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * Adapter for displaying run sessions in the RecyclerView
     */
    private static class RunSessionAdapter extends RecyclerView.Adapter<RunSessionAdapter.ViewHolder> {
        private Context context;
        private List<RunSession> sessions;
        private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

        public RunSessionAdapter(Context context, List<RunSession> sessions) {
            this.context = context;
            this.sessions = sessions;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_run_session, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            RunSession session = sessions.get(position);

            // Set session title
            holder.titleTextView.setText(session.getTitle());

            // Set session date
            if (session.getDate() != null) {
                holder.dateTextView.setText(dateFormat.format(session.getDate()));
            } else {
                holder.dateTextView.setText("No date");
            }

            // Set session details
            String details = String.format(Locale.getDefault(), "%.1f km • %d min",
                    session.getDistance(), session.getDuration());
            holder.detailsTextView.setText(details);

            // Set session status with appropriate color
            holder.statusTextView.setText(capitalizeFirstLetter(session.getStatus()));

            // Set color based on status
            int statusColor;
            if ("completed".equals(session.getStatus())) {
                statusColor = ContextCompat.getColor(context, R.color.success_color);
            } else if ("active".equals(session.getStatus())) {
                statusColor = ContextCompat.getColor(context, R.color.active_color);
            } else {
                statusColor = ContextCompat.getColor(context, R.color.accent_color);
            }
            holder.statusTextView.setBackgroundTintList(ColorStateList.valueOf(statusColor));
        }

        @Override
        public int getItemCount() {
            return sessions.size();
        }

        private String capitalizeFirstLetter(String text) {
            if (text == null || text.isEmpty()) {
                return text;
            }
            return text.substring(0, 1).toUpperCase() + text.substring(1);
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView titleTextView;
            TextView dateTextView;
            TextView detailsTextView;
            TextView statusTextView;

            public ViewHolder(View itemView) {
                super(itemView);
                titleTextView = itemView.findViewById(R.id.session_title);
                dateTextView = itemView.findViewById(R.id.session_date);
                detailsTextView = itemView.findViewById(R.id.session_details);
                statusTextView = itemView.findViewById(R.id.session_status);
            }
        }
    }
}