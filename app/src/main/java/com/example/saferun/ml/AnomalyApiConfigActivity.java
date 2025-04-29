package com.example.saferun.ml;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.saferun.R;
import com.example.saferun.data.model.SensorData;

/**
 * Configuration activity for the Anomaly Detection ML API
 */
public class AnomalyApiConfigActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private Switch apiEnabledSwitch;
    private EditText apiUrlEditText;
    private Button testApiButton;
    private TextView apiStatusTextView;
    private TextView testResultTextView;
    private Button resetButton;
    private Button saveButton;

    private AnomalyDetectionModel detectionModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anomaly_api_config);

        // Initialize model
        detectionModel = AnomalyDetectionModel.getInstance(this);

        // Initialize views
        initViews();

        // Set initial values
        loadSettings();

        // Setup listeners
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("ML API Configuration");

        apiEnabledSwitch = findViewById(R.id.api_enabled_switch);
        apiUrlEditText = findViewById(R.id.api_url_edit_text);
        testApiButton = findViewById(R.id.test_api_button);
        apiStatusTextView = findViewById(R.id.api_status_text_view);
        testResultTextView = findViewById(R.id.test_result_text_view);
        resetButton = findViewById(R.id.reset_button);
        saveButton = findViewById(R.id.save_button);
    }

    private void loadSettings() {
        // Load current settings
        apiEnabledSwitch.setChecked(detectionModel.isApiEnabled());
        apiUrlEditText.setText(detectionModel.getApiUrl());
        updateApiStatus();
    }

    private void setupListeners() {
        // API enabled switch
        apiEnabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            apiUrlEditText.setEnabled(isChecked);
            testApiButton.setEnabled(isChecked);
        });

        // Test API button
        testApiButton.setOnClickListener(v -> testApi());

        // Reset button
        resetButton.setOnClickListener(v -> {
            apiEnabledSwitch.setChecked(true);
            apiUrlEditText.setText("http://192.168.11.109:5000/api/predict");
            detectionModel.resetFailureCounter();
            updateApiStatus();
            testResultTextView.setText("");
        });

        // Save button
        saveButton.setOnClickListener(v -> saveSettings());
    }

    private void updateApiStatus() {
        String status = detectionModel.getApiStatus();
        apiStatusTextView.setText("Status: " + status);
    }

    private void testApi() {
        // Temporarily update URL to test
        String testUrl = apiUrlEditText.getText().toString().trim();
        if (testUrl.isEmpty()) {
            apiUrlEditText.setError("URL is required");
            return;
        }

        // Disable button during test
        testApiButton.setEnabled(false);
        testResultTextView.setText("Testing API connection...");

        // Create test data
        SensorData testData = new SensorData();
        testData.setHeartRate(75);
        testData.setTemperature(36.5);
        testData.setSpeed(5.0);
        testData.setTimestamp(System.currentTimeMillis());
        testData.setStatus("active");

        // Update URL temporarily
        String originalUrl = detectionModel.getApiUrl();
        detectionModel.setApiUrl(testUrl);

        // Test API with temporary settings
        AnomalyPredictionClient client = AnomalyPredictionClient.getInstance();
        client.predictAnomaly("test_user", testData, new AnomalyPredictionClient.PredictionCallback() {
            @Override
            public void onSuccess(boolean isAnomaly) {
                // Restore original URL (save hasn't been clicked yet)
                detectionModel.setApiUrl(originalUrl);

                // Update UI on main thread
                runOnUiThread(() -> {
                    testApiButton.setEnabled(true);
                    testResultTextView.setText("API Test Successful!\nReceived response: " +
                            (isAnomaly ? "ANOMALY" : "NORMAL"));
                    detectionModel.resetFailureCounter();
                    updateApiStatus();
                });
            }

            @Override
            public void onError(String errorMessage) {
                // Restore original URL (save hasn't been clicked yet)
                detectionModel.setApiUrl(originalUrl);

                // Update UI on main thread
                runOnUiThread(() -> {
                    testApiButton.setEnabled(true);
                    testResultTextView.setText("API Test Failed: " + errorMessage);
                    updateApiStatus();
                });
            }
        });
    }

    private void saveSettings() {
        // Validate URL if API is enabled
        if (apiEnabledSwitch.isChecked()) {
            String url = apiUrlEditText.getText().toString().trim();
            if (url.isEmpty()) {
                apiUrlEditText.setError("URL is required");
                return;
            }

            // Save URL
            detectionModel.setApiUrl(url);
        }

        // Save enabled state
        detectionModel.setApiEnabled(apiEnabledSwitch.isChecked());

        // Clear cache to ensure settings take immediate effect
        detectionModel.clearCache();

        // Show success message and finish
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}