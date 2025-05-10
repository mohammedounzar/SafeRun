package com.example.saferun.ui.athlete;

import android.content.Intent;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.saferun.R;
import com.example.saferun.data.firebase.FirebaseAuthManager;
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
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AthleteRunHistoryActivity extends AppCompatActivity implements AthleteRunSessionAdapter.OnSessionClickListener {

    private static final String TAG = "AthleteRunHistory";

    private Toolbar toolbar;
    private RecyclerView sessionsRecyclerView;
    private TextView noSessionsTextView;
    private ProgressBar progressBar;
    private MaterialCardView summaryCardView;
    private TextView totalSessionsTextView;
    private TextView totalDistanceTextView;
    private TextView totalDurationTextView;
    private TextView avgSpeedTextView;
    private TextView avgHeartRateTextView;
    private LineChart performanceChart;

    private RunSessionRepository runSessionRepository;
    private SensorDataRepository sensorDataRepository;
    private AthleteRunSessionAdapter sessionAdapter;
    private List<RunSession> sessions = new ArrayList<>();
    private Map<String, List<SensorData>> sessionSensorDataMap = new HashMap<>();

    private String currentAthleteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_athlete_run_history);

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            // Get current athlete ID
            FirebaseAuthManager authManager = FirebaseAuthManager.getInstance();
            currentAthleteId = authManager.getCurrentUserId();

            if (currentAthleteId == null) {
                Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show();
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

            // Setup RecyclerView
            setupRecyclerView();

            // Setup performance chart
            setupPerformanceChart();

            // Load athlete's run sessions
            loadRunSessions();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "An error occurred while initializing the screen", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        try {
            toolbar = findViewById(R.id.toolbar);
            sessionsRecyclerView = findViewById(R.id.sessions_recycler_view);
            noSessionsTextView = findViewById(R.id.no_sessions_text);
            progressBar = findViewById(R.id.progress_bar);
            summaryCardView = findViewById(R.id.summary_card);

            // Find all TextView references
            totalSessionsTextView = findViewById(R.id.total_sessions_text);
            totalDistanceTextView = findViewById(R.id.total_distance_text);
            totalDurationTextView = findViewById(R.id.total_duration_text);
            avgSpeedTextView = findViewById(R.id.avg_speed_text);
            avgHeartRateTextView = findViewById(R.id.avg_heart_rate_text);
            performanceChart = findViewById(R.id.performance_chart);

            // Log missing views to help debug
            if (totalSessionsTextView == null) Log.e(TAG, "totalSessionsTextView is null!");
            if (totalDistanceTextView == null) Log.e(TAG, "totalDistanceTextView is null!");
            if (totalDurationTextView == null) Log.e(TAG, "totalDurationTextView is null!");
            if (avgSpeedTextView == null) Log.e(TAG, "avgSpeedTextView is null!");
            if (avgHeartRateTextView == null) Log.e(TAG, "avgHeartRateTextView is null!");
            if (performanceChart == null) Log.e(TAG, "performanceChart is null!");

            // Verify RecyclerView is found
            if (sessionsRecyclerView == null) {
                Log.e(TAG, "sessionsRecyclerView is null!");
                Toast.makeText(this, "Error: Unable to initialize the view", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in initViews: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupToolbar() {
        try {
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setTitle("My Running History");
                }
                toolbar.setNavigationOnClickListener(v -> onBackPressed());
            } else {
                Log.e(TAG, "Toolbar is null in setupToolbar");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in setupToolbar: " + e.getMessage(), e);
        }
    }

    private void setupRecyclerView() {
        try {
            if (sessionsRecyclerView != null) {
                sessionAdapter = new AthleteRunSessionAdapter(this, sessions, this);
                sessionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                sessionsRecyclerView.setAdapter(sessionAdapter);
                Log.d(TAG, "RecyclerView setup complete");
            } else {
                Log.e(TAG, "sessionsRecyclerView is null in setupRecyclerView");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in setupRecyclerView: " + e.getMessage(), e);
        }
    }

    private void setupPerformanceChart() {
        try {
            if (performanceChart == null) {
                Log.e(TAG, "performanceChart is null in setupPerformanceChart");
                return;
            }

            performanceChart.getDescription().setEnabled(false);
            performanceChart.setTouchEnabled(true);
            performanceChart.setDragEnabled(true);
            performanceChart.setScaleEnabled(true);
            performanceChart.setPinchZoom(false);
            performanceChart.setDrawGridBackground(false);

            // X-axis setup
            XAxis xAxis = performanceChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setDrawGridLines(false);
            xAxis.setGranularity(1f);

            // Y-axis setup
            YAxis leftAxis = performanceChart.getAxisLeft();
            leftAxis.setDrawGridLines(true);
            leftAxis.setAxisMinimum(0f);

            // Disable right Y-axis
            performanceChart.getAxisRight().setEnabled(false);

            // Legend setup
            Legend legend = performanceChart.getLegend();
            legend.setForm(Legend.LegendForm.LINE);
            legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
            legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
            legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
            legend.setDrawInside(false);

            Log.d(TAG, "Performance chart setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error in setupPerformanceChart: " + e.getMessage(), e);
        }
    }

    private void loadRunSessions() {
        try {
            showProgress(true);

            runSessionRepository.getRunSessionsByAthlete(currentAthleteId, new RunSessionRepository.RunSessionsCallback() {
                @Override
                public void onSuccess(List<RunSession> runSessions) {
                    try {
                        sessions.clear();

                        if (runSessions != null && !runSessions.isEmpty()) {
                            // Filter for completed sessions only
                            for (RunSession session : runSessions) {
                                if ("completed".equals(session.getStatus())) {
                                    sessions.add(session);
                                }
                            }

                            // Sort sessions by date, most recent first
                            Collections.sort(sessions, (s1, s2) -> {
                                if (s1.getDate() == null || s2.getDate() == null) {
                                    return 0;
                                }
                                return s2.getDate().compareTo(s1.getDate());
                            });

                            // Check if adapter is null (safety check)
                            if (sessionAdapter != null) {
                                sessionAdapter.notifyDataSetChanged();
                            } else {
                                Log.e(TAG, "sessionAdapter is null in loadRunSessions onSuccess");
                            }

                            showNoSessions(false);

                            // Load sensor data for statistical calculations
                            loadSensorDataForSessions();
                        } else {
                            Log.d(TAG, "No sessions found or runSessions is null");
                            showProgress(false);
                            showNoSessions(true);

                            // Call updateSummaryStatistics with empty list to show zeros
                            setSafeStatisticsToZero();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in loadRunSessions onSuccess: " + e.getMessage(), e);
                        showProgress(false);
                        Toast.makeText(AthleteRunHistoryActivity.this,
                                "Error processing session data", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    showProgress(false);
                    showNoSessions(true);
                    Toast.makeText(AthleteRunHistoryActivity.this,
                            "Error loading run sessions: " + errorMessage,
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error loading run sessions: " + errorMessage);

                    // Set all stats to zero on error
                    setSafeStatisticsToZero();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in loadRunSessions: " + e.getMessage(), e);
            showProgress(false);
            Toast.makeText(this, "Error loading sessions", Toast.LENGTH_SHORT).show();
        }
    }

    // Safe method to set all statistics to zero
    private void setSafeStatisticsToZero() {
        try {
            if (totalSessionsTextView != null) totalSessionsTextView.setText("0");
            if (totalDistanceTextView != null) totalDistanceTextView.setText("0.0 km");
            if (totalDurationTextView != null) totalDurationTextView.setText("0 min");
            if (avgSpeedTextView != null) avgSpeedTextView.setText("0.0 km/h");
            if (avgHeartRateTextView != null) avgHeartRateTextView.setText("0 bpm");

            // Clear the performance chart
            if (performanceChart != null) {
                performanceChart.clear();
                performanceChart.setNoDataText("No performance data available");
                performanceChart.invalidate();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in setSafeStatisticsToZero: " + e.getMessage(), e);
        }
    }

    private void loadSensorDataForSessions() {
        try {
            // Reset tracking variables
            sessionSensorDataMap.clear();
            final int[] loadedCount = {0};
            final int totalSessions = sessions.size();

            if (totalSessions == 0) {
                showProgress(false);
                setSafeStatisticsToZero();
                return;
            }

            // Load sensor data for each session
            for (RunSession session : sessions) {
                if (session == null) {
                    loadedCount[0]++;
                    checkIfDoneLoading(loadedCount[0], totalSessions);
                    continue;
                }

                final String sessionId = session.getId();

                if (sessionId == null || sessionId.isEmpty()) {
                    loadedCount[0]++;
                    checkIfDoneLoading(loadedCount[0], totalSessions);
                    continue;
                }

                sensorDataRepository.getSensorDataHistory(sessionId, currentAthleteId, 1000,
                        new SensorDataRepository.SensorDataListCallback() {
                            @Override
                            public void onSuccess(List<SensorData> sensorDataList) {
                                loadedCount[0]++;

                                if (sensorDataList != null && !sensorDataList.isEmpty()) {
                                    sessionSensorDataMap.put(sessionId, sensorDataList);
                                }

                                checkIfDoneLoading(loadedCount[0], totalSessions);
                            }

                            @Override
                            public void onError(String errorMessage) {
                                loadedCount[0]++;
                                Log.e(TAG, "Error loading sensor data for session " + sessionId + ": " + errorMessage);

                                checkIfDoneLoading(loadedCount[0], totalSessions);
                            }
                        });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in loadSensorDataForSessions: " + e.getMessage(), e);
            showProgress(false);
            Toast.makeText(this, "Error loading sensor data", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkIfDoneLoading(int loadedCount, int totalSessions) {
        if (loadedCount >= totalSessions) {
            showProgress(false);
            updateSummaryStatistics();
            updatePerformanceChart();
        }
    }

    private void updateSummaryStatistics() {
        try {
            // Verify all TextViews exist
            if (totalSessionsTextView == null || totalDistanceTextView == null ||
                    totalDurationTextView == null || avgSpeedTextView == null ||
                    avgHeartRateTextView == null) {

                Log.e(TAG, "One or more TextViews are null in updateSummaryStatistics");
                return;
            }

            if (sessions.isEmpty()) {
                setSafeStatisticsToZero();
                return;
            }

            // Calculate total distance, duration and sessions
            double totalDistance = 0;
            long totalDuration = 0;

            for (RunSession session : sessions) {
                if (session != null) {
                    totalDistance += session.getDistance();
                    totalDuration += session.getDuration();
                }
            }

            // Calculate average speed, heart rate from sensor data
            double avgSpeed = 0;
            int avgHeartRate = 0;
            int validSpeedDatapoints = 0;
            int validHeartRateDatapoints = 0;

            for (Map.Entry<String, List<SensorData>> entry : sessionSensorDataMap.entrySet()) {
                List<SensorData> dataList = entry.getValue();
                if (dataList != null) {
                    for (SensorData data : dataList) {
                        if (data != null) {
                            if (data.getSpeed() > 0) {
                                avgSpeed += data.getSpeed();
                                validSpeedDatapoints++;
                            }

                            if (data.getHeartRate() > 0) {
                                avgHeartRate += data.getHeartRate();
                                validHeartRateDatapoints++;
                            }
                        }
                    }
                }
            }

            if (validSpeedDatapoints > 0) {
                avgSpeed /= validSpeedDatapoints;
            }

            if (validHeartRateDatapoints > 0) {
                avgHeartRate /= validHeartRateDatapoints;
            }

            // Update UI with calculated statistics - safely
            totalSessionsTextView.setText(String.valueOf(sessions.size()));
            totalDistanceTextView.setText(String.format("%.1f km", totalDistance));
            totalDurationTextView.setText(String.format("%d min", totalDuration));
            avgSpeedTextView.setText(String.format("%.1f km/h", avgSpeed));
            avgHeartRateTextView.setText(String.format("%d bpm", avgHeartRate));

            Log.d(TAG, "Summary statistics updated successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in updateSummaryStatistics: " + e.getMessage(), e);
            // Try to set to zero values in case of error
            setSafeStatisticsToZero();
        }
    }

    private void updatePerformanceChart() {
        try {
            if (performanceChart == null) {
                Log.e(TAG, "performanceChart is null in updatePerformanceChart");
                return;
            }

            List<Entry> performanceEntries = new ArrayList<>();
            List<String> sessionLabels = new ArrayList<>();

            // Utiliser les 10 dernières sessions, dans l'ordre chronologique
            int total = sessions.size();
            int start = Math.max(0, total - 10); // Pour afficher les 10 dernières sans inverser

            for (int i = start; i < total; i++) {
                RunSession session = sessions.get(i);
                if (session == null) continue;

                float distance = (float) session.getDistance();

                // Vérification de sécurité
                if (Float.isNaN(distance) || Float.isInfinite(distance) || distance < 0) {
                    Log.w(TAG, "Skipping invalid distance value: " + distance + " at index " + i);
                    continue;
                }

                // L'index X sera basé sur l'ordre d'affichage
                int xIndex = performanceEntries.size();
                performanceEntries.add(new Entry(xIndex, distance));

                String label = "";
                if (session.getDate() != null) {
                    label = String.format("%tm/%td", session.getDate(), session.getDate());
                }
                sessionLabels.add(label);
            }

            if (performanceEntries.isEmpty()) {
                performanceChart.clear();
                performanceChart.setNoDataText("No performance data available");
                performanceChart.invalidate();
                return;
            }

            // Créer le dataset
            LineDataSet dataSet = new LineDataSet(performanceEntries, "Distance (km)");
            dataSet.setColor(getResources().getColor(R.color.primary_color));
            dataSet.setValueTextColor(getResources().getColor(R.color.text_primary));
            dataSet.setLineWidth(2f);
            dataSet.setCircleColor(getResources().getColor(R.color.primary_color));
            dataSet.setCircleRadius(4f);
            dataSet.setDrawCircleHole(false);
            dataSet.setValueTextSize(10f);
            dataSet.setMode(LineDataSet.Mode.LINEAR); // Sécurisé vs HORIZONTAL_BEZIER

            LineData lineData = new LineData(dataSet);
            performanceChart.setData(lineData);

            // Mise à jour du graphique
            performanceChart.invalidate();
            Log.d(TAG, "Performance chart updated successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in updatePerformanceChart: " + e.getMessage(), e);
            if (performanceChart != null) {
                performanceChart.clear();
                performanceChart.setNoDataText("Error loading performance data");
                performanceChart.invalidate();
            }
        }
    }

    private void showNoSessions(boolean show) {
        try {
            if (noSessionsTextView != null && sessionsRecyclerView != null) {
                if (show) {
                    noSessionsTextView.setVisibility(View.VISIBLE);
                    sessionsRecyclerView.setVisibility(View.GONE);
                } else {
                    noSessionsTextView.setVisibility(View.GONE);
                    sessionsRecyclerView.setVisibility(View.VISIBLE);
                }
            } else {
                Log.e(TAG, "noSessionsTextView or sessionsRecyclerView is null in showNoSessions");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in showNoSessions: " + e.getMessage(), e);
        }
    }

    private void showProgress(boolean show) {
        try {
            if (progressBar != null) {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            } else {
                Log.e(TAG, "progressBar is null in showProgress");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in showProgress: " + e.getMessage(), e);
        }
    }

    @Override
    public void onSessionClick(RunSession session) {
        try {
            // Navigate to detailed session view
            Intent intent = new Intent(this, RunDetailActivity.class);
            intent.putExtra("session_id", session.getId());
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error in onSessionClick: " + e.getMessage(), e);
            Toast.makeText(this, "Error opening session details", Toast.LENGTH_SHORT).show();
        }
    }
}