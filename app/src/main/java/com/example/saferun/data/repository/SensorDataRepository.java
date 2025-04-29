package com.example.saferun.data.repository;

import android.util.Log;

import com.example.saferun.data.firebase.FirebaseAuthManager;
import com.example.saferun.data.model.SensorData;
import com.example.saferun.ml.AnomalyPredictionClient;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SensorDataRepository {
    private static final String TAG = "SensorDataRepository";

    private FirebaseAuthManager authManager;
    private DatabaseReference sensorDataRef;
    private static SensorDataRepository instance;
    private AnomalyPredictionClient predictionClient;

    private SensorDataRepository() {
        authManager = FirebaseAuthManager.getInstance();
        // Updated path to match the simulator's path (sensor_data instead of sensorData)
        sensorDataRef = FirebaseDatabase.getInstance().getReference("sensor_data");
        predictionClient = AnomalyPredictionClient.getInstance();
        Log.d(TAG, "Initialized SensorDataRepository with path: sensor_data");
    }

    public static synchronized SensorDataRepository getInstance() {
        if (instance == null) {
            instance = new SensorDataRepository();
        }
        return instance;
    }

    public interface SensorDataCallback {
        void onSuccess(SensorData sensorData);
        void onError(String errorMessage);
    }

    public interface SensorDataListCallback {
        void onSuccess(List<SensorData> sensorDataList);
        void onError(String errorMessage);
    }

    /**
     * Get the latest sensor data for a specific athlete in a session
     */
    public void getLatestSensorData(String sessionId, String athleteId, SensorDataCallback callback) {
        Log.d(TAG, "Getting latest sensor data for session: " + sessionId + ", athlete: " + athleteId);

        // Reference to the specific path where the data should be
        DatabaseReference pathRef = sensorDataRef.child(sessionId).child(athleteId);

        // First check if any data exists at this path
        pathRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "Path exists: " + dataSnapshot.exists() + ", has children: " + dataSnapshot.hasChildren());

                if (!dataSnapshot.exists()) {
                    Log.e(TAG, "No data found at path: sensor_data/" + sessionId + "/" + athleteId);
                    callback.onError("No sensor data found for this athlete and session");
                    return;
                }

                // Try to create SensorData from the data at this path
                try {
                    // This is a special case: if the data is stored directly without document IDs
                    // e.g., if the Python simulator is sending data directly to this path
                    Object rawValue = dataSnapshot.getValue();
                    Log.d(TAG, "Raw value type: " + (rawValue != null ? rawValue.getClass().getName() : "null"));

                    // Create SensorData object
                    SensorData sensorData = createSensorDataFromSnapshot(dataSnapshot, sessionId, athleteId);

                    // Check if data requires anomaly prediction
                    if (!sensorData.isAnomalyDetected()) {
                        // Use ML prediction API to detect anomalies
                        detectAnomaliesWithML(sensorData, athleteId, new AnomalyPredictionClient.PredictionCallback() {
                            @Override
                            public void onSuccess(boolean isAnomaly) {
                                sensorData.setAnomalyDetected(isAnomaly);
                                callback.onSuccess(sensorData);
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Log.e(TAG, "ML prediction failed: " + errorMessage);
                                // Fall back to basic anomaly detection
                                detectAnomaliesLocally(sensorData);
                                callback.onSuccess(sensorData);
                            }
                        });
                    } else {
                        // If anomaly already detected, return as is
                        callback.onSuccess(sensorData);
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error parsing sensor data directly: " + e.getMessage(), e);

                    // Try the traditional approach with child nodes
                    try {
                        // If the data has child nodes, try to get the latest one
                        if (dataSnapshot.hasChildren()) {
                            Log.d(TAG, "Attempting to read from child nodes instead");

                            // Get all children and find the latest by timestamp
                            SensorData latestData = null;
                            long latestTimestamp = 0;

                            for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                                try {
                                    SensorData data = createSensorDataFromSnapshot(childSnapshot, sessionId, athleteId);
                                    if (data.getTimestamp() > latestTimestamp) {
                                        latestData = data;
                                        latestTimestamp = data.getTimestamp();
                                    }
                                } catch (Exception ex) {
                                    Log.e(TAG, "Error parsing child node: " + ex.getMessage());
                                }
                            }

                            if (latestData != null) {
                                Log.d(TAG, "Found latest sensor data from child nodes");

                                // Final data object to be returned
                                final SensorData finalData = latestData;

                                // Check for anomalies before returning
                                if (!finalData.isAnomalyDetected()) {
                                    // Use ML prediction API
                                    detectAnomaliesWithML(finalData, athleteId, new AnomalyPredictionClient.PredictionCallback() {
                                        @Override
                                        public void onSuccess(boolean isAnomaly) {
                                            finalData.setAnomalyDetected(isAnomaly);
                                            callback.onSuccess(finalData);
                                        }

                                        @Override
                                        public void onError(String errorMessage) {
                                            Log.e(TAG, "ML prediction failed: " + errorMessage);
                                            // Fall back to basic anomaly detection
                                            detectAnomaliesLocally(finalData);
                                            callback.onSuccess(finalData);
                                        }
                                    });
                                } else {
                                    callback.onSuccess(finalData);
                                }
                                return;
                            }
                        }

                        Log.e(TAG, "No valid sensor data found after all attempts");
                        callback.onError("Failed to parse sensor data properly");

                    } catch (Exception ex) {
                        Log.e(TAG, "Error in fallback approach: " + ex.getMessage(), ex);
                        callback.onError("Error processing sensor data: " + ex.getMessage());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Database error: " + databaseError.getMessage());
                callback.onError("Database error: " + databaseError.getMessage());
            }
        });
    }

    /**
     * Create a SensorData object from a DataSnapshot, handling various data formats
     */
    private SensorData createSensorDataFromSnapshot(DataSnapshot snapshot, String sessionId, String athleteId) {
        SensorData sensorData = new SensorData();
        sensorData.setSessionId(sessionId);
        sensorData.setAthleteId(athleteId);

        // Default values
        sensorData.setHeartRate(0);
        sensorData.setTemperature(0.0);
        sensorData.setSpeed(0.0);
        sensorData.setTimestamp(System.currentTimeMillis());
        sensorData.setStatus("active");
        sensorData.setAnomalyDetected(false);

        try {
            // Try to read fields directly from the snapshot
            if (snapshot.hasChild("heart_rate")) {
                Object heartRateValue = snapshot.child("heart_rate").getValue();
                if (heartRateValue instanceof Number) {
                    sensorData.setHeartRate(((Number) heartRateValue).intValue());
                    Log.d(TAG, "Found heart rate: " + sensorData.getHeartRate());
                }
            }

            if (snapshot.hasChild("temperature")) {
                Object tempValue = snapshot.child("temperature").getValue();
                if (tempValue instanceof Number) {
                    sensorData.setTemperature(((Number) tempValue).doubleValue());
                    Log.d(TAG, "Found temperature: " + sensorData.getTemperature());
                }
            }

            if (snapshot.hasChild("speed")) {
                Object speedValue = snapshot.child("speed").getValue();
                if (speedValue instanceof Number) {
                    sensorData.setSpeed(((Number) speedValue).doubleValue());
                    Log.d(TAG, "Found speed: " + sensorData.getSpeed());
                }
            }

            if (snapshot.hasChild("timestamp")) {
                Object timeValue = snapshot.child("timestamp").getValue();
                if (timeValue instanceof Number) {
                    sensorData.setTimestamp(((Number) timeValue).longValue());
                    Log.d(TAG, "Found timestamp: " + sensorData.getTimestamp());
                }
            }

            if (snapshot.hasChild("status")) {
                Object statusValue = snapshot.child("status").getValue();
                if (statusValue != null) {
                    sensorData.setStatus(statusValue.toString());
                }
            }

            if (snapshot.hasChild("anomaly_detected")) {
                Object anomalyValue = snapshot.child("anomaly_detected").getValue();
                if (anomalyValue instanceof Boolean) {
                    sensorData.setAnomalyDetected((Boolean) anomalyValue);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading fields directly from snapshot: " + e.getMessage(), e);

            // Fallback: try to see if the snapshot itself contains the data (not a map)
            try {
                // Check if the snapshot itself is a value rather than an object
                Object rawValue = snapshot.getValue();

                if (rawValue instanceof Map) {
                    Log.d(TAG, "Snapshot contains a Map, processing fields...");
                    Map<String, Object> dataMap = (Map<String, Object>) rawValue;

                    if (dataMap.containsKey("heart_rate")) {
                        Object heartRateObj = dataMap.get("heart_rate");
                        if (heartRateObj instanceof Number) {
                            sensorData.setHeartRate(((Number) heartRateObj).intValue());
                            Log.d(TAG, "Found heart rate from map: " + sensorData.getHeartRate());
                        }
                    }

                    if (dataMap.containsKey("temperature")) {
                        Object tempObj = dataMap.get("temperature");
                        if (tempObj instanceof Number) {
                            sensorData.setTemperature(((Number) tempObj).doubleValue());
                            Log.d(TAG, "Found temperature from map: " + sensorData.getTemperature());
                        }
                    }

                    if (dataMap.containsKey("speed")) {
                        Object speedObj = dataMap.get("speed");
                        if (speedObj instanceof Number) {
                            sensorData.setSpeed(((Number) speedObj).doubleValue());
                            Log.d(TAG, "Found speed from map: " + sensorData.getSpeed());
                        }
                    }

                    if (dataMap.containsKey("timestamp")) {
                        Object timestampObj = dataMap.get("timestamp");
                        if (timestampObj instanceof Number) {
                            sensorData.setTimestamp(((Number) timestampObj).longValue());
                            Log.d(TAG, "Found timestamp from map: " + sensorData.getTimestamp());
                        }
                    }

                    if (dataMap.containsKey("status")) {
                        Object statusObj = dataMap.get("status");
                        if (statusObj != null) {
                            sensorData.setStatus(statusObj.toString());
                        }
                    }

                    if (dataMap.containsKey("anomaly_detected")) {
                        Object anomalyObj = dataMap.get("anomaly_detected");
                        if (anomalyObj instanceof Boolean) {
                            sensorData.setAnomalyDetected((Boolean) anomalyObj);
                        }
                    }
                }
            } catch (ClassCastException cce) {
                Log.e(TAG, "ClassCastException when processing raw value: " + cce.getMessage());
                // Continue with the default values set initially
            } catch (Exception ex) {
                Log.e(TAG, "Error in fallback approach: " + ex.getMessage(), ex);
                // Continue with the default values set initially
            }
        }

        return sensorData;
    }

    /**
     * Get sensor data history for a specific athlete in a session
     */
    public void getSensorDataHistory(String sessionId, String athleteId, int limit, SensorDataListCallback callback) {
        Log.d(TAG, "Getting sensor data history for session: " + sessionId + ", athlete: " + athleteId);

        // Reference to the specific path
        DatabaseReference pathRef = sensorDataRef.child(sessionId).child(athleteId);

        pathRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "History path exists: " + dataSnapshot.exists() + ", has children: " + dataSnapshot.hasChildren());

                if (!dataSnapshot.exists()) {
                    callback.onError("No sensor data history found");
                    return;
                }

                List<SensorData> dataList = new ArrayList<>();

                // If the snapshot has children, process each child
                if (dataSnapshot.hasChildren()) {
                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                        try {
                            SensorData data = createSensorDataFromSnapshot(childSnapshot, sessionId, athleteId);
                            dataList.add(data);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing history item: " + e.getMessage());
                            // Continue with other entries
                        }
                    }
                } else {
                    // If no children, try to parse the snapshot itself
                    try {
                        SensorData data = createSensorDataFromSnapshot(dataSnapshot, sessionId, athleteId);
                        dataList.add(data);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing single history item: " + e.getMessage());
                    }
                }

                // Sort by timestamp (ascending)
                Collections.sort(dataList, (data1, data2) ->
                        Long.compare(data1.getTimestamp(), data2.getTimestamp()));

                // Limit results if needed
                if (dataList.size() > limit) {
                    dataList = dataList.subList(dataList.size() - limit, dataList.size());
                }

                Log.d(TAG, "Returning " + dataList.size() + " history items");
                callback.onSuccess(dataList);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Database error in history: " + databaseError.getMessage());
                callback.onError("Database error: " + databaseError.getMessage());
            }
        });
    }

    /**
     * Detect anomalies using ML API
     */
    private void detectAnomaliesWithML(SensorData sensorData, String athleteId, AnomalyPredictionClient.PredictionCallback callback) {
        Log.d(TAG, "Detecting anomalies with ML API for athlete: " + athleteId);
        predictionClient.predictAnomaly(athleteId, sensorData, callback);
    }

    /**
     * Detect anomalies using local thresholds as fallback
     * This is a simple implementation - in a real application,
     * this might involve more sophisticated algorithms
     */
    private void detectAnomaliesLocally(SensorData sensorData) {
        boolean anomalyDetected = false;

        // Check heart rate anomalies (too high or too low)
        if (sensorData.getHeartRate() > 180 || sensorData.getHeartRate() < 40) {
            Log.d(TAG, "Heart rate anomaly detected: " + sensorData.getHeartRate());
            anomalyDetected = true;
        }

        // Check temperature anomalies
        if (sensorData.getTemperature() > 39.0 || sensorData.getTemperature() < 35.0) {
            Log.d(TAG, "Temperature anomaly detected: " + sensorData.getTemperature());
            anomalyDetected = true;
        }

        // Check speed anomalies (sudden drop to zero while session is active)
        if (sensorData.getSpeed() == 0 && "active".equals(sensorData.getStatus())) {
            Log.d(TAG, "Speed anomaly detected: Athlete stopped while session active");
            anomalyDetected = true;
        }

        sensorData.setAnomalyDetected(anomalyDetected);
    }
}