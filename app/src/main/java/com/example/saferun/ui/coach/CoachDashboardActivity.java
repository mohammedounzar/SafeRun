package com.example.saferun.ui.coach;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.saferun.R;
import com.example.saferun.data.model.User;
import com.example.saferun.data.repository.TeamRequestRepository;
import com.example.saferun.data.repository.UserRepository;
import com.example.saferun.ml.AnomalyApiConfigActivity;
import com.example.saferun.ml.AnomalyPredictionTestActivity;
import com.example.saferun.ui.auth.LoginActivity;

public class CoachDashboardActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView userNameTextView;
    private Button createSessionButton, manageAthletesButton, viewSessionsButton;
    private ImageButton logoutButton;
    private UserRepository userRepository;
    private TeamRequestRepository teamRequestRepository;
    private User currentCoach;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_coach_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize repositories
        userRepository = UserRepository.getInstance();
        teamRequestRepository = TeamRequestRepository.getInstance();

        // Initialize views
        initViews();

        // Set up click listeners
        setupClickListeners();

        // Load user data
        loadUserData();
    }

    private void initViews() {
//        toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        userNameTextView = findViewById(R.id.user_name_text);
        createSessionButton = findViewById(R.id.create_session_button);
        manageAthletesButton = findViewById(R.id.manage_athletes_button);
        viewSessionsButton = findViewById(R.id.view_sessions_button);
        logoutButton = findViewById(R.id.logout_button);
    }

    private void setupClickListeners() {
        logoutButton.setOnClickListener(v -> performLogout());

        createSessionButton.setOnClickListener(v -> {
            Intent intent = new Intent(CoachDashboardActivity.this, CreateSessionActivity.class);
            startActivity(intent);
        });

        manageAthletesButton.setOnClickListener(v -> {
            Intent intent = new Intent(CoachDashboardActivity.this, AddAthletesActivity.class);
            startActivity(intent);
        });

        viewSessionsButton.setOnClickListener(v -> {
            // Navigate to the RunSessionListActivity
            Intent intent = new Intent(CoachDashboardActivity.this, RunSessionListActivity.class);
            startActivity(intent);
        });
    }

    private void loadUserData() {
        userRepository.getCurrentUser(new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                currentCoach = user;
                userNameTextView.setText("Welcome, " + user.getName());
            }

            @Override
            public void onError(String errorMessage) {
                userNameTextView.setText("Welcome, Coach");
                Toast.makeText(CoachDashboardActivity.this,
                        "Error loading profile: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performLogout() {
        userRepository.signOut();
        Intent intent = new Intent(CoachDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_coach_dashboard, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_ml_settings) {
            // Open ML API settings activity
            Intent intent = new Intent(this, AnomalyApiConfigActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_test_api) {
            // Open ML API test activity
            Intent intent = new Intent(this, AnomalyPredictionTestActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_settings) {
            // Open general settings (to be implemented)
            Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_about) {
            // Show about dialog
            showAboutDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showAboutDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("About SafeRun");
        builder.setMessage("SafeRun v1.0\n\n" +
                "An advanced running monitoring application with ML-powered anomaly detection " +
                "to ensure athlete safety during training sessions.\n\n" +
                "ML API Status: Active");
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh user data when returning to the dashboard
        loadUserData();
    }
}