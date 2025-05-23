package com.example.saferun.ui.coach;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.saferun.R;
import com.example.saferun.data.model.RunSession;
import com.example.saferun.data.model.SensorData;
import com.example.saferun.data.model.User;
import com.example.saferun.data.repository.RunSessionRepository;
import com.example.saferun.data.repository.SensorDataRepository;
import com.example.saferun.data.repository.UserRepository;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalPerformanceActivity extends AppCompatActivity {

    private static final String TAG = "GlobalPerformance";

    private Toolbar toolbar;
    private TextView totalSessionsText;
    private TextView totalDistanceText;
    private TextView totalDurationText;
    private TextView totalAthletesText;
    private TextView noDataText;
    private HorizontalBarChart performanceChart;
    private BarChart heartRateChart;
    private BarChart speedChart;
    private ProgressBar progressBar;

    private UserRepository userRepository;
    private RunSessionRepository runSessionRepository;
    private SensorDataRepository sensorDataRepository;

    private List<User> athletes = new ArrayList<>();
    private Map<String, User> athleteMap = new HashMap<>();  // For quick lookup
    private List<RunSession> allSessions = new ArrayList<>();
    private Map<String, List<SensorData>> athleteSensorData = new HashMap<>();
    private Map<String, Double> athletePerformanceScores = new HashMap<>();

    // Separate maps for average and maximum values
    private Map<String, Double> athleteAvgHeartRate = new HashMap<>();
    private Map<String, Double> athleteMaxHeartRate = new HashMap<>();
    private Map<String, Double> athleteAvgSpeed = new HashMap<>();
    private Map<String, Double> athleteMaxSpeed = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_global_performance);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize repositories
        userRepository = UserRepository.getInstance();
        runSessionRepository = RunSessionRepository.getInstance();
        sensorDataRepository = SensorDataRepository.getInstance();

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Setup charts
        setupCharts();

        // Load data
        loadAthletes();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        totalSessionsText = findViewById(R.id.total_sessions);
        totalDistanceText = findViewById(R.id.total_distance);
        totalDurationText = findViewById(R.id.total_duration);
        totalAthletesText = findViewById(R.id.total_athletes);
        noDataText = findViewById(R.id.no_data_text);
        performanceChart = findViewById(R.id.performance_chart);
        heartRateChart = findViewById(R.id.heart_rate_chart);
        speedChart = findViewById(R.id.speed_chart);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Global Performance");

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupCharts() {
        // Setup performance chart (horizontal bar chart)
        setupPerformanceChart();

        // Setup heart rate chart
        setupHeartRateChart();

        // Setup speed chart
        setupSpeedChart();
    }

    private void setupPerformanceChart() {
        performanceChart.getDescription().setEnabled(false);
        performanceChart.setDrawGridBackground(false);
        performanceChart.setDrawBarShadow(false);
        performanceChart.setHighlightFullBarEnabled(false);

        XAxis xAxis = performanceChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(10f);

        YAxis leftAxis = performanceChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(100f); // Performance score is 0-100

        YAxis rightAxis = performanceChart.getAxisRight();
        rightAxis.setEnabled(false);

        Legend legend = performanceChart.getLegend();
        legend.setEnabled(false);

        // Empty data initially
        performanceChart.setData(new BarData());
    }

    private void setupHeartRateChart() {
        heartRateChart.getDescription().setEnabled(false);
        heartRateChart.setDrawGridBackground(false);
        heartRateChart.setDrawBarShadow(false);

        XAxis xAxis = heartRateChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(10f);
        xAxis.setLabelRotationAngle(30); // Rotate labels for better visibility

        YAxis leftAxis = heartRateChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(200f); // Max heart rate

        YAxis rightAxis = heartRateChart.getAxisRight();
        rightAxis.setEnabled(false);

        Legend legend = heartRateChart.getLegend();
        legend.setForm(Legend.LegendForm.SQUARE);
        legend.setTextSize(10f);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);

        // Empty data initially
        heartRateChart.setData(new BarData());
    }

    private void setupSpeedChart() {
        speedChart.getDescription().setEnabled(false);
        speedChart.setDrawGridBackground(false);
        speedChart.setDrawBarShadow(false);

        XAxis xAxis = speedChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(10f);
        xAxis.setLabelRotationAngle(30); // Rotate labels for better visibility

        YAxis leftAxis = speedChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(25f); // Max speed

        YAxis rightAxis = speedChart.getAxisRight();
        rightAxis.setEnabled(false);

        Legend legend = speedChart.getLegend();
        legend.setForm(Legend.LegendForm.SQUARE);
        legend.setTextSize(10f);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);

        // Empty data initially
        speedChart.setData(new BarData());
    }

    private void loadAthletes() {
        showProgress(true);

        userRepository.getAthletes(new UserRepository.UsersCallback() {
            @Override
            public void onSuccess(List<User> users) {
                athletes.clear();
                athleteMap.clear();
                athletes.addAll(users);

                // Populate the athlete map for quick lookups
                for (User athlete : athletes) {
                    athleteMap.put(athlete.getUid(), athlete);
                }

                if (athletes.isEmpty()) {
                    showNoData(true);
                    showProgress(false);
                } else {
                    showNoData(false);
                    loadSessions();
                }
            }

            @Override
            public void onError(String errorMessage) {
                showProgress(false);
                showNoData(true);
                Toast.makeText(GlobalPerformanceActivity.this,
                        "Error loading athletes: " + errorMessage,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSessions() {
        final int[] loadedCount = {0};
        final int totalAthletes = athletes.size();

        // Update total athletes count
        totalAthletesText.setText(String.valueOf(totalAthletes));

        for (User athlete : athletes) {
            runSessionRepository.getRunSessionsByAthlete(athlete.getUid(),
                    new RunSessionRepository.RunSessionsCallback() {
                        @Override
                        public void onSuccess(List<RunSession> sessions) {
                            if (sessions != null && !sessions.isEmpty()) {
                                allSessions.addAll(sessions);
                            }

                            loadedCount[0]++;
                            if (loadedCount[0] >= totalAthletes) {
                                // All sessions loaded
                                loadSensorData();
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            loadedCount[0]++;
                            if (loadedCount[0] >= totalAthletes) {
                                // All sessions attempts completed
                                loadSensorData();
                            }
                        }
                    });
        }
    }

    private void loadSensorData() {
        // Update session stats
        totalSessionsText.setText(String.valueOf(allSessions.size()));

        double totalDistance = 0;
        long totalDuration = 0;

        for (RunSession session : allSessions) {
            totalDistance += session.getDistance();
            totalDuration += session.getDuration();
        }

        totalDistanceText.setText(String.format("%.1f km", totalDistance));
        totalDurationText.setText(String.format("%d min", totalDuration));

        // DEBUGGING OPTION: Use simulated data instead of real DB data
        boolean useSimulatedData = true;  // Set to false to use real data from DB

        if (useSimulatedData) {
            Log.d(TAG, "Using simulated data for reliable testing");
            loadSimulatedData();
            return;
        }

        // If no sessions, we can't load sensor data
        if (allSessions.isEmpty()) {
            showProgress(false);
            return;
        }

        final int[] loadedCount = {0};
        final int totalAthletes = athletes.size();

        for (User athlete : athletes) {
            String athleteId = athlete.getUid();
            List<SensorData> athleteData = new ArrayList<>();

            // For simplicity, we'll just load data from one session per athlete
            // In a real app, you'd aggregate data from all sessions
            String sessionId = null;

            for (RunSession session : allSessions) {
                if (session.getAthletes().contains(athleteId) && session.isCompleted()) {
                    sessionId = session.getId();
                    break;
                }
            }

            if (sessionId != null) {
                final String finalSessionId = sessionId;

                sensorDataRepository.getSensorDataHistory(sessionId, athleteId, 1000,
                        new SensorDataRepository.SensorDataListCallback() {
                            @Override
                            public void onSuccess(List<SensorData> sensorDataList) {
                                athleteSensorData.put(athleteId, sensorDataList);

                                // Process the athlete's data
                                processAthletePerformanceData(athlete, sensorDataList);

                                // Calculate performance score for this athlete
                                double score = calculatePerformanceScore(athleteId, sensorDataList);
                                athletePerformanceScores.put(athleteId, score);

                                loadedCount[0]++;
                                if (loadedCount[0] >= totalAthletes) {
                                    // All sensor data loaded
                                    updateCharts();
                                    showProgress(false);
                                }
                            }

                            @Override
                            public void onError(String errorMessage) {
                                loadedCount[0]++;
                                if (loadedCount[0] >= totalAthletes) {
                                    // All sensor data attempts completed
                                    updateCharts();
                                    showProgress(false);
                                }
                            }
                        });
            } else {
                loadedCount[0]++;
                if (loadedCount[0] >= totalAthletes) {
                    // All sensor data attempts completed
                    updateCharts();
                    showProgress(false);
                }
            }
        }
    }

    /**
     * Generate simulated data for testing
     * This allows testing the charts with known good data
     */
    private void loadSimulatedData() {
        // Clear any existing data
        athleteSensorData.clear();
        athleteAvgHeartRate.clear();
        athleteMaxHeartRate.clear();
        athleteAvgSpeed.clear();
        athleteMaxSpeed.clear();
        athletePerformanceScores.clear();

        // Simulate data for each athlete
        for (User athlete : athletes) {
            String athleteId = athlete.getUid();

            // Create simulated data with known averages and maximums
            List<SensorData> simulatedData = new ArrayList<>();

            // Create different values for each athlete
            double baseHR = 70 + (Math.random() * 20);
            double baseSpeed = 4 + (Math.random() * 3);

            // Create 20 data points with variation
            for (int i = 0; i < 20; i++) {
                SensorData data = new SensorData();
                data.setTimestamp(System.currentTimeMillis() - (i * 1000));

                // Heart rate: ensure average is notably different from max
                double factor = Math.random();
                double hr = baseHR + (factor * 40);  // Ensures max is higher than average
                data.setHeartRate((int)hr);

                // Speed: ensure average is notably different from max
                double speed = baseSpeed + (factor * 5);  // Ensures max is higher than average
                data.setSpeed(speed);

                // Temperature is less important for this test
                data.setTemperature(36.5 + (Math.random() * 1.5));

                simulatedData.add(data);
            }

            // Store the simulated data
            athleteSensorData.put(athleteId, simulatedData);

            // Process this data
            processAthletePerformanceData(athlete, simulatedData);

            // Calculate performance score
            double score = calculatePerformanceScore(athleteId, simulatedData);
            athletePerformanceScores.put(athleteId, score);
        }

        // Update charts with the simulated data
        updateCharts();
        showProgress(false);
    }

    /**
     * Processes athlete performance data to prepare for chart rendering
     * Making sure average and maximum values are properly separated
     */
    private void processAthletePerformanceData(User athlete, List<SensorData> dataList) {
        // Calculate per-athlete statistics
        double sumHeartRate = 0;
        int maxHeartRate = 0;
        int validHeartRateCount = 0;

        double sumSpeed = 0;
        double maxSpeed = 0;
        int validSpeedCount = 0;

        // Log for debugging
        Log.d(TAG, "Processing data for athlete: " + athlete.getName() +
                " with " + (dataList != null ? dataList.size() : 0) + " data points");

        if (dataList == null || dataList.isEmpty()) {
            Log.w(TAG, "No data found for athlete: " + athlete.getName());
            return;
        }

        // IMPORTANT: Thoroughly inspect the data before processing
        Log.d(TAG, "First few data points for " + athlete.getName() + ":");
        int sampleSize = Math.min(5, dataList.size());
        for (int i = 0; i < sampleSize; i++) {
            SensorData data = dataList.get(i);
            Log.d(TAG, "Data point " + i + ": HR=" + data.getHeartRate() +
                    ", Speed=" + data.getSpeed() + ", Temp=" + data.getTemperature());
        }

        for (SensorData data : dataList) {
            // Process heart rate - only count valid readings
            if (data.getHeartRate() > 0) {
                sumHeartRate += data.getHeartRate();
                validHeartRateCount++;

                // Track max heart rate
                if (data.getHeartRate() > maxHeartRate) {
                    maxHeartRate = data.getHeartRate();
                }
            }

            // Process speed - only count valid readings
            if (data.getSpeed() > 0) {  // Changed from >= 0 to > 0
                sumSpeed += data.getSpeed();
                validSpeedCount++;

                // Track max speed
                if (data.getSpeed() > maxSpeed) {
                    maxSpeed = data.getSpeed();
                }
            }
        }

        // Calculate averages and store in maps - with thorough checks
        if (validHeartRateCount > 0) {
            double avgHeartRate = sumHeartRate / validHeartRateCount;

            // Verify avg is not equal to max (sanity check)
            if (Math.abs(avgHeartRate - maxHeartRate) < 0.001 && validHeartRateCount > 1) {
                Log.w(TAG, "WARNING: Average heart rate equals max heart rate for " +
                        athlete.getName() + " with " + validHeartRateCount + " readings");
            }

            // Store in separate maps for avg and max
            athleteAvgHeartRate.put(athlete.getUid(), avgHeartRate);
            athleteMaxHeartRate.put(athlete.getUid(), (double)maxHeartRate);

            Log.d(TAG, "Athlete " + athlete.getName() +
                    " - Avg HR: " + avgHeartRate +
                    ", Max HR: " + maxHeartRate +
                    ", From " + validHeartRateCount + " readings" +
                    ", Sum: " + sumHeartRate);
        } else {
            Log.w(TAG, "No valid heart rate readings for " + athlete.getName());
        }

        if (validSpeedCount > 0) {
            double avgSpeed = sumSpeed / validSpeedCount;

            // Verify avg is not equal to max (sanity check)
            if (Math.abs(avgSpeed - maxSpeed) < 0.001 && validSpeedCount > 1) {
                Log.w(TAG, "WARNING: Average speed equals max speed for " +
                        athlete.getName() + " with " + validSpeedCount + " readings");
            }

            // Store in separate maps for avg and max
            athleteAvgSpeed.put(athlete.getUid(), avgSpeed);
            athleteMaxSpeed.put(athlete.getUid(), maxSpeed);

            Log.d(TAG, "Athlete " + athlete.getName() +
                    " - Avg Speed: " + avgSpeed +
                    ", Max Speed: " + maxSpeed +
                    ", From " + validSpeedCount + " readings" +
                    ", Sum: " + sumSpeed);
        } else {
            Log.w(TAG, "No valid speed readings for " + athlete.getName());
        }
    }

    private double calculatePerformanceScore(String athleteId, List<SensorData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return 0;
        }

        // Use the pre-calculated values from our maps if available
        double avgHeartRate = athleteAvgHeartRate.containsKey(athleteId) ?
                athleteAvgHeartRate.get(athleteId) : 0;
        double maxHeartRate = athleteMaxHeartRate.containsKey(athleteId) ?
                athleteMaxHeartRate.get(athleteId) : 0;
        double avgSpeed = athleteAvgSpeed.containsKey(athleteId) ?
                athleteAvgSpeed.get(athleteId) : 0;
        double maxSpeed = athleteMaxSpeed.containsKey(athleteId) ?
                athleteMaxSpeed.get(athleteId) : 0;

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

    private void updateCharts() {
        // Update performance chart
        updatePerformanceChart();

        // Update heart rate chart
        updateHeartRateChart();

        // Update speed chart
        updateSpeedChart();
    }

    private void updatePerformanceChart() {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        // Get top performers sorted by score
        List<Map.Entry<String, Double>> sortedScores = new ArrayList<>(athletePerformanceScores.entrySet());
        Collections.sort(sortedScores, Map.Entry.comparingByValue(Comparator.reverseOrder()));

        // Limit to top 10 athletes
        int maxAthletes = Math.min(10, sortedScores.size());

        for (int i = 0; i < maxAthletes; i++) {
            Map.Entry<String, Double> entry = sortedScores.get(i);
            String athleteId = entry.getKey();
            double score = entry.getValue();

            // Find athlete name
            String athleteName = "Unknown";
            User athlete = athleteMap.get(athleteId);
            if (athlete != null) {
                athleteName = athlete.getName();
            }

            entries.add(new BarEntry(i, (float) score));
            labels.add(athleteName);
        }

        if (entries.isEmpty()) {
            return;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Performance Score");
        dataSet.setColor(getResources().getColor(R.color.accent_color));
        dataSet.setValueTextSize(10f);

        BarData data = new BarData(dataSet);
        // Make bars thinner
        data.setBarWidth(0.5f);

        // Set value text properties
        data.setValueTextSize(10f);
        data.setValueTextColor(Color.BLACK);

        performanceChart.setData(data);

        XAxis xAxis = performanceChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelRotationAngle(45);
        xAxis.setLabelCount(labels.size());

        // Force labels to show for each bar
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);

        performanceChart.notifyDataSetChanged();
        performanceChart.invalidate();
    }

    private void updateHeartRateChart() {
        if (athleteAvgHeartRate.isEmpty() || athleteMaxHeartRate.isEmpty()) {
            heartRateChart.setNoDataText("No heart rate data available");
            heartRateChart.invalidate();
            return;
        }

        List<BarEntry> avgEntries = new ArrayList<>();
        List<BarEntry> maxEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        // Debug to check all values
        Log.d(TAG, "--------------------");
        Log.d(TAG, "ALL HEART RATE VALUES FOR CHART:");
        for (Map.Entry<String, Double> entry : athleteAvgHeartRate.entrySet()) {
            String athleteId = entry.getKey();
            Double avgValue = entry.getValue();
            Double maxValue = athleteMaxHeartRate.get(athleteId);
            User athlete = athleteMap.get(athleteId);
            String name = athlete != null ? athlete.getName() : "Unknown";

            Log.d(TAG, name + " - Avg HR: " + avgValue + ", Max HR: " + maxValue);
        }
        Log.d(TAG, "--------------------");

        // Get athletes
        List<String> athleteIds = new ArrayList<>(athleteAvgHeartRate.keySet());
        int limit = Math.min(10, athleteIds.size());

        for (int i = 0; i < limit; i++) {
            String athleteId = athleteIds.get(i);
            User athlete = athleteMap.get(athleteId);

            if (athlete != null) {
                // Get values from separate maps with explicit type conversion
                Double avgHRValue = athleteAvgHeartRate.get(athleteId);
                Double maxHRValue = athleteMaxHeartRate.get(athleteId);

                if (avgHRValue != null && maxHRValue != null) {
                    float avgHR = avgHRValue.floatValue();
                    float maxHR = maxHRValue.floatValue();

                    // Create separate entries for each
                    BarEntry avgEntry = new BarEntry(i, avgHR);
                    BarEntry maxEntry = new BarEntry(i, maxHR);

                    // Log for debugging
                    Log.d(TAG, "Adding chart HR for " + athlete.getName() +
                            ": Avg=" + avgHR + " (Entry value=" + avgEntry.getY() + ")" +
                            ", Max=" + maxHR + " (Entry value=" + maxEntry.getY() + ")");

                    avgEntries.add(avgEntry);
                    maxEntries.add(maxEntry);
                    labels.add(getShortenedName(athlete.getName()));
                }
            }
        }

        if (avgEntries.isEmpty() || maxEntries.isEmpty()) {
            Log.w(TAG, "No valid entries for heart rate chart");
            return;
        }

        // Create datasets with DISTINCT colors and labels
        BarDataSet avgDataSet = new BarDataSet(avgEntries, "Average Heart Rate");
        avgDataSet.setColor(Color.BLUE);
        avgDataSet.setValueTextColor(Color.BLACK);
        avgDataSet.setValueTextSize(9f);

        BarDataSet maxDataSet = new BarDataSet(maxEntries, "Maximum Heart Rate");
        maxDataSet.setColor(Color.RED);
        maxDataSet.setValueTextColor(Color.BLACK);
        maxDataSet.setValueTextSize(9f);

        // Group bars
        float groupSpace = 0.3f;
        float barSpace = 0.05f;
        float barWidth = 0.3f;

        BarData data = new BarData(avgDataSet, maxDataSet);
        data.setBarWidth(barWidth);

        // Configure and update chart
        heartRateChart.setData(data);
        heartRateChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        heartRateChart.groupBars(0, groupSpace, barSpace);

        // Ensure proper scaling
        heartRateChart.getXAxis().setAxisMinimum(0);
        heartRateChart.getXAxis().setAxisMaximum(0 + heartRateChart.getBarData().getGroupWidth(groupSpace, barSpace) * limit);

        // Force re-draw
        heartRateChart.notifyDataSetChanged();
        heartRateChart.invalidate();
    }

    private void updateSpeedChart() {
        if (athleteAvgSpeed.isEmpty() || athleteMaxSpeed.isEmpty()) {
            speedChart.setNoDataText("No speed data available");
            speedChart.invalidate();
            return;
        }

        List<BarEntry> avgEntries = new ArrayList<>();
        List<BarEntry> maxEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        // Debug to check all values
        Log.d(TAG, "--------------------");
        Log.d(TAG, "ALL SPEED VALUES FOR CHART:");
        for (Map.Entry<String, Double> entry : athleteAvgSpeed.entrySet()) {
            String athleteId = entry.getKey();
            Double avgValue = entry.getValue();
            Double maxValue = athleteMaxSpeed.get(athleteId);
            User athlete = athleteMap.get(athleteId);
            String name = athlete != null ? athlete.getName() : "Unknown";

            Log.d(TAG, name + " - Avg Speed: " + avgValue + ", Max Speed: " + maxValue);
        }
        Log.d(TAG, "--------------------");

        // Get athletes
        List<String> athleteIds = new ArrayList<>(athleteAvgSpeed.keySet());
        int limit = Math.min(10, athleteIds.size());

        for (int i = 0; i < limit; i++) {
            String athleteId = athleteIds.get(i);
            User athlete = athleteMap.get(athleteId);

            if (athlete != null) {
                // Get values from separate maps with explicit type conversion
                Double avgSpeedValue = athleteAvgSpeed.get(athleteId);
                Double maxSpeedValue = athleteMaxSpeed.get(athleteId);

                if (avgSpeedValue != null && maxSpeedValue != null) {
                    float avgSpeed = avgSpeedValue.floatValue();
                    float maxSpeed = maxSpeedValue.floatValue();

                    // Create separate entries for each
                    BarEntry avgEntry = new BarEntry(i, avgSpeed);
                    BarEntry maxEntry = new BarEntry(i, maxSpeed);

                    // Log for debugging
                    Log.d(TAG, "Adding chart speed for " + athlete.getName() +
                            ": Avg=" + avgSpeed + " (Entry value=" + avgEntry.getY() + ")" +
                            ", Max=" + maxSpeed + " (Entry value=" + maxEntry.getY() + ")");

                    avgEntries.add(avgEntry);
                    maxEntries.add(maxEntry);
                    labels.add(getShortenedName(athlete.getName()));
                }
            }
        }

        if (avgEntries.isEmpty() || maxEntries.isEmpty()) {
            Log.w(TAG, "No valid entries for speed chart");
            return;
        }

        // Create datasets with VERY DISTINCT colors and labels
        BarDataSet avgDataSet = new BarDataSet(avgEntries, "Average Speed");
        avgDataSet.setColor(Color.GREEN);
        avgDataSet.setValueTextColor(Color.BLACK);
        avgDataSet.setValueTextSize(9f);

        BarDataSet maxDataSet = new BarDataSet(maxEntries, "Maximum Speed");
        maxDataSet.setColor(Color.CYAN);
        maxDataSet.setValueTextColor(Color.BLACK);
        maxDataSet.setValueTextSize(9f);

        // Group bars
        float groupSpace = 0.3f;
        float barSpace = 0.05f;
        float barWidth = 0.3f;

        BarData data = new BarData(avgDataSet, maxDataSet);
        data.setBarWidth(barWidth);

        // Configure and update chart
        speedChart.setData(data);
        speedChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        speedChart.groupBars(0, groupSpace, barSpace);

        // Ensure proper scaling
        speedChart.getXAxis().setAxisMinimum(0);
        speedChart.getXAxis().setAxisMaximum(0 + speedChart.getBarData().getGroupWidth(groupSpace, barSpace) * limit);

        // Force re-draw
        speedChart.notifyDataSetChanged();
        speedChart.invalidate();
    }

    // Helper method to shorten names for better display in charts
    private String getShortenedName(String name) {
        if (name == null || name.isEmpty()) {
            return "Unknown";
        }

        // Get first name only
        String[] parts = name.split("\\s+");
        if (parts.length > 0) {
            // Use first name if it's short enough
            if (parts[0].length() <= 7) {
                return parts[0];
            } else {
                // Truncate long first names
                return parts[0].substring(0, 6) + ".";
            }
        }

        // Fallback if can't split or empty
        if (name.length() > 7) {
            return name.substring(0, 6) + ".";
        }
        return name;
    }

    private void showNoData(boolean show) {
        if (show) {
            noDataText.setVisibility(View.VISIBLE);
            performanceChart.setVisibility(View.GONE);
            heartRateChart.setVisibility(View.GONE);
            speedChart.setVisibility(View.GONE);
        } else {
            noDataText.setVisibility(View.GONE);
            performanceChart.setVisibility(View.VISIBLE);
            heartRateChart.setVisibility(View.VISIBLE);
            speedChart.setVisibility(View.VISIBLE);
        }
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}