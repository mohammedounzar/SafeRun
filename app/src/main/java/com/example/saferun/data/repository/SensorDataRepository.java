package com.example.saferun.data.repository;

import android.util.Log;

import com.example.saferun.data.firebase.FirebaseAuthManager;
import com.example.saferun.data.model.SensorData;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SensorDataRepository {
    private static final String TAG = "SensorDataRepository";

    private FirebaseAuthManager authManager;
    private DatabaseReference sensorDataRef;
    private static SensorDataRepository instance;

    private SensorDataRepository() {
        authManager = FirebaseAuthManager.getInstance();
        sensorDataRef = FirebaseDatabase.getInstance().getReference("sensorData");
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
        Query query = sensorDataRef.child(sessionId).child(athleteId)
                .orderByKey()
                .limitToLast(1);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists() || !dataSnapshot.hasChildren()) {
                    callback.onError("No sensor data found");
                    return;
                }

                // Get the only child (latest data point)
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        SensorData sensorData = snapshot.getValue(SensorData.class);
                        if (sensorData != null) {
                            // Set the timestamp from the key
                            long timestamp = Long.parseLong(snapshot.getKey());
                            sensorData.setTimestamp(timestamp);

                            // Detect anomalies
                            detectAnomalies(sensorData);

                            callback.onSuccess(sensorData);
                        } else {
                            callback.onError("Invalid sensor data format");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing sensor data", e);
                        callback.onError("Error parsing sensor data: " + e.getMessage());
                    }
                    break; // We only need the first (and only) child
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
     * Get sensor data history for a specific athlete in a session
     */
    public void getSensorDataHistory(String sessionId, String athleteId, int limit, SensorDataListCallback callback) {
        Query query = sensorDataRef.child(sessionId).child(athleteId)
                .orderByKey()
                .limitToLast(limit);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    callback.onError("No sensor data found");
                    return;
                }

                List<SensorData> dataList = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        SensorData sensorData = snapshot.getValue(SensorData.class);
                        if (sensorData != null) {
                            // Set the timestamp from the key
                            long timestamp = Long.parseLong(snapshot.getKey());
                            sensorData.setTimestamp(timestamp);

                            // Detect anomalies
                            detectAnomalies(sensorData);

                            dataList.add(sensorData);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing sensor data", e);
                        // Continue with other entries even if one fails
                    }
                }

                // Sort by timestamp (ascending)
                Collections.sort(dataList, (data1, data2) ->
                        Long.compare(data1.getTimestamp(), data2.getTimestamp()));

                callback.onSuccess(dataList);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Database error: " + databaseError.getMessage());
                callback.onError("Database error: " + databaseError.getMessage());
            }
        });
    }

    /**
     * Detect anomalies in sensor data
     * This is a simple implementation - in a real application,
     * this might involve more sophisticated algorithms
     */
    private void detectAnomalies(SensorData sensorData) {
        boolean anomalyDetected = false;

        // Check heart rate anomalies (too high or too low)
        if (sensorData.getHeartRate() > 180 || sensorData.getHeartRate() < 40) {
            anomalyDetected = true;
        }

        // Check temperature anomalies
        if (sensorData.getTemperature() > 39.0 || sensorData.getTemperature() < 35.0) {
            anomalyDetected = true;
        }

        // Check speed anomalies (sudden drop to zero while session is active)
        if (sensorData.getSpeed() == 0 && "active".equals(sensorData.getStatus())) {
            anomalyDetected = true;
        }

        sensorData.setAnomalyDetected(anomalyDetected);
    }
}