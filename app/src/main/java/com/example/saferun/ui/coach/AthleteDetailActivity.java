package com.example.saferun.ui.coach;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.saferun.R;
import com.example.saferun.data.model.User;
import com.example.saferun.data.repository.UserRepository;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;

import java.text.DecimalFormat;

import de.hdodenhof.circleimageview.CircleImageView;

public class AthleteDetailActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private CollapsingToolbarLayout collapsingToolbar;
    private CircleImageView athleteImageView;
    private TextView athleteNameTextView;
    private TextView athleteEmailTextView;
    private TextView performanceScoreTextView;
    private TextView noSessionsTextView;
    private RecyclerView sessionsRecyclerView;
    private FrameLayout chartContainer;
    private MaterialButton viewAllSessionsButton;
    private MaterialButton analyzePerformanceButton;
    private MaterialButton messageAthleteButton;
    private MaterialButton createTrainingPlanButton;
    private MaterialButton removeAthleteButton;
    private ProgressBar progressBar;

    private UserRepository userRepository;
    private String athleteId;
    private User athlete;
    private DecimalFormat scoreFormat = new DecimalFormat("#.#");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_athlete_detail);

        // Get athlete ID from intent
        athleteId = getIntent().getStringExtra("athlete_id");
        if (TextUtils.isEmpty(athleteId)) {
            Toast.makeText(this, "Error: No athlete selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize repository
        userRepository = UserRepository.getInstance();

        // Initialize views
        initViews();

        // Set up toolbar
        setupToolbar();

        // Load athlete data
        loadAthleteData();

        // Set up click listeners
        setupClickListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        athleteImageView = findViewById(R.id.athlete_image);
        athleteNameTextView = findViewById(R.id.athlete_name);
        athleteEmailTextView = findViewById(R.id.athlete_email);
        performanceScoreTextView = findViewById(R.id.performance_score_label);
        noSessionsTextView = findViewById(R.id.no_sessions_text);
        sessionsRecyclerView = findViewById(R.id.sessions_recycler_view);
        chartContainer = findViewById(R.id.chart_container);
        viewAllSessionsButton = findViewById(R.id.view_all_sessions_button);
        analyzePerformanceButton = findViewById(R.id.analyze_performance_button);
        messageAthleteButton = findViewById(R.id.message_athlete_button);
        createTrainingPlanButton = findViewById(R.id.create_training_plan_button);
        removeAthleteButton = findViewById(R.id.remove_athlete_button);
        progressBar = findViewById(R.id.progress_bar);

        // Setup RecyclerView
        sessionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Currently no sessions to display
        noSessionsTextView.setVisibility(View.VISIBLE);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadAthleteData() {
        showProgress(true);

        userRepository.getUserById(athleteId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                showProgress(false);
                athlete = user;
                updateUI(user);
            }

            @Override
            public void onError(String errorMessage) {
                showProgress(false);
                Toast.makeText(AthleteDetailActivity.this, "Error loading athlete: " + errorMessage, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void updateUI(User user) {
        // Set athlete name
        athleteNameTextView.setText(user.getName());
        collapsingToolbar.setTitle(user.getName());

        // Set athlete email
        athleteEmailTextView.setText(user.getEmail());

        // Set performance score
        double score = user.getPerformanceScore();
        performanceScoreTextView.setText("Performance: " + scoreFormat.format(score));

        // Adjust color based on performance score
        if (score < 40) {
            performanceScoreTextView.setBackgroundResource(R.drawable.rounded_score_background_red);
        } else if (score < 70) {
            performanceScoreTextView.setBackgroundResource(R.drawable.rounded_score_background_orange);
        } else {
            performanceScoreTextView.setBackgroundResource(R.drawable.rounded_score_background);
        }

        // Load profile image
        if (!TextUtils.isEmpty(user.getProfilePic())) {
            Glide.with(this)
                    .load(user.getProfilePic())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(athleteImageView);
        }
    }

    private void setupClickListeners() {
        viewAllSessionsButton.setOnClickListener(v -> {
            Toast.makeText(this, "View all sessions feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        analyzePerformanceButton.setOnClickListener(v -> {
            Toast.makeText(this, "Performance analysis feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        messageAthleteButton.setOnClickListener(v -> {
            Toast.makeText(this, "Messaging feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        createTrainingPlanButton.setOnClickListener(v -> {
            Toast.makeText(this, "Training plan feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        removeAthleteButton.setOnClickListener(v -> confirmRemoveAthlete());
    }

    private void confirmRemoveAthlete() {
        if (athlete == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Remove Athlete")
                .setMessage("Are you sure you want to remove " + athlete.getName() + " from your team?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    Toast.makeText(this, athlete.getName() + " has been removed from your team", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}