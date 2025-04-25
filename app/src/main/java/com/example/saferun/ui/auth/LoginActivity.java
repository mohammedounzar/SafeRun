package com.example.saferun.ui.auth;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout emailLayout, passwordLayout;
    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private TextView registerLinkTextView, forgotPasswordTextView;
    private ProgressBar progressBar;

    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize repository
        userRepository = UserRepository.getInstance();

        // Initialize views first
        initViews();

        // Set up click listeners
        setupClickListeners();

        // Check if user is already logged in AFTER views are initialized
        if (userRepository.isUserLoggedIn()) {
            navigateToAppropriateScreen();
        }
    }

    private void initViews() {
        // Note: we're no longer using TextInputLayout

        // EditTexts (now regular EditText instead of TextInputEditText)
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);

        // Button and TextViews
        loginButton = findViewById(R.id.login_button);
        registerLinkTextView = findViewById(R.id.register_link_text_view);
        forgotPasswordTextView = findViewById(R.id.forgot_password_text_view);

        // Progress bar
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> attemptLogin());

        registerLinkTextView.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        forgotPasswordTextView.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void attemptLogin() {
        // Reset any previous errors
        emailEditText.setError(null);
        passwordEditText.setError(null);

        // Get values
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        // Validate inputs
        boolean cancel = false;
        View focusView = null;

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

        if (cancel) {
            // There was an error; focus the first field with an error
            focusView.requestFocus();
        } else {
            // Show progress spinner and perform login
            showProgress(true);
            performLogin(email, password);
        }
    }


    private void performLogin(String email, String password) {
        userRepository.signIn(email, password, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                showProgress(false);

                // Navigate based on user role
                if (user.isAthlete()) {
                    startActivity(new Intent(LoginActivity.this, AthleteDashboardActivity.class));
                } else if (user.isCoach()) {
                    startActivity(new Intent(LoginActivity.this, CoachDashboardActivity.class));
                }

                finish(); // Close login activity
            }

            @Override
            public void onError(String errorMessage) {
                showProgress(false);
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void navigateToAppropriateScreen() {
        showProgress(true);

        userRepository.getCurrentUser(new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                showProgress(false);

                // Navigate based on user role
                if (user.isAthlete()) {
                    startActivity(new Intent(LoginActivity.this, AthleteDashboardActivity.class));
                } else if (user.isCoach()) {
                    startActivity(new Intent(LoginActivity.this, CoachDashboardActivity.class));
                }

                finish(); // Close login activity
            }

            @Override
            public void onError(String errorMessage) {
                showProgress(false);
                // Error retrieving user data, force login
                Toast.makeText(LoginActivity.this, "Please sign in again", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showForgotPasswordDialog() {
        // Get the email from the input field
        String email = emailEditText.getText().toString().trim();

        // Create forgot password dialog fragment
        ForgotPasswordDialogFragment dialog = ForgotPasswordDialogFragment.newInstance(email);
        dialog.show(getSupportFragmentManager(), "ForgotPasswordDialog");
    }

    private boolean isEmailValid(String email) {
        return email.contains("@") && email.contains(".");
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!show);
    }
}