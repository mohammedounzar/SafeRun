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
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

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
    private List<RunSession> allSessions = new ArrayList<>();
    private Map<String, List<SensorData>> athleteSensorData = new HashMap<>();
    private Map<String, Double> athletePerformanceScores = new HashMap<>();

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
                athletes.addAll(users);

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

    private double calculatePerformanceScore(String athleteId, List<SensorData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return 0;
        }

        double avgHeartRate = 0;
        int maxHeartRate = 0;
        double avgSpeed = 0;
        double maxSpeed = 0;
        int count = 0;

        for (SensorData data : dataList) {
            if (data.getHeartRate() > 0) {
                avgHeartRate += data.getHeartRate();
                maxHeartRate = Math.max(maxHeartRate, data.getHeartRate());
                avgSpeed += data.getSpeed();
                maxSpeed = Math.max(maxSpeed, data.getSpeed());
                count++;
            }
        }

        if (count > 0) {
            avgHeartRate /= count;
            avgSpeed /= count;
        }

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
            for (User athlete : athletes) {
                if (athlete.getUid().equals(athleteId)) {
                    athleteName = athlete.getName();
                    break;
                }
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
        List<BarEntry> avgEntries = new ArrayList<>();
        List<BarEntry> maxEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        int athleteIndex = 0;

        for (User athlete : athletes) {
            String athleteId = athlete.getUid();
            List<SensorData> dataList = athleteSensorData.get(athleteId);

            if (dataList != null && !dataList.isEmpty()) {
                double avgHeartRate = 0;
                int maxHeartRate = 0;
                int count = 0;

                for (SensorData data : dataList) {
                    if (data.getHeartRate() > 0) {
                        avgHeartRate += data.getHeartRate();
                        maxHeartRate = Math.max(maxHeartRate, data.getHeartRate());
                        count++;
                    }
                }

                if (count > 0) {
                    avgHeartRate /= count;

                    avgEntries.add(new BarEntry(athleteIndex, (float) avgHeartRate));
                    maxEntries.add(new BarEntry(athleteIndex, maxHeartRate));
                    // Use shorter name display (first name or first 7 chars)
                    String shortName = getShortenedName(athlete.getName());
                    labels.add(shortName);

                    athleteIndex++;
                }
            }
        }

        if (avgEntries.isEmpty()) {
            return;
        }

        BarDataSet avgDataSet = new BarDataSet(avgEntries, "Average Heart Rate");
        avgDataSet.setColor(Color.BLUE);

        BarDataSet maxDataSet = new BarDataSet(maxEntries, "Max Heart Rate");
        maxDataSet.setColor(Color.RED);

        BarData data = new BarData(avgDataSet, maxDataSet);
        data.setValueTextSize(8f); // Smaller text values

        // Set bar width - make them thinner
        float groupSpace = 0.25f; // Increase space between groups
        float barSpace = 0.05f;   // Space between bars in group
        float barWidth = 0.25f;   // Reduced from 0.4f to make bars thinner

        data.setBarWidth(barWidth);

        heartRateChart.setData(data);

        XAxis xAxis = heartRateChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelRotationAngle(45);
        xAxis.setLabelCount(labels.size());
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);

        // Adjust X axis to fit all bars
        heartRateChart.groupBars(0, groupSpace, barSpace);
        heartRateChart.setFitBars(true);

        heartRateChart.notifyDataSetChanged();
        heartRateChart.invalidate();
    }

    private void updateSpeedChart() {
        List<BarEntry> avgEntries = new ArrayList<>();
        List<BarEntry> maxEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        int athleteIndex = 0;

        for (User athlete : athletes) {
            String athleteId = athlete.getUid();
            List<SensorData> dataList = athleteSensorData.get(athleteId);

            if (dataList != null && !dataList.isEmpty()) {
                double avgSpeed = 0;
                double maxSpeed = 0;
                int count = 0;

                for (SensorData data : dataList) {
                    avgSpeed += data.getSpeed();
                    maxSpeed = Math.max(maxSpeed, data.getSpeed());
                    count++;
                }

                if (count > 0) {
                    avgSpeed /= count;

                    avgEntries.add(new BarEntry(athleteIndex, (float) avgSpeed));
                    maxEntries.add(new BarEntry(athleteIndex, (float) maxSpeed));
                    // Use shorter name display
                    String shortName = getShortenedName(athlete.getName());
                    labels.add(shortName);

                    athleteIndex++;
                }
            }
        }

        if (avgEntries.isEmpty()) {
            return;
        }

        BarDataSet avgDataSet = new BarDataSet(avgEntries, "Average Speed");
        avgDataSet.setColor(Color.GREEN);

        BarDataSet maxDataSet = new BarDataSet(maxEntries, "Max Speed");
        maxDataSet.setColor(Color.CYAN);

        BarData data = new BarData(avgDataSet, maxDataSet);
        data.setValueTextSize(8f); // Smaller text values

        // Set bar width - make them thinner
        float groupSpace = 0.25f; // Increase space between groups
        float barSpace = 0.05f;   // Space between bars in group
        float barWidth = 0.25f;   // Reduced from 0.4f to make bars thinner

        data.setBarWidth(barWidth);

        speedChart.setData(data);

        XAxis xAxis = speedChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelRotationAngle(45);
        xAxis.setLabelCount(labels.size());
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);

        // Adjust X axis to fit all bars
        speedChart.groupBars(0, groupSpace, barSpace);
        speedChart.setFitBars(true);

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