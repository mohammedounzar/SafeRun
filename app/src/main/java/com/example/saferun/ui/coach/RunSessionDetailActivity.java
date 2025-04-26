package com.example.saferun.ui.coach;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.saferun.R;
import com.example.saferun.data.model.RunSession;
import com.example.saferun.data.model.User;
import com.example.saferun.data.repository.RunSessionRepository;
import com.example.saferun.data.repository.UserRepository;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RunSessionDetailActivity extends AppCompatActivity {

    private static final String TAG = "RunSessionDetailActivity";

    private Toolbar toolbar;
    private CollapsingToolbarLayout collapsingToolbar;
    private TextView sessionDescription, sessionDate, sessionDuration, sessionDistance, sessionStatus;
    private RecyclerView athletesRecyclerView;
    private TextView noAthletesText;
    private Button startSessionButton, editSessionButton, deleteSessionButton;
    private ProgressBar progressBar;

    private RunSessionRepository runSessionRepository;
    private UserRepository userRepository;
    private String sessionId;
    private RunSession session;
    private List<User> participatingAthletes = new ArrayList<>();
    private SessionAthleteListAdapter athleteAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run_session_detail);

        // Get session ID from intent
        sessionId = getIntent().getStringExtra("session_id");
        if (TextUtils.isEmpty(sessionId)) {
            Toast.makeText(this, "Error: No session selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize repositories
        runSessionRepository = RunSessionRepository.getInstance();
        userRepository = UserRepository.getInstance();

        // Initialize views
        initViews();

        // Set up toolbar
        setupToolbar();

        // Set up RecyclerView
        setupRecyclerView();

        // Load session data
        loadSessionData();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        sessionDescription = findViewById(R.id.session_description);
        sessionDate = findViewById(R.id.session_date);
        sessionDuration = findViewById(R.id.session_duration);
        sessionDistance = findViewById(R.id.session_distance);
        sessionStatus = findViewById(R.id.session_status);
        athletesRecyclerView = findViewById(R.id.athletes_recycler_view);
        noAthletesText = findViewById(R.id.no_athletes_text);
        startSessionButton = findViewById(R.id.start_session_button);
        editSessionButton = findViewById(R.id.edit_session_button);
        deleteSessionButton = findViewById(R.id.delete_session_button);
        progressBar = findViewById(R.id.progress_bar);

        // Set up click listeners
        startSessionButton.setOnClickListener(v -> startSession());
        editSessionButton.setOnClickListener(v -> editSession());
        deleteSessionButton.setOnClickListener(v -> confirmDeleteSession());
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        athleteAdapter = new SessionAthleteListAdapter(this, participatingAthletes);
        athletesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        athletesRecyclerView.setAdapter(athleteAdapter);
        athletesRecyclerView.setNestedScrollingEnabled(false);
    }

    private void loadSessionData() {
        showProgress(true);

        runSessionRepository.getRunSession(sessionId, new RunSessionRepository.RunSessionCallback() {
            @Override
            public void onSuccess(RunSession loadedSession) {
                session = loadedSession;
                updateUI();
                loadParticipatingAthletes();
            }

            @Override
            public void onError(String errorMessage) {
                showProgress(false);
                Toast.makeText(RunSessionDetailActivity.this,
                        "Error loading session: " + errorMessage, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void updateUI() {
        if (session == null) return;

        // Set session title
        collapsingToolbar.setTitle(session.getTitle());

        // Set session description
        if (!TextUtils.isEmpty(session.getDescription())) {
            sessionDescription.setText(session.getDescription());
        } else {
            sessionDescription.setText("No description provided");
        }

        // Set session date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault());
        if (session.getDate() != null) {
            sessionDate.setText(dateFormat.format(session.getDate()));
        } else {
            sessionDate.setText("Date not set");
        }

        // Set session duration
        sessionDuration.setText(session.getDuration() + " min");

        // Set session distance
        sessionDistance.setText(String.format(Locale.getDefault(), "%.1f km", session.getDistance()));

        // Set session status
        sessionStatus.setText(capitalizeFirstLetter(session.getStatus()));
        updateStatusColor();

        // Update button visibility based on status
        updateButtonVisibility();
    }

    private void loadParticipatingAthletes() {
        if (session == null || session.getAthletes() == null || session.getAthletes().isEmpty()) {
            showNoAthletesMessage(true);
            showProgress(false);
            return;
        }

        participatingAthletes.clear();
        final int[] loadedCount = {0};
        final int totalAthletes = session.getAthletes().size();

        for (String athleteId : session.getAthletes()) {
            userRepository.getUserById(athleteId, new UserRepository.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    loadedCount[0]++;
                    participatingAthletes.add(user);

                    // Check if all athletes are loaded
                    if (loadedCount[0] >= totalAthletes) {
                        athleteAdapter.notifyDataSetChanged();
                        showNoAthletesMessage(participatingAthletes.isEmpty());
                        showProgress(false);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    loadedCount[0]++;

                    // Check if all athletes are loaded
                    if (loadedCount[0] >= totalAthletes) {
                        athleteAdapter.notifyDataSetChanged();
                        showNoAthletesMessage(participatingAthletes.isEmpty());
                        showProgress(false);
                    }
                }
            });
        }
    }

    private void showNoAthletesMessage(boolean show) {
        if (show) {
            noAthletesText.setVisibility(View.VISIBLE);
            athletesRecyclerView.setVisibility(View.GONE);
        } else {
            noAthletesText.setVisibility(View.GONE);
            athletesRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void updateStatusColor() {
        int colorRes;
        switch (session.getStatus()) {
            case "scheduled":
                colorRes = R.color.accent_color;
                break;
            case "active":
                colorRes = R.color.active_color;
                break;
            case "completed":
                colorRes = R.color.success_color;
                break;
            default:
                colorRes = R.color.accent_color;
                break;
        }
        sessionStatus.setBackgroundTintList(ContextCompat.getColorStateList(this, colorRes));
    }

    private void updateButtonVisibility() {
        if ("scheduled".equals(session.getStatus())) {
            startSessionButton.setVisibility(View.VISIBLE);
            startSessionButton.setText("Start Session");
            editSessionButton.setVisibility(View.VISIBLE);
            deleteSessionButton.setVisibility(View.VISIBLE);
        } else if ("active".equals(session.getStatus())) {
            startSessionButton.setVisibility(View.VISIBLE);
            startSessionButton.setText("Complete Session");
            editSessionButton.setVisibility(View.GONE);
            deleteSessionButton.setVisibility(View.GONE);
        } else {
            startSessionButton.setVisibility(View.GONE);
            editSessionButton.setVisibility(View.GONE);
            deleteSessionButton.setVisibility(View.VISIBLE);
        }
    }

    private void startSession() {
        if (session == null) return;

        String newStatus;
        String confirmMessage;
        String successMessage;

        if ("scheduled".equals(session.getStatus())) {
            newStatus = "active";
            confirmMessage = "Are you sure you want to start this session?";
            successMessage = "Session started successfully";
        } else if ("active".equals(session.getStatus())) {
            newStatus = "completed";
            confirmMessage = "Are you sure you want to complete this session?";
            successMessage = "Session completed successfully";
        } else {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Confirm Action")
                .setMessage(confirmMessage)
                .setPositiveButton("Yes", (dialog, which) -> {
                    showProgress(true);

                    // Update session status
                    session.setStatus(newStatus);

                    runSessionRepository.updateRunSession(session, new RunSessionRepository.RunSessionCallback() {
                        @Override
                        public void onSuccess(RunSession updatedSession) {
                            showProgress(false);
                            session = updatedSession;
                            updateUI();
                            Toast.makeText(RunSessionDetailActivity.this,
                                    successMessage, Toast.LENGTH_SHORT).show();

                            // If session is now active, navigate to live session screen
                            if ("active".equals(newStatus)) {
                                navigateToLiveSession();
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            showProgress(false);
                            Toast.makeText(RunSessionDetailActivity.this,
                                    "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void navigateToLiveSession() {
        Intent intent = new Intent(this, LiveSessionActivity.class);
        intent.putExtra("session_id", sessionId);
        startActivity(intent);
    }

    private void editSession() {
        if (session == null) return;

        // Navigate to edit screen (you would need to create this)
        Intent intent = new Intent(this, CreateSessionActivity.class);
        intent.putExtra("session_id", sessionId);
        intent.putExtra("is_edit", true);
        startActivity(intent);
    }

    private void confirmDeleteSession() {
        if (session == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Delete Session")
                .setMessage("Are you sure you want to delete this session? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    showProgress(true);

                    runSessionRepository.deleteRunSession(sessionId, new RunSessionRepository.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            showProgress(false);
                            Toast.makeText(RunSessionDetailActivity.this,
                                    "Session deleted successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            showProgress(false);
                            Toast.makeText(RunSessionDetailActivity.this,
                                    "Error deleting session: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}