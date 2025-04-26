package com.example.saferun.ui.coach;

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
import com.example.saferun.data.model.User;
import com.example.saferun.data.repository.TeamRequestRepository;
import com.example.saferun.data.repository.UserRepository;
import com.example.saferun.ui.auth.LoginActivity;

public class CoachDashboardActivity extends AppCompatActivity {

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
            // Will be implemented in future
            Toast.makeText(this, "View Run Sessions clicked", Toast.LENGTH_SHORT).show();
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
    protected void onResume() {
        super.onResume();
        // Refresh user data when returning to the dashboard
        loadUserData();
    }
}