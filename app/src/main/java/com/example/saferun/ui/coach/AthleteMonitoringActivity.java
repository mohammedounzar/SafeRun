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
import com.example.saferun.data.model.SensorData;
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
    private TextView alertSourceTextView; // New field to show if ML detected the anomaly
    private Button dismissAlertButton;
    private Button contactAthleteButton; // New button to contact athlete in case of emergency
    private ImageButton backButton;
    private ProgressBar progressBar;

    private LineChart heartRateSpeedChart;
    private LineChart temperatureChart;
    private List<Entry> heartRateEntries;
    private List<Entry> speedEntries;
    private List<Entry> temperatureEntries;
    private SimpleDateFormat timeFormat;

    private SensorDataRepository sensorDataRepository;

    private String sessionId;
    private String athleteId;
    private String athleteName;

    private Timer dataRefreshTimer;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int alertsCount = 0;
    private SensorData lastSensorData; // To keep track of the last reading

    // Flag to track if athlete has been contacted for current anomaly
    private boolean athleteContacted = false;

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

        // Get data from intent
        sessionId = getIntent().getStringExtra(EXTRA_SESSION_ID);
        athleteId = getIntent().getStringExtra(EXTRA_ATHLETE_ID);
        athleteName = getIntent().getStringExtra(EXTRA_ATHLETE_NAME);

        if (sessionId == null || athleteId == null) {
            Toast.makeText(this, "Error: Missing required data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize repository
        sensorDataRepository = SensorDataRepository.getInstance();

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Initialize charts
        initCharts();

        // Start data refresh
        startDataRefresh();
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

        // Set up additional views if they exist in your layout
        try {
//            alertSourceTextView = findViewById(R.id.alert_source);
            contactAthleteButton = findViewById(R.id.contact_athlete_button);

            // Set up contact button if it exists
            if (contactAthleteButton != null) {
                contactAthleteButton.setOnClickListener(v -> contactAthlete());
            }
        } catch (Exception e) {
            Log.w(TAG, "Some new UI elements not found in layout. Using fallback layout.");
        }

        dismissAlertButton = findViewById(R.id.dismiss_alert_button);
        backButton = findViewById(R.id.back_button);
        progressBar = findViewById(R.id.progress_bar);

        heartRateSpeedChart = findViewById(R.id.heart_rate_speed_chart);
        temperatureChart = findViewById(R.id.temperature_chart);
        heartRateEntries = new ArrayList<>();
        speedEntries = new ArrayList<>();
        temperatureEntries = new ArrayList<>();
        timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        // Set initial values
        athleteNameTextView.setText(athleteName);
        alertCardView.setVisibility(View.GONE);
        setAlertsCount(0);

        // Set click listeners
        dismissAlertButton.setOnClickListener(v -> dismissAlert());
        backButton.setOnClickListener(v -> onBackPressed());
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    private void initCharts() {
        // Setup heart rate and speed chart
        setupHeartRateSpeedChart();

        // Setup temperature chart
        setupTemperatureChart();
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

        // Setup left Y axis (heart rate)
        YAxis leftAxis = heartRateSpeedChart.getAxisLeft();
        leftAxis.setTextColor(Color.RED);
        leftAxis.setAxisMinimum(40f);
        leftAxis.setAxisMaximum(200f);
        leftAxis.setDrawGridLines(false);

        // Setup right Y axis (speed)
        YAxis rightAxis = heartRateSpeedChart.getAxisRight();
        rightAxis.setTextColor(Color.BLUE);
        rightAxis.setAxisMinimum(0f);
        rightAxis.setAxisMaximum(25f);
        rightAxis.setDrawGridLines(false);

        // Setup legend
        Legend legend = heartRateSpeedChart.getLegend();
        legend.setForm(Legend.LegendForm.LINE);

        // Create empty data
        LineData data = new LineData();
        data.setValueTextColor(Color.BLACK);
        heartRateSpeedChart.setData(data);
    }

    private void setupTemperatureChart() {
        // Similar setup for temperature chart
        temperatureChart.getDescription().setEnabled(false);
        temperatureChart.setTouchEnabled(true);
        temperatureChart.setDragEnabled(true);
        temperatureChart.setScaleEnabled(true);
        temperatureChart.setPinchZoom(true);
        temperatureChart.setDrawGridBackground(false);
        temperatureChart.setBackgroundColor(Color.WHITE);

        // Setup X axis (time)
        XAxis xAxis = temperatureChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return timeFormat.format(new Date((long) value));
            }
        });

        // Setup Y axis (temperature)
        YAxis leftAxis = temperatureChart.getAxisLeft();
        leftAxis.setTextColor(Color.GREEN);
        leftAxis.setAxisMinimum(34f); // Minimum temperature
        leftAxis.setAxisMaximum(42f); // Maximum temperature
        leftAxis.setDrawGridLines(false);

        // Disable right axis
        temperatureChart.getAxisRight().setEnabled(false);

        // Create empty data
        LineData data = new LineData();
        data.setValueTextColor(Color.BLACK);
        temperatureChart.setData(data);
    }

    private void startDataRefresh() {
        // Cancel any existing timer
        if (dataRefreshTimer != null) {
            dataRefreshTimer.cancel();
        }

        // Create new timer that refreshes data every 2 seconds
        dataRefreshTimer = new Timer();
        dataRefreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                refreshData();
            }
        }, 0, 2000); // Initial delay 0, period 2 seconds
    }

    private void refreshData() {
        // This will run on a background thread, so we need to use the main handler to update UI
        sensorDataRepository.getLatestSensorData(sessionId, athleteId, new SensorDataRepository.SensorDataCallback() {
            @Override
            public void onSuccess(SensorData sensorData) {
                lastSensorData = sensorData; // Store for reference
                mainHandler.post(() -> updateUI(sensorData));
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
        if (sensorData == null) {
            return;
        }

        // Update current values
        heartRateTextView.setText(String.format("%d bpm", sensorData.getHeartRate()));
        temperatureTextView.setText(String.format("%.1f°C", sensorData.getTemperature()));
        speedTextView.setText(String.format("%.1f km/h", sensorData.getSpeed()));

        // Update heart rate and speed chart
        updateHeartRateSpeedChart(sensorData);

        // Update temperature chart
        updateTemperatureChart(sensorData);

        // Check for anomalies
        if (sensorData.isAnomalyDetected()) {
            showAlert(sensorData);
        }
    }

    private void updateHeartRateSpeedChart(SensorData sensorData) {
        long timestamp = sensorData.getTimestamp();

        // Add heart rate entry
        heartRateEntries.add(new Entry(timestamp, sensorData.getHeartRate()));

        // Add speed entry
        speedEntries.add(new Entry(timestamp, (float) sensorData.getSpeed()));

        // Limit data points to keep performance optimal
        if (heartRateEntries.size() > 100) {
            heartRateEntries.remove(0);
        }
        if (speedEntries.size() > 100) {
            speedEntries.remove(0);
        }

        LineData data = heartRateSpeedChart.getData();

        // Update or create heart rate dataset
        if (data.getDataSetCount() > 0) {
            LineDataSet heartRateSet = (LineDataSet) data.getDataSetByIndex(0);
            heartRateSet.clear();
            heartRateSet.setValues(heartRateEntries);

            if (data.getDataSetCount() > 1) {
                LineDataSet speedSet = (LineDataSet) data.getDataSetByIndex(1);
                speedSet.clear();
                speedSet.setValues(speedEntries);
            } else {
                // Create speed dataset
                LineDataSet speedSet = createSpeedDataSet();
                data.addDataSet(speedSet);
            }
        } else {
            // Create both datasets
            LineDataSet heartRateSet = createHeartRateDataSet();
            data.addDataSet(heartRateSet);

            LineDataSet speedSet = createSpeedDataSet();
            data.addDataSet(speedSet);
        }

        // Notify data changed
        data.notifyDataChanged();
        heartRateSpeedChart.notifyDataSetChanged();

        // Scroll to end to show latest data
        heartRateSpeedChart.moveViewToX(timestamp);

        // Refresh chart
        heartRateSpeedChart.invalidate();
    }

    private void updateTemperatureChart(SensorData sensorData) {
        long timestamp = sensorData.getTimestamp();

        // Add temperature entry
        temperatureEntries.add(new Entry(timestamp, (float) sensorData.getTemperature()));

        // Limit data points
        if (temperatureEntries.size() > 100) {
            temperatureEntries.remove(0);
        }

        LineData data = temperatureChart.getData();

        // Update or create temperature dataset
        if (data.getDataSetCount() > 0) {
            LineDataSet tempSet = (LineDataSet) data.getDataSetByIndex(0);
            tempSet.clear();
            tempSet.setValues(temperatureEntries);
        } else {
            // Create temperature dataset
            LineDataSet tempSet = createTemperatureDataSet();
            data.addDataSet(tempSet);
        }

        // Notify data changed
        data.notifyDataChanged();
        temperatureChart.notifyDataSetChanged();

        // Scroll to end to show latest data
        temperatureChart.moveViewToX(timestamp);

        // Refresh chart
        temperatureChart.invalidate();
    }

    private LineDataSet createHeartRateDataSet() {
        LineDataSet set = new LineDataSet(heartRateEntries, "Heart Rate (bpm)");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(Color.RED);
        set.setCircleColor(Color.RED);
        set.setLineWidth(2f);
        set.setCircleRadius(3f);
        set.setFillAlpha(65);
        set.setFillColor(Color.RED);
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setDrawCircleHole(false);
        return set;
    }

    private LineDataSet createSpeedDataSet() {
        LineDataSet set = new LineDataSet(speedEntries, "Speed (km/h)");
        set.setAxisDependency(YAxis.AxisDependency.RIGHT);
        set.setColor(Color.BLUE);
        set.setCircleColor(Color.BLUE);
        set.setLineWidth(2f);
        set.setCircleRadius(3f);
        set.setFillAlpha(65);
        set.setFillColor(Color.BLUE);
        set.setHighLightColor(Color.rgb(117, 117, 244));
        set.setDrawCircleHole(false);
        return set;
    }

    private LineDataSet createTemperatureDataSet() {
        LineDataSet set = new LineDataSet(temperatureEntries, "Temperature (°C)");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(Color.GREEN);
        set.setCircleColor(Color.GREEN);
        set.setLineWidth(2f);
        set.setCircleRadius(3f);
        set.setFillAlpha(65);
        set.setFillColor(Color.GREEN);
        set.setHighLightColor(Color.rgb(117, 244, 117));
        set.setDrawCircleHole(false);
        return set;
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
            // This is a simplified approach. In reality, you would need to pass this information from the backend
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
    protected void onDestroy() {
        super.onDestroy();
        // Stop timer to prevent memory leaks
        if (dataRefreshTimer != null) {
            dataRefreshTimer.cancel();
            dataRefreshTimer = null;
        }
    }

    @Override
    public void onBackPressed() {
        // Show confirmation dialog if there are active alerts
        if (alertsCount > 0 && alertCardView.getVisibility() == View.VISIBLE) {
            new AlertDialog.Builder(this)
                    .setTitle("Warning")
                    .setMessage("There are active alerts for this athlete. Are you sure you want to go back?")
                    .setPositiveButton("Yes", (dialog, which) -> finish())
                    .setNegativeButton("No", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }
}