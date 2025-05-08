package com.example.saferun.data.repository;

import android.util.Log;

import com.example.saferun.data.firebase.FirebaseAuthManager;
import com.example.saferun.data.firebase.FirestoreManager;
import com.example.saferun.data.model.SensorData;
import com.example.saferun.ml.AnomalyPredictionClient;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

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
    private FirestoreManager firestoreManager;
    private FirebaseFirestore db;

    private SensorDataRepository() {
        authManager = FirebaseAuthManager.getInstance();
        firestoreManager = FirestoreManager.getInstance();
        db = FirebaseFirestore.getInstance();
        Log.d(TAG, "Initialized SensorDataRepository");
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
            // Try to read timestamp directly from key
            String timestampKey = snapshot.getKey();
            if (timestampKey != null) {
                try {
                    // Some implementations store timestamp as key
                    sensorData.setTimestamp(Long.parseLong(timestampKey));
                    Log.d(TAG, "Using timestamp from key: " + timestampKey);
                } catch (NumberFormatException e) {
                    // Not a numeric timestamp key, try to read from value
                    if (snapshot.hasChild("timestamp")) {
                        Object timeValue = snapshot.child("timestamp").getValue();
                        if (timeValue instanceof Number) {
                            sensorData.setTimestamp(((Number) timeValue).longValue());
                            Log.d(TAG, "Using timestamp from value: " + sensorData.getTimestamp());
                        }
                    }
                }
            }

            // Try to read fields directly from the snapshot
            if (snapshot.hasChild("heart_rate")) {
                Object heartRateValue = snapshot.child("heart_rate").getValue();
                if (heartRateValue instanceof Number) {
                    sensorData.setHeartRate(((Number) heartRateValue).intValue());
                    Log.d(TAG, "Found heart rate: " + sensorData.getHeartRate());
                }
            } else if (snapshot.hasChild("heartRate")) {
                // Try alternative field name
                Object heartRateValue = snapshot.child("heartRate").getValue();
                if (heartRateValue instanceof Number) {
                    sensorData.setHeartRate(((Number) heartRateValue).intValue());
                    Log.d(TAG, "Found heart rate (alternative field): " + sensorData.getHeartRate());
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
            } else if (snapshot.hasChild("anomalyDetected")) {
                // Try alternative field name
                Object anomalyValue = snapshot.child("anomalyDetected").getValue();
                if (anomalyValue instanceof Boolean) {
                    sensorData.setAnomalyDetected((Boolean) anomalyValue);
                }
            }

            // If the snapshot itself is a Map, try to extract values directly
            if (snapshot.getValue() instanceof Map) {
                Map<String, Object> valueMap = (Map<String, Object>) snapshot.getValue();

                // Log all keys for debugging
                Log.d(TAG, "Map keys: " + valueMap.keySet());

                // Check all possible field names for heart rate
                for (String key : new String[]{"heart_rate", "heartRate", "heart-rate", "heartrate"}) {
                    if (valueMap.containsKey(key) && valueMap.get(key) instanceof Number) {
                        sensorData.setHeartRate(((Number) valueMap.get(key)).intValue());
                        Log.d(TAG, "Found heart rate from map with key '" + key + "': " + sensorData.getHeartRate());
                        break;
                    }
                }

                // Check all possible field names for temperature
                for (String key : new String[]{"temperature", "temp", "bodyTemp", "body_temp"}) {
                    if (valueMap.containsKey(key) && valueMap.get(key) instanceof Number) {
                        sensorData.setTemperature(((Number) valueMap.get(key)).doubleValue());
                        Log.d(TAG, "Found temperature from map with key '" + key + "': " + sensorData.getTemperature());
                        break;
                    }
                }

                // Check all possible field names for speed
                for (String key : new String[]{"speed", "velocity"}) {
                    if (valueMap.containsKey(key) && valueMap.get(key) instanceof Number) {
                        sensorData.setSpeed(((Number) valueMap.get(key)).doubleValue());
                        Log.d(TAG, "Found speed from map with key '" + key + "': " + sensorData.getSpeed());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while parsing sensor data fields: " + e.getMessage(), e);
        }

        return sensorData;
    }

    /**
     * Get sensor data history for a specific athlete in a session
     */
    // Modifications to SensorDataRepository.java to handle Firestore structure properly

    /**
     * Get sensor data history for a specific athlete in a session
     */
    // Method in SensorDataRepository that only retrieves real data from Firebase
    // Modified getSensorDataHistory method for SensorDataRepository.java
    public void getSensorDataHistory(String sessionId, String athleteId, int limit, SensorDataListCallback callback) {
        Log.d(TAG, "Getting sensor data history for session: " + sessionId + ", athlete: " + athleteId);

        // Build a query for the 'sensor_data' collection
        // filtered by session_id and athlete_id fields
        db.collection("sensor_data")
                .whereEqualTo("session_id", sessionId)
                .whereEqualTo("athlete_id", athleteId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        List<SensorData> dataList = new ArrayList<>();

                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            SensorData sensorData = new SensorData();

                            // Extract fields directly from the document
                            if (document.contains("heart_rate")) {
                                Long heartRate = document.getLong("heart_rate");
                                sensorData.setHeartRate(heartRate != null ? heartRate.intValue() : 0);
                            }

                            if (document.contains("temperature")) {
                                Double temperature = document.getDouble("temperature");
                                sensorData.setTemperature(temperature != null ? temperature : 0.0);
                            }

                            if (document.contains("speed")) {
                                Double speed = document.getDouble("speed");
                                sensorData.setSpeed(speed != null ? speed : 0.0);
                            }

                            if (document.contains("timestamp")) {
                                Long timestamp = document.getLong("timestamp");
                                sensorData.setTimestamp(timestamp != null ? timestamp : 0L);
                            }

                            if (document.contains("anomaly_detected")) {
                                Boolean anomalyDetected = document.getBoolean("anomaly_detected");
                                sensorData.setAnomalyDetected(anomalyDetected != null ? anomalyDetected : false);
                            }

                            sensorData.setSessionId(sessionId);
                            sensorData.setAthleteId(athleteId);

                            dataList.add(sensorData);
                        }

                        Log.d(TAG, "Retrieved " + dataList.size() + " sensor data points");
                        callback.onSuccess(dataList);
                    } else {
                        Log.w(TAG, "No sensor data found for session:" + sessionId + ", athlete:" + athleteId);
                        callback.onError("No sensor data found for this athlete and session");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting sensor data", e);
                    callback.onError("Error getting sensor data: " + e.getMessage());
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