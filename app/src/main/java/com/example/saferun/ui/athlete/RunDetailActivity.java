package com.example.saferun.ui.athlete;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RunDetailActivity extends AppCompatActivity {

    private static final String TAG = "RunDetailActivity";

    private Toolbar toolbar;
    private TextView sessionTitleTextView;
    private TextView sessionDescriptionTextView;
    private TextView sessionDateTextView;
    private TextView sessionDurationTextView;
    private TextView sessionDistanceTextView;
    private TextView avgSpeedTextView;
    private TextView maxSpeedTextView;
    private TextView avgHeartRateTextView;
    private TextView maxHeartRateTextView;
    private TextView avgTemperatureTextView;
    private LineChart heartRateChart;
    private LineChart speedChart;
    private LineChart temperatureChart;
    private ProgressBar progressBar;

    private RunSessionRepository runSessionRepository;
    private SensorDataRepository sensorDataRepository;
    private String sessionId;
    private String athleteId;
    private RunSession session;

    // Charts data
    private List<Entry> heartRateEntries = new ArrayList<>();
    private List<Entry> speedEntries = new ArrayList<>();
    private List<Entry> temperatureEntries = new ArrayList<>();
    private SimpleDateFormat timeFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_run_detail);
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            // Get session ID from intent
            sessionId = getIntent().getStringExtra("session_id");
            if (sessionId == null) {
                Toast.makeText(this, "Error: No session selected", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Initialize repositories
            runSessionRepository = RunSessionRepository.getInstance();
            sensorDataRepository = SensorDataRepository.getInstance();

            // Initialize views
            initViews();

            // Setup toolbar
            setupToolbar();

            // Setup charts
            setupCharts();

            // Load session data
            loadSessionData();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing run detail view", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        try {
            toolbar = findViewById(R.id.toolbar);
            sessionTitleTextView = findViewById(R.id.session_title);
            sessionDescriptionTextView = findViewById(R.id.session_description);
            sessionDateTextView = findViewById(R.id.session_date);
            sessionDurationTextView = findViewById(R.id.session_duration);
            sessionDistanceTextView = findViewById(R.id.session_distance);
            avgSpeedTextView = findViewById(R.id.avg_speed);
            maxSpeedTextView = findViewById(R.id.max_speed);
            avgHeartRateTextView = findViewById(R.id.avg_heart_rate);
            maxHeartRateTextView = findViewById(R.id.max_heart_rate);
            avgTemperatureTextView = findViewById(R.id.avg_temperature);
            heartRateChart = findViewById(R.id.heart_rate_chart);
            speedChart = findViewById(R.id.speed_chart);
            temperatureChart = findViewById(R.id.temperature_chart);
            progressBar = findViewById(R.id.progress_bar);

            // Initialize time format
            timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

            // Log any null views to help debug
            if (sessionTitleTextView == null) Log.e(TAG, "sessionTitleTextView is null");
            if (sessionDescriptionTextView == null) Log.e(TAG, "sessionDescriptionTextView is null");
            if (sessionDateTextView == null) Log.e(TAG, "sessionDateTextView is null");
            if (sessionDurationTextView == null) Log.e(TAG, "sessionDurationTextView is null");
            if (sessionDistanceTextView == null) Log.e(TAG, "sessionDistanceTextView is null");
            if (avgSpeedTextView == null) Log.e(TAG, "avgSpeedTextView is null");
            if (maxSpeedTextView == null) Log.e(TAG, "maxSpeedTextView is null");
            if (avgHeartRateTextView == null) Log.e(TAG, "avgHeartRateTextView is null");
            if (maxHeartRateTextView == null) Log.e(TAG, "maxHeartRateTextView is null");
            if (avgTemperatureTextView == null) Log.e(TAG, "avgTemperatureTextView is null");
            if (heartRateChart == null) Log.e(TAG, "heartRateChart is null");
            if (speedChart == null) Log.e(TAG, "speedChart is null");
            if (temperatureChart == null) Log.e(TAG, "temperatureChart is null");
            if (progressBar == null) Log.e(TAG, "progressBar is null");
        } catch (Exception e) {
            Log.e(TAG, "Error in initViews: " + e.getMessage(), e);
        }
    }

    private void setupToolbar() {
        try {
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setTitle("Run Details");
                }
                toolbar.setNavigationOnClickListener(v -> onBackPressed());
            } else {
                Log.e(TAG, "toolbar is null in setupToolbar");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in setupToolbar: " + e.getMessage(), e);
        }
    }

    private void setupCharts() {
        try {
            // Setup heart rate chart
            setupHeartRateChart();

            // Setup speed chart
            setupSpeedChart();

            // Setup temperature chart
            setupTemperatureChart();
        } catch (Exception e) {
            Log.e(TAG, "Error in setupCharts: " + e.getMessage(), e);
        }
    }

    private void setupHeartRateChart() {
        try {
            if (heartRateChart == null) {
                Log.e(TAG, "heartRateChart is null in setupHeartRateChart");
                return;
            }

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
            xAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return timeFormat.format(new Date((long) value));
                }
            });

            YAxis leftAxis = heartRateChart.getAxisLeft();
            leftAxis.setTextColor(Color.RED);
            leftAxis.setAxisMinimum(40f);
            leftAxis.setAxisMaximum(200f);
            leftAxis.setDrawGridLines(true);

            heartRateChart.getAxisRight().setEnabled(false);

            Legend legend = heartRateChart.getLegend();
            legend.setForm(Legend.LegendForm.LINE);
            legend.setTextColor(Color.BLACK);
            legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
            legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
            legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
            legend.setDrawInside(false);

            // Set empty data initially
            heartRateChart.setData(new LineData());
            heartRateChart.invalidate();
        } catch (Exception e) {
            Log.e(TAG, "Error in setupHeartRateChart: " + e.getMessage(), e);
        }
    }

    private void setupSpeedChart() {
        try {
            if (speedChart == null) {
                Log.e(TAG, "speedChart is null in setupSpeedChart");
                return;
            }

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
            xAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return timeFormat.format(new Date((long) value));
                }
            });

            YAxis leftAxis = speedChart.getAxisLeft();
            leftAxis.setTextColor(Color.BLUE);
            leftAxis.setAxisMinimum(0f);
            leftAxis.setAxisMaximum(20f);
            leftAxis.setDrawGridLines(true);

            speedChart.getAxisRight().setEnabled(false);

            Legend legend = speedChart.getLegend();
            legend.setForm(Legend.LegendForm.LINE);
            legend.setTextColor(Color.BLACK);
            legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
            legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
            legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
            legend.setDrawInside(false);

            // Set empty data initially
            speedChart.setData(new LineData());
            speedChart.invalidate();
        } catch (Exception e) {
            Log.e(TAG, "Error in setupSpeedChart: " + e.getMessage(), e);
        }
    }

    private void setupTemperatureChart() {
        try {
            if (temperatureChart == null) {
                Log.e(TAG, "temperatureChart is null in setupTemperatureChart");
                return;
            }

            temperatureChart.getDescription().setEnabled(false);
            temperatureChart.setTouchEnabled(true);
            temperatureChart.setDragEnabled(true);
            temperatureChart.setScaleEnabled(true);
            temperatureChart.setPinchZoom(true);
            temperatureChart.setDrawGridBackground(false);
            temperatureChart.setBackgroundColor(Color.WHITE);

            XAxis xAxis = temperatureChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setDrawGridLines(false);
            xAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return timeFormat.format(new Date((long) value));
                }
            });

            YAxis leftAxis = temperatureChart.getAxisLeft();
            leftAxis.setTextColor(Color.GREEN);
            leftAxis.setAxisMinimum(34f);
            leftAxis.setAxisMaximum(41f);
            leftAxis.setDrawGridLines(true);

            temperatureChart.getAxisRight().setEnabled(false);

            Legend legend = temperatureChart.getLegend();
            legend.setForm(Legend.LegendForm.LINE);
            legend.setTextColor(Color.BLACK);
            legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
            legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
            legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
            legend.setDrawInside(false);

            // Set empty data initially
            temperatureChart.setData(new LineData());
            temperatureChart.invalidate();
        } catch (Exception e) {
            Log.e(TAG, "Error in setupTemperatureChart: " + e.getMessage(), e);
        }
    }

    private void loadSessionData() {
        try {
            showProgress(true);

            if (sessionId == null || sessionId.isEmpty()) {
                showProgress(false);
                Toast.makeText(this, "Error: No session selected", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            runSessionRepository.getRunSession(sessionId, new RunSessionRepository.RunSessionCallback() {
                @Override
                public void onSuccess(RunSession runSession) {
                    try {
                        if (runSession == null) {
                            showProgress(false);
                            Toast.makeText(RunDetailActivity.this,
                                    "Error: Session not found",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }

                        session = runSession;

                        if (session.getAthletes() == null || session.getAthletes().isEmpty()) {
                            // Try to use the current user's ID if session has no athletes
                            athleteId = session.getCoachId(); // Fallback
                            Log.w(TAG, "No athletes found for session, using coach ID as fallback");
                        } else {
                            athleteId = session.getAthletes().get(0);
                        }

                        if (athleteId == null || athleteId.isEmpty()) {
                            showProgress(false);
                            Toast.makeText(RunDetailActivity.this,
                                    "Error: No athlete associated with this session",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Update UI with session details
                        updateSessionDetails();

                        // Load sensor data for this session
                        loadSensorData();
                    } catch (Exception e) {
                        showProgress(false);
                        Log.e(TAG, "Error in loadSessionData onSuccess: " + e.getMessage(), e);
                        Toast.makeText(RunDetailActivity.this,
                                "Error processing session data",
                                Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    showProgress(false);
                    Toast.makeText(RunDetailActivity.this,
                            "Error loading session: " + errorMessage,
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error loading session: " + errorMessage);
                    finish();
                }
            });
        } catch (Exception e) {
            showProgress(false);
            Log.e(TAG, "Error in loadSessionData: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading session", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void updateSessionDetails() {
        try {
            if (session == null) {
                Log.e(TAG, "Session is null in updateSessionDetails");
                return;
            }

            // Update title and description (with null checks)
            safeSetText(sessionTitleTextView, session.getTitle(), "Untitled Session");
            safeSetText(sessionDescriptionTextView, session.getDescription(), "No description available");

            // Update date
            if (sessionDateTextView != null) {
                if (session.getDate() != null) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault());
                    sessionDateTextView.setText(dateFormat.format(session.getDate()));
                } else {
                    sessionDateTextView.setText("No date");
                }
            }

            // Update duration and distance (with null checks)
            if (sessionDurationTextView != null) {
                sessionDurationTextView.setText(String.format("%d min", session.getDuration()));
            }
            if (sessionDistanceTextView != null) {
                sessionDistanceTextView.setText(String.format("%.1f km", session.getDistance()));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in updateSessionDetails: " + e.getMessage(), e);
        }
    }

    private void safeSetText(TextView textView, String text, String defaultText) {
        if (textView != null) {
            textView.setText(text != null && !text.isEmpty() ? text : defaultText);
        }
    }

    private void loadSensorData() {
        try {
            if (sessionId == null || athleteId == null) {
                showProgress(false);
                Toast.makeText(this, "Error: Invalid session or athlete ID", Toast.LENGTH_SHORT).show();
                return;
            }

            sensorDataRepository.getSensorDataHistory(sessionId, athleteId, 1000,
                    new SensorDataRepository.SensorDataListCallback() {
                        @Override
                        public void onSuccess(List<SensorData> sensorDataList) {
                            showProgress(false);

                            if (sensorDataList == null || sensorDataList.isEmpty()) {
                                Toast.makeText(RunDetailActivity.this,
                                        "No sensor data available for this session",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }

                            processSensorData(sensorDataList);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            showProgress(false);
                            Toast.makeText(RunDetailActivity.this,
                                    "Error loading sensor data: " + errorMessage,
                                    Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error loading sensor data: " + errorMessage);
                        }
                    });
        } catch (Exception e) {
            showProgress(false);
            Log.e(TAG, "Error in loadSensorData: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading sensor data", Toast.LENGTH_SHORT).show();
        }
    }

    private void processSensorData(List<SensorData> sensorDataList) {
        try {
            if (sensorDataList == null || sensorDataList.isEmpty()) {
                Log.w(TAG, "No sensor data to process");
                return;
            }

            // Clear existing entries
            heartRateEntries.clear();
            speedEntries.clear();
            temperatureEntries.clear();

            // Calculate averages and maximums
            double sumHeartRate = 0;
            int maxHeartRate = 0;
            double sumSpeed = 0;
            double maxSpeed = 0;
            double sumTemperature = 0;

            int validHeartRateCount = 0;
            int validSpeedCount = 0;
            int validTemperatureCount = 0;

            // Process sensor data
            for (SensorData data : sensorDataList) {
                if (data == null) continue;

                long timestamp = data.getTimestamp();

                // Add heart rate data point
                if (data.getHeartRate() > 0) {
                    heartRateEntries.add(new Entry(timestamp, data.getHeartRate()));
                    sumHeartRate += data.getHeartRate();
                    maxHeartRate = Math.max(maxHeartRate, data.getHeartRate());
                    validHeartRateCount++;
                }

                // Add speed data point
                if (data.getSpeed() >= 0) {
                    speedEntries.add(new Entry(timestamp, (float) data.getSpeed()));
                    sumSpeed += data.getSpeed();
                    maxSpeed = Math.max(maxSpeed, data.getSpeed());
                    validSpeedCount++;
                }

                // Add temperature data point
                if (data.getTemperature() > 0) {
                    temperatureEntries.add(new Entry(timestamp, (float) data.getTemperature()));
                    sumTemperature += data.getTemperature();
                    validTemperatureCount++;
                }
            }

            // Calculate averages
            double avgHeartRate = validHeartRateCount > 0 ? sumHeartRate / validHeartRateCount : 0;
            double avgSpeed = validSpeedCount > 0 ? sumSpeed / validSpeedCount : 0;
            double avgTemperature = validTemperatureCount > 0 ? sumTemperature / validTemperatureCount : 0;

            // Update UI with calculated statistics (with null checks)
            safeSetText(avgHeartRateTextView, String.format("%.0f bpm", avgHeartRate));
            safeSetText(maxHeartRateTextView, String.format("%d bpm", maxHeartRate));
            safeSetText(avgSpeedTextView, String.format("%.1f km/h", avgSpeed));
            safeSetText(maxSpeedTextView, String.format("%.1f km/h", maxSpeed));
            safeSetText(avgTemperatureTextView, String.format("%.1f °C", avgTemperature));

            // Update charts
            updateCharts();
        } catch (Exception e) {
            Log.e(TAG, "Error in processSensorData: " + e.getMessage(), e);
            Toast.makeText(this, "Error processing sensor data", Toast.LENGTH_SHORT).show();
        }
    }

    private void safeSetText(TextView textView, String text) {
        if (textView != null) {
            textView.setText(text);
        }
    }

    private void updateCharts() {
        try {
            // Update heart rate chart
            if (heartRateChart != null && !heartRateEntries.isEmpty()) {
                LineDataSet heartRateDataSet = new LineDataSet(heartRateEntries, "Heart Rate (bpm)");
                heartRateDataSet.setColor(Color.RED);
                heartRateDataSet.setCircleColor(Color.RED);
                heartRateDataSet.setDrawCircles(false);
                heartRateDataSet.setLineWidth(2f);
                heartRateDataSet.setDrawValues(false);
                heartRateDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

                LineData heartRateData = new LineData(heartRateDataSet);
                heartRateChart.setData(heartRateData);
                heartRateChart.invalidate();
            } else if (heartRateChart != null) {
                // Clear chart if no data
                heartRateChart.clear();
                heartRateChart.setNoDataText("No heart rate data available");
                heartRateChart.invalidate();
            }

            // Update speed chart
            if (speedChart != null && !speedEntries.isEmpty()) {
                LineDataSet speedDataSet = new LineDataSet(speedEntries, "Speed (km/h)");
                speedDataSet.setColor(Color.BLUE);
                speedDataSet.setCircleColor(Color.BLUE);
                speedDataSet.setDrawCircles(false);
                speedDataSet.setLineWidth(2f);
                speedDataSet.setDrawValues(false);
                speedDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

                LineData speedData = new LineData(speedDataSet);
                speedChart.setData(speedData);
                speedChart.invalidate();
            } else if (speedChart != null) {
                // Clear chart if no data
                speedChart.clear();
                speedChart.setNoDataText("No speed data available");
                speedChart.invalidate();
            }

            // Update temperature chart
            if (temperatureChart != null && !temperatureEntries.isEmpty()) {
                LineDataSet temperatureDataSet = new LineDataSet(temperatureEntries, "Temperature (°C)");
                temperatureDataSet.setColor(Color.GREEN);
                temperatureDataSet.setCircleColor(Color.GREEN);
                temperatureDataSet.setDrawCircles(false);
                temperatureDataSet.setLineWidth(2f);
                temperatureDataSet.setDrawValues(false);
                temperatureDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

                LineData temperatureData = new LineData(temperatureDataSet);
                temperatureChart.setData(temperatureData);
                temperatureChart.invalidate();
            } else if (temperatureChart != null) {
                // Clear chart if no data
                temperatureChart.clear();
                temperatureChart.setNoDataText("No temperature data available");
                temperatureChart.invalidate();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in updateCharts: " + e.getMessage(), e);
        }
    }

    private void showProgress(boolean show) {
        try {
            if (progressBar != null) {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in showProgress: " + e.getMessage(), e);
        }
    }
}