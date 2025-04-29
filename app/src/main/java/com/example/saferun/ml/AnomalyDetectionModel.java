package com.example.saferun.ml;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.saferun.data.model.SensorData;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Model for detecting anomalies in athlete sensor data
 * Combines both local rule-based detection and ML prediction API
 */
public class AnomalyDetectionModel {
    private static final String TAG = "AnomalyDetectionModel";
    private static final String PREFS_NAME = "AnomalyDetectionPrefs";
    private static final String KEY_API_ENABLED = "ml_api_enabled";
    private static final String KEY_API_URL = "ml_api_url";
    private static final String KEY_API_FAILURES = "ml_api_failures";

    private static final int MAX_API_FAILURES = 5;  // After this many failures, fall back to local detection

    private static AnomalyDetectionModel instance;
    private final AnomalyPredictionClient predictionClient;
    private final Context context;
    private final SharedPreferences preferences;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

    // Cache of recent anomaly predictions to avoid repeated API calls for similar data
    private final Map<String, Boolean> predictionCache = new HashMap<>();

    public interface AnomalyCallback {
        void onDetectionComplete(boolean isAnomaly, String source);
    }

    private AnomalyDetectionModel(Context context) {
        this.context = context.getApplicationContext();
        this.predictionClient = AnomalyPredictionClient.getInstance();
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized AnomalyDetectionModel getInstance(Context context) {
        if (instance == null) {
            instance = new AnomalyDetectionModel(context);
        }
        return instance;
    }

    /**
     * Check if the sensor data represents an anomaly
     * @param athleteId athlete's ID
     * @param sensorData sensor data to check
     * @param callback callback to receive the result
     */
    public void detectAnomaly(@NonNull String athleteId, @NonNull SensorData sensorData,
                              @NonNull AnomalyCallback callback) {
        // Check if ML API is enabled
        if (isApiEnabled() && consecutiveFailures.get() < MAX_API_FAILURES) {
            // Check cache first for similar data to avoid excessive API calls
            String cacheKey = createCacheKey(athleteId, sensorData);
            if (predictionCache.containsKey(cacheKey)) {
                boolean cachedResult = predictionCache.get(cacheKey);
                Log.d(TAG, "Using cached prediction for athlete " + athleteId + ": " + cachedResult);
                callback.onDetectionComplete(cachedResult, "ML API (cached)");
                return;
            }

            // Not in cache, make API call
            predictionClient.predictAnomaly(athleteId, sensorData, new AnomalyPredictionClient.PredictionCallback() {
                @Override
                public void onSuccess(boolean isAnomaly) {
                    // Reset failure counter on success
                    consecutiveFailures.set(0);

                    // Cache the result
                    predictionCache.put(cacheKey, isAnomaly);

                    // Limit cache size
                    if (predictionCache.size() > 100) {
                        // Remove a random entry
                        String keyToRemove = predictionCache.keySet().iterator().next();
                        predictionCache.remove(keyToRemove);
                    }

                    // Return the result
                    callback.onDetectionComplete(isAnomaly, "ML API");
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "ML API error: " + errorMessage);

                    // Increment failure counter
                    int failures = consecutiveFailures.incrementAndGet();
                    Log.w(TAG, "Consecutive API failures: " + failures);

                    if (failures >= MAX_API_FAILURES) {
                        Log.w(TAG, "Too many API failures, switching to local detection");
                    }

                    // Fall back to local detection
                    boolean localResult = detectAnomalyLocally(sensorData);
                    callback.onDetectionComplete(localResult, "Local rules (API fallback)");
                }
            });
        } else {
            // Use local detection
            boolean localResult = detectAnomalyLocally(sensorData);
            callback.onDetectionComplete(localResult, "Local rules");
        }
    }

    /**
     * Detect anomalies using local rule-based approach
     * This serves as fallback when ML API is unavailable
     * @param sensorData sensor data to check
     * @return true if an anomaly is detected, false otherwise
     */
    private boolean detectAnomalyLocally(SensorData sensorData) {
        // Check heart rate anomalies (too high or too low)
        if (sensorData.getHeartRate() > 180 || sensorData.getHeartRate() < 40) {
            Log.d(TAG, "Local detection: Heart rate anomaly detected: " + sensorData.getHeartRate());
            return true;
        }

        // Check temperature anomalies
        if (sensorData.getTemperature() > 39.0 || sensorData.getTemperature() < 35.0) {
            Log.d(TAG, "Local detection: Temperature anomaly detected: " + sensorData.getTemperature());
            return true;
        }

        // Check speed anomalies (sudden drop to zero while session is active)
        if (sensorData.getSpeed() == 0.0 && "active".equals(sensorData.getStatus())) {
            Log.d(TAG, "Local detection: Speed anomaly detected: Athlete stopped while session active");
            return true;
        }

        return false;
    }

    /**
     * Create a cache key for the given athlete and sensor data
     * This helps avoid redundant API calls for similar data
     */
    private String createCacheKey(String athleteId, SensorData data) {
        // Round values to reduce sensitivity and increase cache hits
        int roundedHeartRate = roundToNearest(data.getHeartRate(), 5);
        double roundedTemp = roundToNearest(data.getTemperature(), 0.5);
        double roundedSpeed = roundToNearest(data.getSpeed(), 1.0);

        return athleteId + "_" + roundedHeartRate + "_" + roundedTemp + "_" + roundedSpeed;
    }

    /**
     * Round a value to the nearest multiple of the given unit
     */
    private int roundToNearest(int value, int unit) {
        return Math.round((float) value / unit) * unit;
    }

    /**
     * Round a value to the nearest multiple of the given unit
     */
    private double roundToNearest(double value, double unit) {
        return Math.round(value / unit) * unit;
    }

    /**
     * Check if the ML API is enabled
     */
    public boolean isApiEnabled() {
        return preferences.getBoolean(KEY_API_ENABLED, true);
    }

    /**
     * Enable or disable the ML API
     */
    public void setApiEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_API_ENABLED, enabled).apply();

        if (enabled) {
            // Reset failures counter when re-enabling
            consecutiveFailures.set(0);
        }
    }

    /**
     * Get the current API URL
     */
    public String getApiUrl() {
        return preferences.getString(KEY_API_URL, "http://localhost:5000/api/predict");
    }

    /**
     * Set the API URL
     */
    public void setApiUrl(String url) {
        preferences.edit().putString(KEY_API_URL, url).apply();
    }

    /**
     * Clear the prediction cache
     */
    public void clearCache() {
        predictionCache.clear();
    }

    /**
     * Get current API status summary
     */
    public String getApiStatus() {
        boolean enabled = isApiEnabled();
        int failures = consecutiveFailures.get();

        if (!enabled) {
            return "ML API Disabled";
        } else if (failures >= MAX_API_FAILURES) {
            return "ML API Failing (using local detection)";
        } else if (failures > 0) {
            return "ML API Warning (" + failures + " failures)";
        } else {
            return "ML API Active";
        }
    }

    /**
     * Reset API failure counter
     */
    public void resetFailureCounter() {
        consecutiveFailures.set(0);
    }
}