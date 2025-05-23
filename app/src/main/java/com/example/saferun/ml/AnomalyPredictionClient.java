package com.example.saferun.ml;

import android.util.Log;

import com.example.saferun.data.model.SensorData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Client for interacting with the anomaly prediction API
 */
public class AnomalyPredictionClient {
    private static final String TAG = "AnomalyPredictionClient";
    private static final String API_URL = "http://192.168.11.109:5000/api/predict";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final int SEQUENCE_LENGTH = 5;

    private static AnomalyPredictionClient instance;
    private final OkHttpClient client;

    // Sliding window of past sensor data for each athlete
    private final Map<String, LinkedList<SensorData>> athleteDataHistory = new HashMap<>();

    // Interface for receiving prediction results
    public interface PredictionCallback {
        void onSuccess(boolean isAnomaly);
        void onError(String errorMessage);
    }

    private AnomalyPredictionClient() {
        // Configure OkHttpClient with reasonable timeouts
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public static synchronized AnomalyPredictionClient getInstance() {
        if (instance == null) {
            instance = new AnomalyPredictionClient();
        }
        return instance;
    }

    /**
     * Predicts if the given sensor data represents an anomaly using a time sequence.
     * @param athleteId the ID of the athlete
     * @param sensorData the latest sensor data point
     * @param callback callback for receiving the prediction result
     */
    public void predictAnomaly(String athleteId, SensorData sensorData, PredictionCallback callback) {
        try {
            // Maintain a sliding window of sensor data
            athleteDataHistory.putIfAbsent(athleteId, new LinkedList<>());
            LinkedList<SensorData> history = athleteDataHistory.get(athleteId);
            history.addLast(sensorData);
            if (history.size() > SEQUENCE_LENGTH) {
                history.removeFirst();
            }

            // Wait until we have a full sequence
            if (history.size() < SEQUENCE_LENGTH) {
                Log.d(TAG, "Waiting for full sequence (need " + SEQUENCE_LENGTH + " points).");
                callback.onError("Not enough data yet for prediction.");
                return;
            }

            // Create JSON request
            JSONObject requestBody = new JSONObject();
            requestBody.put("user_id", athleteId);

            JSONArray dataArray = new JSONArray();
            for (SensorData dataPoint : history) {
                JSONObject jsonData = new JSONObject();
                jsonData.put("temperature", dataPoint.getTemperature());
                jsonData.put("speed", dataPoint.getSpeed());
                jsonData.put("heart_beat", dataPoint.getHeartRate());
                dataArray.put(jsonData);
            }

            requestBody.put("data", dataArray);

            // HTTP request
            Request request = new Request.Builder()
                    .url(API_URL)
                    .post(RequestBody.create(requestBody.toString(), JSON))
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "API request failed: " + e.getMessage());
                    callback.onError("Failed to communicate with prediction API: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        callback.onError("API returned error code: " + response.code());
                        return;
                    }

                    try {
                        String responseBody = response.body().string();
                        JSONObject result = new JSONObject(responseBody);
                        boolean isAnomaly = result.optBoolean("is_anomaly", false);

                        Log.d(TAG, "Prediction result for athlete " + athleteId + ": " + isAnomaly);
                        callback.onSuccess(isAnomaly);

                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing API response: " + e.getMessage());
                        callback.onError("Error parsing prediction result: " + e.getMessage());
                    }
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "Error creating request body: " + e.getMessage());
            callback.onError("Error creating API request: " + e.getMessage());
        }
    }
}
