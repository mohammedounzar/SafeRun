package com.example.saferun.ml;

import android.util.Log;

import com.example.saferun.data.model.SensorData;

/**
 * Test class for demonstrating how to use the AnomalyPredictionClient
 */
public class AnomalyPredictionTest {
    private static final String TAG = "AnomalyPredictionTest";

    /**
     * Demonstrates how to use the prediction client
     */
    public static void testPrediction(String athleteId) {
        // Create sample sensor data
        SensorData sensorData = new SensorData();
        sensorData.setHeartRate(150); // Elevated heart rate
        sensorData.setTemperature(37.5); // Normal temperature
        sensorData.setSpeed(3.0); // Moderate speed
        sensorData.setTimestamp(System.currentTimeMillis());
        sensorData.setStatus("active");

        // Get prediction client instance
        AnomalyPredictionClient client = AnomalyPredictionClient.getInstance();

        // Make prediction request
        client.predictAnomaly(athleteId, sensorData, new AnomalyPredictionClient.PredictionCallback() {
            @Override
            public void onSuccess(boolean isAnomaly) {
                Log.d(TAG, "Prediction result: " + (isAnomaly ? "ANOMALY DETECTED" : "NORMAL"));

                // Here you would typically update the UI or take some action based on the result
                if (isAnomaly) {
                    // Example: trigger alert, update UI, etc.
                    Log.d(TAG, "Should trigger alert for athlete: " + athleteId);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Prediction error: " + errorMessage);

                // Handle error - perhaps fall back to local detection
                Log.d(TAG, "Falling back to local anomaly detection");
            }
        });
    }
}