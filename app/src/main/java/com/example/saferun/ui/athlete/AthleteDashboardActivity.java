package com.example.saferun.ui.athlete;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.saferun.R;
import com.example.saferun.data.repository.UserRepository;
import com.example.saferun.ui.auth.LoginActivity;

public class AthleteDashboardActivity extends AppCompatActivity {

    private TextView userNameTextView;
    private Button myRunsButton, globalPerformanceButton, teamRequestsButton;
    private ImageButton logoutButton;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_athlete_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize repository
        userRepository = UserRepository.getInstance();

        // Initialize views
        initViews();

        // Set up click listeners
        setupClickListeners();

        // Load user name
        loadUserName();
    }

    private void initViews() {
        userNameTextView = findViewById(R.id.user_name_text);
        myRunsButton = findViewById(R.id.my_runs_button);
        globalPerformanceButton = findViewById(R.id.global_performance_button);
        teamRequestsButton = findViewById(R.id.team_requests_button);
        logoutButton = findViewById(R.id.logout_button);
    }

    private void setupClickListeners() {
        logoutButton.setOnClickListener(v -> performLogout());

        myRunsButton.setOnClickListener(v -> {
            // Navigate to the run history screen
            Intent intent = new Intent(AthleteDashboardActivity.this, AthleteRunHistoryActivity.class);
            startActivity(intent);
        });

        globalPerformanceButton.setOnClickListener(v -> {
            Toast.makeText(this, "Global Performance clicked", Toast.LENGTH_SHORT).show();
            // Navigation would be implemented here
        });

        teamRequestsButton.setOnClickListener(v -> {
            // Navigate to team requests activity
            Intent intent = new Intent(AthleteDashboardActivity.this, AthleteTeamRequestsActivity.class);
            startActivity(intent);
        });
    }

    private void loadUserName() {
        userRepository.getCurrentUser(new UserRepository.UserCallback() {
            @Override
            public void onSuccess(com.example.saferun.data.model.User user) {
                userNameTextView.setText("Welcome, " + user.getName());
            }

            @Override
            public void onError(String errorMessage) {
                userNameTextView.setText("Welcome, Athlete");
            }
        });
    }

    private void performLogout() {
        userRepository.signOut();
        Intent intent = new Intent(AthleteDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}