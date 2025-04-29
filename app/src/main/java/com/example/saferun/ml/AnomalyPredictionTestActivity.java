package com.example.saferun.ml;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.saferun.R;
import com.example.saferun.data.model.SensorData;

/**
 * Test activity for trying out the anomaly prediction API directly
 * This is for development and testing purposes only
 */
public class AnomalyPredictionTestActivity extends AppCompatActivity {

    private static final String TAG = "AnomalyPredictionTest";

    private EditText athleteIdEditText;
    private EditText heartRateEditText;
    private EditText temperatureEditText;
    private EditText speedEditText;
    private Button predictButton;
    private TextView resultTextView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anomaly_prediction_test);

        // Initialize views
        athleteIdEditText = findViewById(R.id.athlete_id_edit_text);
        heartRateEditText = findViewById(R.id.heart_rate_edit_text);
        temperatureEditText = findViewById(R.id.temperature_edit_text);
        speedEditText = findViewById(R.id.speed_edit_text);
        predictButton = findViewById(R.id.predict_button);
        resultTextView = findViewById(R.id.result_text_view);
        progressBar = findViewById(R.id.progress_bar);

        // Set default values
        heartRateEditText.setText("75");
        temperatureEditText.setText("36.5");
        speedEditText.setText("0.0");

        // Set up predict button click listener
        predictButton.setOnClickListener(v -> makePrediction());
    }

    private void makePrediction() {
        // Get input values
        String athleteId = athleteIdEditText.getText().toString().trim();
        String heartRateStr = heartRateEditText.getText().toString().trim();
        String temperatureStr = temperatureEditText.getText().toString().trim();
        String speedStr = speedEditText.getText().toString().trim();

        // Validate inputs
        if (athleteId.isEmpty()) {
            athleteIdEditText.setError("Athlete ID is required");
            return;
        }

        int heartRate;
        double temperature;
        double speed;

        try {
            heartRate = Integer.parseInt(heartRateStr);
            temperature = Double.parseDouble(temperatureStr);
            speed = Double.parseDouble(speedStr);
        } catch (NumberFormatException e) {
            resultTextView.setText("Invalid input values. Please check your entries.");
            return;
        }

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        resultTextView.setText("Sending prediction request...");
        predictButton.setEnabled(false);

        // Create sensor data object
        SensorData sensorData = new SensorData();
        sensorData.setHeartRate(heartRate);
        sensorData.setTemperature(temperature);
        sensorData.setSpeed(speed);
        sensorData.setTimestamp(System.currentTimeMillis());
        sensorData.setStatus("active");

        // Get prediction client
        AnomalyPredictionClient client = AnomalyPredictionClient.getInstance();

        // Make prediction
        client.predictAnomaly(athleteId, sensorData, new AnomalyPredictionClient.PredictionCallback() {
            @Override
            public void onSuccess(boolean isAnomaly) {
                // Update UI on main thread
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    predictButton.setEnabled(true);

                    if (isAnomaly) {
                        resultTextView.setText("PREDICTION RESULT: ANOMALY DETECTED\n\n" +
                                "The machine learning model has detected abnormal patterns in the provided vital signs.");
                        resultTextView.setTextColor(getResources().getColor(R.color.error_color));
                    } else {
                        resultTextView.setText("PREDICTION RESULT: NORMAL\n\n" +
                                "The machine learning model determined that the vital signs are within normal parameters.");
                        resultTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                // Update UI on main thread
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    predictButton.setEnabled(true);
                    resultTextView.setText("ERROR: " + errorMessage +
                            "\n\nPlease check your connection and API endpoint configuration.");
                    resultTextView.setTextColor(getResources().getColor(R.color.error_color));

                    Log.e(TAG, "Prediction error: " + errorMessage);
                });
            }
        });
    }
}