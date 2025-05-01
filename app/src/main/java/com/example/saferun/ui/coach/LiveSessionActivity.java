package com.example.saferun.ui.coach;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.saferun.R;
import com.example.saferun.data.model.RunSession;
import com.example.saferun.data.model.User;
import com.example.saferun.data.repository.RunSessionRepository;
import com.example.saferun.data.repository.UserRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LiveSessionActivity extends AppCompatActivity implements ParticipatingAthleteAdapter.OnAthleteClickListener {

    private static final String TAG = "LiveSessionActivity";
    private static final String EXTRA_SESSION_ID = "session_id";

    private Toolbar toolbar;
    private TextView sessionTitleTextView;
    private TextView sessionDetailsTextView;
    private TextView noAthletesTextView;
    private RecyclerView athletesRecyclerView;
    private Button endSessionButton;
    private ProgressBar progressBar;
    private ImageButton backButton;

    private RunSessionRepository runSessionRepository;
    private UserRepository userRepository;
    private ParticipatingAthleteAdapter athleteAdapter;

    private String sessionId;
    private RunSession currentSession;
    private List<User> participatingAthletes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_live_session);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get session ID from intent
        sessionId = getIntent().getStringExtra(EXTRA_SESSION_ID);
        if (sessionId == null) {
            Toast.makeText(this, "Error: No session selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize repositories
        runSessionRepository = RunSessionRepository.getInstance();
        userRepository = UserRepository.getInstance();

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Setup RecyclerView
        setupRecyclerView();

        // Load session data
        loadSessionData();
    }

    public static void start(AppCompatActivity activity, String sessionId) {
        Intent intent = new Intent(activity, LiveSessionActivity.class);
        intent.putExtra(EXTRA_SESSION_ID, sessionId);
        activity.startActivity(intent);
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        sessionTitleTextView = findViewById(R.id.session_title);
        sessionDetailsTextView = findViewById(R.id.session_details);
        noAthletesTextView = findViewById(R.id.no_athletes_text);
        athletesRecyclerView = findViewById(R.id.athletes_recycler_view);
        endSessionButton = findViewById(R.id.end_session_button);
        progressBar = findViewById(R.id.progress_bar);
        backButton = findViewById(R.id.back_button);

        // Set click listeners
        endSessionButton.setOnClickListener(v -> confirmEndSession());
        backButton.setOnClickListener(v -> onBackPressed());
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    private void setupRecyclerView() {
        athleteAdapter = new ParticipatingAthleteAdapter(this, participatingAthletes, this);
        athletesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        athletesRecyclerView.setAdapter(athleteAdapter);
    }

    private void loadSessionData() {
        showProgress(true);

        runSessionRepository.getRunSession(sessionId, new RunSessionRepository.RunSessionCallback() {
            @Override
            public void onSuccess(RunSession session) {
                currentSession = session;
                updateUI(session);
                loadParticipatingAthletes(session);
            }

            @Override
            public void onError(String errorMessage) {
                showProgress(false);
                Toast.makeText(LiveSessionActivity.this,
                        "Error loading session: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(RunSession session) {
        sessionTitleTextView.setText(session.getTitle());

        // Format session details: distance, duration, date
        String details = String.format("%.1f km • %d min",
                session.getDistance(),
                session.getDuration());
        sessionDetailsTextView.setText(details);
    }

    private void loadParticipatingAthletes(RunSession session) {
        List<String> athleteIds = new ArrayList<>(session.getAthleteStatuses().keySet());

        if (athleteIds.isEmpty()) {
            showNoAthletesMessage(true);
            showProgress(false);
            return;
        }

        // Reset counters for async loading
        final int[] loadedCount = {0};
        final int totalAthletes = athleteIds.size();

        // Clear current list
        participatingAthletes.clear();

        // Create a Set to track athletes we've already processed
        final Set<String> processedAthleteIds = new HashSet<>();

        // Load each athlete's data
        for (String athleteId : athleteIds) {
            // Skip if we've already processed this athlete
            if (processedAthleteIds.contains(athleteId)) {
                loadedCount[0]++;
                continue;
            }

            // Mark this athlete as processed
            processedAthleteIds.add(athleteId);

            userRepository.getUserById(athleteId, new UserRepository.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    // Add user and their status to our list
                    user.setSessionStatus(session.getAthleteStatuses().get(user.getUid()));
                    participatingAthletes.add(user);

                    // Check if all athletes are loaded
                    loadedCount[0]++;
                    if (loadedCount[0] >= totalAthletes) {
                        athleteAdapter.notifyDataSetChanged();
                        showNoAthletesMessage(participatingAthletes.isEmpty());
                        showProgress(false);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    // Still increment counter on error
                    loadedCount[0]++;
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
            noAthletesTextView.setVisibility(View.VISIBLE);
            athletesRecyclerView.setVisibility(View.GONE);
        } else {
            noAthletesTextView.setVisibility(View.GONE);
            athletesRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void confirmEndSession() {
        new AlertDialog.Builder(this)
                .setTitle("End Session")
                .setMessage("Are you sure you want to end this session?")
                .setPositiveButton("End Session", (dialog, which) -> endSession())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void endSession() {
        showProgress(true);

        runSessionRepository.updateSessionStatus(sessionId, "completed", new RunSessionRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                showProgress(false);
                Toast.makeText(LiveSessionActivity.this, "Session ended successfully", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                showProgress(false);
                Toast.makeText(LiveSessionActivity.this,
                        "Error ending session: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onAthleteClick(User athlete) {
        // Navigate to the athlete monitoring screen
        AthleteMonitoringActivity.start(this, sessionId, athlete.getUid(), athlete.getName());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this screen
        if (sessionId != null) {
            loadSessionData();
        }
    }
}