package com.example.saferun.ui.auth;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.saferun.R;
import com.example.saferun.data.model.User;
import com.example.saferun.data.repository.UserRepository;
import com.example.saferun.ui.athlete.AthleteDashboardActivity;
import com.example.saferun.ui.coach.CoachDashboardActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout nameLayout, emailLayout, passwordLayout, confirmPasswordLayout;
    private EditText nameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private RadioButton athleteRadioButton, coachRadioButton;
    private Button registerButton;
    private ImageButton backButton;
    private TextView loginTextView;
    private ProgressBar progressBar;
    private MaterialCardView athleteCard, coachCard;

    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        // Initialize repository
        userRepository = UserRepository.getInstance();

        // Initialize views
        initViews();

        // Set up click listeners
        setupClickListeners();
    }

    private void initViews() {
        // Back button
        backButton = findViewById(R.id.back_button);

        // EditTexts (now regular EditText instead of TextInputEditText)
        nameEditText = findViewById(R.id.name_edit_text);
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        confirmPasswordEditText = findViewById(R.id.confirm_password_edit_text);

        // Radio buttons
        athleteRadioButton = findViewById(R.id.athlete_radio_button);
        coachRadioButton = findViewById(R.id.coach_radio_button);

        // Button and TextView
        registerButton = findViewById(R.id.register_button);
        loginTextView = findViewById(R.id.login_text_view);

        // Progress bar
        progressBar = findViewById(R.id.progress_bar);

        // Cards for selection
        athleteCard = findViewById(R.id.athlete_card);
        coachCard = findViewById(R.id.coach_card);

        // Set default role selection
        athleteRadioButton.setChecked(true);
        athleteCard.setStrokeWidth(4);
        coachCard.setStrokeWidth(1);
    }


    private void setupClickListeners() {
        registerButton.setOnClickListener(v -> attemptRegister());

        loginTextView.setOnClickListener(v -> {
            finish();
        });

        backButton.setOnClickListener(v -> {
            finish();
        });

        // Role selection with visual feedback
        athleteCard.setOnClickListener(v -> {
            athleteRadioButton.setChecked(true);
            coachRadioButton.setChecked(false);
            athleteCard.setStrokeWidth(4);
            coachCard.setStrokeWidth(1);
        });

        coachCard.setOnClickListener(v -> {
            coachRadioButton.setChecked(true);
            athleteRadioButton.setChecked(false);
            coachCard.setStrokeWidth(4);
            athleteCard.setStrokeWidth(1);
        });
    }

    private void attemptRegister() {
        // Reset errors - now we'll need to handle errors differently
        // since we're not using TextInputLayout anymore

        // Clear any existing error states
        nameEditText.setError(null);
        emailEditText.setError(null);
        passwordEditText.setError(null);
        confirmPasswordEditText.setError(null);

        // Get values
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();
        String role = athleteRadioButton.isChecked() ? "athlete" : "coach";

        // Validate inputs
        boolean cancel = false;
        View focusView = null;

        // Check name
        if (TextUtils.isEmpty(name)) {
            nameEditText.setError(getString(R.string.error_field_required));
            focusView = nameEditText;
            cancel = true;
        }

        // Check email
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError(getString(R.string.error_field_required));
            focusView = emailEditText;
            cancel = true;
        } else if (!isEmailValid(email)) {
            emailEditText.setError(getString(R.string.error_invalid_email));
            focusView = emailEditText;
            cancel = true;
        }

        // Check password
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError(getString(R.string.error_field_required));
            focusView = passwordEditText;
            cancel = true;
        } else if (password.length() < 6) {
            passwordEditText.setError(getString(R.string.error_invalid_password));
            focusView = passwordEditText;
            cancel = true;
        }

        // Check password confirmation
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordEditText.setError(getString(R.string.error_field_required));
            focusView = confirmPasswordEditText;
            cancel = true;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError(getString(R.string.error_password_mismatch));
            focusView = confirmPasswordEditText;
            cancel = true;
        }

        if (cancel) {
            // There was an error; focus the first field with an error
            focusView.requestFocus();
        } else {
            // Show progress spinner and perform registration
            showProgress(true);
            performRegistration(name, email, password, role);
        }
    }

    private void performRegistration(String name, String email, String password, String role) {
        userRepository.registerUser(email, password, name, role, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                showProgress(false);

                // Navigate based on user role
                if (user.isAthlete()) {
                    startActivity(new Intent(RegisterActivity.this, AthleteDashboardActivity.class));
                } else if (user.isCoach()) {
                    startActivity(new Intent(RegisterActivity.this, CoachDashboardActivity.class));
                }

                Toast.makeText(RegisterActivity.this, R.string.registration_success, Toast.LENGTH_SHORT).show();
                finish(); // Close registration activity
            }

            @Override
            public void onError(String errorMessage) {
                showProgress(false);
                Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean isEmailValid(String email) {
        return email.contains("@") && email.contains(".");
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        registerButton.setEnabled(!show);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}