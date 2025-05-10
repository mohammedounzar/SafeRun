package com.example.saferun.ui.athlete;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.saferun.R;
import com.example.saferun.data.model.RunSession;
import com.example.saferun.data.model.SensorData;
import com.example.saferun.data.repository.RunSessionRepository;
import com.example.saferun.data.repository.SensorDataRepository;
import com.example.saferun.data.repository.UserRepository;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AthleteGlobalPerformanceActivity extends AppCompatActivity {
    private static final String TAG = "AthleteGlobalPerf";

    private Toolbar toolbar;
    private TextView totalSessionsText;
    private TextView totalDistanceText;
    private TextView totalDurationText;
    private TextView noDataText;
    private LineChart performanceChart;
    private LineChart heartRateChart;
    private LineChart speedChart;
    private ProgressBar progressBar;

    private RunSessionRepository runSessionRepository;
    private SensorDataRepository sensorDataRepository;
    private UserRepository userRepository;

    private List<RunSession> sessions = new ArrayList<>();
    private Map<String, List<SensorData>> sessionSensorData = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_athlete_global_performance);

        // Initialize repositories
        runSessionRepository = RunSessionRepository.getInstance();
        sensorDataRepository = SensorDataRepository.getInstance();
        userRepository = UserRepository.getInstance();

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Setup charts
        setupCharts();

        // Load data
        loadData();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        totalSessionsText = findViewById(R.id.total_sessions);
        totalDistanceText = findViewById(R.id.total_distance);
        totalDurationText = findViewById(R.id.total_duration);
        noDataText = findViewById(R.id.no_data_text);
        performanceChart = findViewById(R.id.performance_chart);
        heartRateChart = findViewById(R.id.heart_rate_chart);
        speedChart = findViewById(R.id.speed_chart);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("My Global Performance");

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupCharts() {
        // Setup performance trend chart
        setupPerformanceChart();

        // Setup heart rate chart
        setupHeartRateChart();

        // Setup speed chart
        setupSpeedChart();
    }

    private void setupPerformanceChart() {
        // Configure the performance chart (progress over time)
        performanceChart.getDescription().setEnabled(false);
        performanceChart.setTouchEnabled(true);
        performanceChart.setDragEnabled(true);
        performanceChart.setScaleEnabled(true);
        performanceChart.setPinchZoom(true);
        performanceChart.setDrawGridBackground(false);

        // X-axis configuration
        XAxis xAxis = performanceChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(45);

        // Left Y-axis (performance score)
        YAxis leftAxis = performanceChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(100f);
        leftAxis.setTextColor(Color.BLACK);

        // Disable right axis
        performanceChart.getAxisRight().setEnabled(false);

        // Configure legend
        Legend legend = performanceChart.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
    }

    private void setupHeartRateChart() {
        // Configure the heart rate chart
        heartRateChart.getDescription().setEnabled(false);
        heartRateChart.setTouchEnabled(true);
        heartRateChart.setDragEnabled(true);
        heartRateChart.setScaleEnabled(true);
        heartRateChart.setPinchZoom(true);
        heartRateChart.setDrawGridBackground(false);

        // X-axis configuration
        XAxis xAxis = heartRateChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(45);

        // Left Y-axis (heart rate values)
        YAxis leftAxis = heartRateChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(200f);
        leftAxis.setTextColor(Color.BLACK);

        // Disable right axis
        heartRateChart.getAxisRight().setEnabled(false);

        // Configure legend
        Legend legend = heartRateChart.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
    }

    private void setupSpeedChart() {
        // Configure the speed chart
        speedChart.getDescription().setEnabled(false);
        speedChart.setTouchEnabled(true);
        speedChart.setDragEnabled(true);
        speedChart.setScaleEnabled(true);
        speedChart.setPinchZoom(true);
        speedChart.setDrawGridBackground(false);

        // X-axis configuration
        XAxis xAxis = speedChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(45);

        // Left Y-axis (speed values)
        YAxis leftAxis = speedChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(20f);
        leftAxis.setTextColor(Color.BLACK);

        // Disable right axis
        speedChart.getAxisRight().setEnabled(false);

        // Configure legend
        Legend legend = speedChart.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
    }

    private void loadData() {
        showProgress(true);
        String athleteId = userRepository.getCurrentUserId();

        if (athleteId == null) {
            showProgress(false);
            showNoData(true);
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Load all sessions for the athlete
        runSessionRepository.getRunSessionsByAthlete(athleteId, new RunSessionRepository.RunSessionsCallback() {
            @Override
            public void onSuccess(List<RunSession> loadedSessions) {
                if (loadedSessions == null || loadedSessions.isEmpty()) {
                    showProgress(false);
                    showNoData(true);
                    Log.d(TAG, "No sessions found for athlete " + athleteId);
                    return;
                }

                sessions.clear();
                sessions.addAll(loadedSessions);
                Log.d(TAG, "Loaded " + sessions.size() + " sessions");

                // Load sensor data for each session
                loadSensorDataForSessions();
            }

            @Override
            public void onError(String errorMessage) {
                showProgress(false);
                showNoData(true);
                Toast.makeText(AthleteGlobalPerformanceActivity.this,
                        "Error loading sessions: " + errorMessage, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading sessions: " + errorMessage);
            }
        });
    }

    private void loadSensorDataForSessions() {
        final int[] loadedCount = {0};
        final int totalSessions = sessions.size();

        if (totalSessions == 0) {
            showProgress(false);
            showNoData(true);
            return;
        }

        // Clear previous data
        sessionSensorData.clear();

        // Process each session
        for (RunSession session : sessions) {
            String sessionId = session.getId();
            Log.d(TAG, "Loading sensor data for session: " + sessionId);

            sensorDataRepository.getSensorDataHistory(sessionId, userRepository.getCurrentUserId(), 1000,
                    new SensorDataRepository.SensorDataListCallback() {
                        @Override
                        public void onSuccess(List<SensorData> sensorDataList) {
                            if (sensorDataList != null && !sensorDataList.isEmpty()) {
                                sessionSensorData.put(sessionId, sensorDataList);
                                Log.d(TAG, "Loaded " + sensorDataList.size() + " data points for session " + sessionId);
                            }

                            loadedCount[0]++;
                            checkAllDataLoaded(loadedCount[0], totalSessions);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Log.e(TAG, "Error loading sensor data for session " + sessionId + ": " + errorMessage);
                            loadedCount[0]++;
                            checkAllDataLoaded(loadedCount[0], totalSessions);
                        }
                    });
        }
    }

    private void checkAllDataLoaded(int loadedCount, int totalSessions) {
        if (loadedCount >= totalSessions) {
            // All sessions processed
            showProgress(false);

            if (isAllSensorDataEmpty()) {
                showNoData(true);
                Log.d(TAG, "No sensor data found for any session");
            } else {
                showNoData(false);
                updateStatistics();
                updateCharts();
            }
        }
    }

    private boolean isAllSensorDataEmpty() {
        for (List<SensorData> dataList : sessionSensorData.values()) {
            if (dataList != null && !dataList.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void updateStatistics() {
        // Calculate total sessions, distance, and duration
        int totalSessionsCount = sessions.size();
        double totalDistanceValue = 0;
        long totalDurationValue = 0;

        for (RunSession session : sessions) {
            totalDistanceValue += session.getDistance();
            totalDurationValue += session.getDuration();
        }

        // Update UI
        totalSessionsText.setText(String.valueOf(totalSessionsCount));
        totalDistanceText.setText(String.format(Locale.getDefault(), "%.1f km", totalDistanceValue));
        totalDurationText.setText(String.format(Locale.getDefault(), "%d min", totalDurationValue));

        Log.d(TAG, "Statistics updated: " + totalSessionsCount + " sessions, " +
                totalDistanceValue + " km, " + totalDurationValue + " min");
    }

    private void updateCharts() {
        // Update the performance trend chart
        updatePerformanceChart();

        // Update the heart rate chart
        updateHeartRateChart();

        // Update the speed chart
        updateSpeedChart();
    }

    private void updatePerformanceChart() {
        // Calculate session scores and prepare data for chart
        List<Entry> performanceEntries = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();

        // Limit to last 8 sessions to avoid overcrowding
        int sessionLimit = Math.min(sessions.size(), 8);
        int index = 0;

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());

        // We'll use the most recent sessions for display
        for (int i = Math.max(0, sessions.size() - sessionLimit); i < sessions.size(); i++) {
            RunSession session = sessions.get(i);

            // Calculate performance score for this session
            double score = calculateSessionPerformance(session);

            // Add data point
            performanceEntries.add(new Entry(index, (float)score));

            // Add label (date or session name)
            String label = session.getDate() != null ?
                    dateFormat.format(session.getDate()) :
                    abbreviateSessionTitle(session.getTitle());
            xLabels.add(label);

            index++;
        }

        // Create data set if we have data
        if (!performanceEntries.isEmpty()) {
            LineDataSet dataSet = new LineDataSet(performanceEntries, "Performance Score");
            dataSet.setColor(Color.rgb(65, 105, 225)); // Royal Blue
            dataSet.setCircleColor(Color.rgb(65, 105, 225));
            dataSet.setLineWidth(2f);
            dataSet.setCircleRadius(4f);
            dataSet.setDrawValues(true);
            dataSet.setValueTextSize(10f);
            dataSet.setValueTextColor(Color.BLACK);
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

            // Set X-axis labels
            performanceChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabels));

            // Set data and refresh
            LineData lineData = new LineData(dataSet);
            performanceChart.setData(lineData);
            performanceChart.invalidate();

            Log.d(TAG, "Performance chart updated with " + performanceEntries.size() + " data points");
        } else {
            performanceChart.setNoDataText("No performance data available");
            performanceChart.invalidate();
        }
    }

    private double calculateSessionPerformance(RunSession session) {
        // Get sensor data for this session
        List<SensorData> dataList = sessionSensorData.get(session.getId());
        if (dataList == null || dataList.isEmpty()) {
            return 50.0; // Default score if no data
        }

        // Calculate metrics from sensor data
        double avgHeartRate = 0;
        double maxHeartRate = 0;
        int heartRateCount = 0;

        double avgSpeed = 0;
        double maxSpeed = 0;
        int speedCount = 0;

        for (SensorData data : dataList) {
            // Process heart rate
            if (data.getHeartRate() > 0) {
                avgHeartRate += data.getHeartRate();
                heartRateCount++;
                maxHeartRate = Math.max(maxHeartRate, data.getHeartRate());
            }

            // Process speed
            if (data.getSpeed() > 0) {
                avgSpeed += data.getSpeed();
                speedCount++;
                maxSpeed = Math.max(maxSpeed, data.getSpeed());
            }
        }

        // Calculate averages
        if (heartRateCount > 0) {
            avgHeartRate /= heartRateCount;
        }

        if (speedCount > 0) {
            avgSpeed /= speedCount;
        }

        // Calculate performance score (simplified algorithm)
        double heartRateEfficiency = 0;
        if (avgHeartRate > 0) {
            // Better heart rate efficiency = lower heart rate for same output
            // Assuming target zone is 120-150 bpm
            if (avgHeartRate < 120) {
                heartRateEfficiency = 100; // Very efficient
            } else if (avgHeartRate <= 150) {
                heartRateEfficiency = 90 - ((avgHeartRate - 120) / 30) * 10; // 90-80 range
            } else if (avgHeartRate <= 170) {
                heartRateEfficiency = 80 - ((avgHeartRate - 150) / 20) * 20; // 80-60 range
            } else {
                heartRateEfficiency = 60 - ((avgHeartRate - 170) / 30) * 30; // 60-30 range
            }
        }

        // Speed performance (higher is better)
        double speedPerformance = 0;
        if (avgSpeed > 0) {
            // Using km/h: Jogging ~8-10 km/h, Running ~10-15 km/h
            speedPerformance = Math.min(100, avgSpeed * 8); // 10 km/h = 80 points
        }

        // Combine for final score (weighted)
        double score = (heartRateEfficiency * 0.6) + (speedPerformance * 0.4);

        // Ensure score is in 0-100 range
        return Math.min(100, Math.max(0, score));
    }

    private void updateHeartRateChart() {
        // Calculate average and max heart rates per session
        List<Entry> avgHeartRateEntries = new ArrayList<>();
        List<Entry> maxHeartRateEntries = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();

        // Limit to last 8 sessions
        int sessionLimit = Math.min(sessions.size(), 8);
        int index = 0;

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());

        // Use most recent sessions
        for (int i = Math.max(0, sessions.size() - sessionLimit); i < sessions.size(); i++) {
            RunSession session = sessions.get(i);
            List<SensorData> dataList = sessionSensorData.get(session.getId());

            if (dataList != null && !dataList.isEmpty()) {
                // Calculate average and max heart rate
                double avgHeartRate = 0;
                int maxHeartRate = 0;
                int count = 0;

                for (SensorData data : dataList) {
                    if (data.getHeartRate() > 0) {
                        avgHeartRate += data.getHeartRate();
                        count++;
                        maxHeartRate = Math.max(maxHeartRate, data.getHeartRate());
                    }
                }

                if (count > 0) {
                    avgHeartRate /= count;

                    // Add data points
                    avgHeartRateEntries.add(new Entry(index, (float)avgHeartRate));
                    maxHeartRateEntries.add(new Entry(index, (float)maxHeartRate));

                    // Add label
                    String label = session.getDate() != null ?
                            dateFormat.format(session.getDate()) :
                            abbreviateSessionTitle(session.getTitle());
                    xLabels.add(label);

                    index++;
                }
            }
        }

        // Create data sets if we have data
        if (!avgHeartRateEntries.isEmpty()) {
            LineDataSet avgDataSet = new LineDataSet(avgHeartRateEntries, "Avg Heart Rate");
            avgDataSet.setColor(Color.rgb(220, 20, 60)); // Crimson
            avgDataSet.setCircleColor(Color.rgb(220, 20, 60));
            avgDataSet.setLineWidth(2f);
            avgDataSet.setCircleRadius(4f);
            avgDataSet.setDrawValues(true);
            avgDataSet.setValueTextSize(10f);

            LineDataSet maxDataSet = new LineDataSet(maxHeartRateEntries, "Max Heart Rate");
            maxDataSet.setColor(Color.rgb(255, 140, 0)); // Dark Orange
            maxDataSet.setCircleColor(Color.rgb(255, 140, 0));
            maxDataSet.setLineWidth(2f);
            maxDataSet.setCircleRadius(4f);
            maxDataSet.setDrawValues(true);
            maxDataSet.setValueTextSize(10f);

            // Set X-axis labels
            heartRateChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabels));

            // Set data and refresh
            LineData lineData = new LineData(avgDataSet, maxDataSet);
            heartRateChart.setData(lineData);
            heartRateChart.invalidate();

            Log.d(TAG, "Heart rate chart updated with " + avgHeartRateEntries.size() + " data points");
        } else {
            heartRateChart.setNoDataText("No heart rate data available");
            heartRateChart.invalidate();
        }
    }

    private void updateSpeedChart() {
        // Calculate average and max speeds per session
        List<Entry> avgSpeedEntries = new ArrayList<>();
        List<Entry> maxSpeedEntries = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();

        // Limit to last 8 sessions
        int sessionLimit = Math.min(sessions.size(), 8);
        int index = 0;

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());

        // Use most recent sessions
        for (int i = Math.max(0, sessions.size() - sessionLimit); i < sessions.size(); i++) {
            RunSession session = sessions.get(i);
            List<SensorData> dataList = sessionSensorData.get(session.getId());

            if (dataList != null && !dataList.isEmpty()) {
                // Calculate average and max speed
                double avgSpeed = 0;
                double maxSpeed = 0;
                int count = 0;

                for (SensorData data : dataList) {
                    if (data.getSpeed() > 0) {
                        avgSpeed += data.getSpeed();
                        count++;
                        maxSpeed = Math.max(maxSpeed, data.getSpeed());
                    }
                }

                if (count > 0) {
                    avgSpeed /= count;

                    // Add data points
                    avgSpeedEntries.add(new Entry(index, (float)avgSpeed));
                    maxSpeedEntries.add(new Entry(index, (float)maxSpeed));

                    // Add label
                    String label = session.getDate() != null ?
                            dateFormat.format(session.getDate()) :
                            abbreviateSessionTitle(session.getTitle());
                    xLabels.add(label);

                    index++;
                }
            }
        }

        // Create data sets if we have data
        if (!avgSpeedEntries.isEmpty()) {
            LineDataSet avgDataSet = new LineDataSet(avgSpeedEntries, "Avg Speed");
            avgDataSet.setColor(Color.rgb(34, 139, 34)); // Forest Green
            avgDataSet.setCircleColor(Color.rgb(34, 139, 34));
            avgDataSet.setLineWidth(2f);
            avgDataSet.setCircleRadius(4f);
            avgDataSet.setDrawValues(true);
            avgDataSet.setValueTextSize(10f);

            LineDataSet maxDataSet = new LineDataSet(maxSpeedEntries, "Max Speed");
            maxDataSet.setColor(Color.rgb(0, 100, 0)); // Dark Green
            maxDataSet.setCircleColor(Color.rgb(0, 100, 0));
            maxDataSet.setLineWidth(2f);
            maxDataSet.setCircleRadius(4f);
            maxDataSet.setDrawValues(true);
            maxDataSet.setValueTextSize(10f);

            // Set X-axis labels
            speedChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabels));

            // Set data and refresh
            LineData lineData = new LineData(avgDataSet, maxDataSet);
            speedChart.setData(lineData);
            speedChart.invalidate();

            Log.d(TAG, "Speed chart updated with " + avgSpeedEntries.size() + " data points");
        } else {
            speedChart.setNoDataText("No speed data available");
            speedChart.invalidate();
        }
    }

    private String abbreviateSessionTitle(String title) {
        if (title == null || title.isEmpty()) {
            return "Session";
        }

        // If title is already short, return as is
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

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
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
}