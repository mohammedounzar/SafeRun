package com.example.saferun.ui.coach;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.saferun.R;
import com.example.saferun.data.model.Team;
import com.example.saferun.data.model.User;
import com.example.saferun.data.repository.TeamRepository;
import com.example.saferun.data.repository.UserRepository;
import com.example.saferun.ui.auth.LoginActivity;
import com.example.saferun.ui.common.adapters.AthleteAdapter;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class TeamDetailActivity extends AppCompatActivity implements AthleteAdapter.OnAthleteClickListener {

    private Toolbar toolbar;
    private CollapsingToolbarLayout collapsingToolbar;
    private TextView teamNameTextView;
    private TextView teamDescriptionTextView;
    private TextView noAthletesTextView;
    private TextView noSessionsTextView;
    private RecyclerView athletesRecyclerView;
    private MaterialButton addAthleteButton;
    private MaterialButton createSessionButton;
    private MaterialButton editTeamButton;
    private MaterialButton deleteTeamButton;
    private ProgressBar progressBar;

    private TeamRepository teamRepository;
    private UserRepository userRepository;
    private String teamId;
    private Team team;
    private List<User> athletes = new ArrayList<>();
    private AthleteAdapter athleteAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_detail);

        // Get team ID from intent
        teamId = getIntent().getStringExtra("team_id");
        if (TextUtils.isEmpty(teamId)) {
            Toast.makeText(this, "Error: No team selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize repositories
        teamRepository = TeamRepository.getInstance();
        userRepository = UserRepository.getInstance();

        // Initialize views
        initViews();

        // Set up toolbar
        setupToolbar();

        // Set up RecyclerView
        setupRecyclerView();

        // Load team data
        loadTeamData();

        // Set up click listeners
        setupClickListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        teamNameTextView = findViewById(R.id.team_name);
        teamDescriptionTextView = findViewById(R.id.team_description);
        noAthletesTextView = findViewById(R.id.no_athletes_text);
        noSessionsTextView = findViewById(R.id.no_sessions_text);
        athletesRecyclerView = findViewById(R.id.athletes_recycler_view);
        addAthleteButton = findViewById(R.id.add_athlete_button);
        createSessionButton = findViewById(R.id.create_session_button);
        editTeamButton = findViewById(R.id.edit_team_button);
        deleteTeamButton = findViewById(R.id.delete_team_button);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        athleteAdapter = new AthleteAdapter(this, athletes, this);
        athletesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        athletesRecyclerView.setAdapter(athleteAdapter);

        // Set nestedScrollingEnabled to false to avoid scrolling conflicts
        athletesRecyclerView.setNestedScrollingEnabled(false);
    }

    private void loadTeamData() {
        showProgress(true);

        teamRepository.getTeam(teamId, new TeamRepository.TeamCallback() {
            @Override
            public void onSuccess(Team loadedTeam) {
                team = loadedTeam;
                updateUI();

                // Load team athletes
                loadTeamAthletes();
            }

            @Override
            public void onError(String errorMessage) {
                showProgress(false);
                Toast.makeText(TeamDetailActivity.this, "Error loading team: " + errorMessage, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void updateUI() {
        if (team == null) return;

        // Set team name
        teamNameTextView.setText(team.getName());
        collapsingToolbar.setTitle(team.getName());

        // Set team description
        if (TextUtils.isEmpty(team.getDescription())) {
            teamDescriptionTextView.setVisibility(View.GONE);
        } else {
            teamDescriptionTextView.setText(team.getDescription());
            teamDescriptionTextView.setVisibility(View.VISIBLE);
        }
    }

    private void loadTeamAthletes() {
        if (team == null || team.getAthleteIds() == null || team.getAthleteIds().isEmpty()) {
            // No athletes in team
            showNoAthletes(true);
            showProgress(false);
            return;
        }

        athletes.clear();
        final int[] loadedCount = {0};
        final int totalAthletes = team.getAthleteIds().size();

        for (String athleteId : team.getAthleteIds()) {
            userRepository.getUserById(athleteId, new UserRepository.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    loadedCount[0]++;
                    athletes.add(user);

                    // Check if all athletes are loaded
                    if (loadedCount[0] >= totalAthletes) {
                        athleteAdapter.notifyDataSetChanged();
                        showNoAthletes(athletes.isEmpty());
                        showProgress(false);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    loadedCount[0]++;

                    // Check if all athletes are loaded
                    if (loadedCount[0] >= totalAthletes) {
                        athleteAdapter.notifyDataSetChanged();
                        showNoAthletes(athletes.isEmpty());
                        showProgress(false);
                    }
                }
            });
        }
    }

    private void showNoAthletes(boolean show) {
        if (show) {
            noAthletesTextView.setVisibility(View.VISIBLE);
            athletesRecyclerView.setVisibility(View.GONE);
        } else {
            noAthletesTextView.setVisibility(View.GONE);
            athletesRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void setupClickListeners() {
        addAthleteButton.setOnClickListener(v -> handleAddAthlete());

        createSessionButton.setOnClickListener(v -> {
            // Navigate to create session activity
            Intent intent = new Intent(this, CreateSessionActivity.class);
            intent.putExtra("team_id", teamId);
            startActivity(intent);
        });

        editTeamButton.setOnClickListener(v -> handleEditTeam());

        deleteTeamButton.setOnClickListener(v -> confirmDeleteTeam());
    }

    private void handleAddAthlete() {
        // Inflate the dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_athlete, null);

        // Create the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Get dialog views
        com.google.android.material.textfield.TextInputLayout emailLayout = dialogView.findViewById(R.id.email_text_input_layout);
        com.google.android.material.textfield.TextInputEditText emailEditText = dialogView.findViewById(R.id.email_edit_text);
        android.widget.Button cancelButton = dialogView.findViewById(R.id.cancel_button);
        android.widget.Button sendInviteButton = dialogView.findViewById(R.id.send_invite_button);

        // Set click listeners
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        sendInviteButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();

            // Validate input
            if (email.isEmpty()) {
                emailLayout.setError("Email is required");
                return;
            }

            if (!isEmailValid(email)) {
                emailLayout.setError("Please enter a valid email");
                return;
            }

            dialog.dismiss();

            // Show confirmation and loading dialog
            showProgress(true);

            // Simulate inviting athlete
            new android.os.Handler().postDelayed(() -> {
                showProgress(false);

                // Show success dialog
                new AlertDialog.Builder(this)
                        .setTitle("Invitation Sent")
                        .setMessage("An invitation has been sent to " + email)
                        .setPositiveButton("OK", null)
                        .show();
            }, 1500);
        });
    }

    private void handleEditTeam() {
        if (team == null) return;

        // Inflate the dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_team, null);

        // Create the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Get dialog views
        com.google.android.material.textfield.TextInputLayout teamNameLayout = dialogView.findViewById(R.id.team_name_text_input_layout);
        com.google.android.material.textfield.TextInputEditText teamNameEditText = dialogView.findViewById(R.id.team_name_edit_text);
        com.google.android.material.textfield.TextInputEditText teamDescriptionEditText = dialogView.findViewById(R.id.team_description_edit_text);
        android.widget.Button cancelButton = dialogView.findViewById(R.id.cancel_button);
        android.widget.Button createTeamButton = dialogView.findViewById(R.id.create_team_button);

        // Update dialog UI for editing
        createTeamButton.setText("Update Team");

        // Pre-fill current team info
        teamNameEditText.setText(team.getName());
        if (team.getDescription() != null) {
            teamDescriptionEditText.setText(team.getDescription());
        }

        // Set click listeners
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        createTeamButton.setOnClickListener(v -> {
            String teamName = teamNameEditText.getText().toString().trim();
            String teamDescription = teamDescriptionEditText.getText().toString().trim();

            // Validate input
            if (teamName.isEmpty()) {
                teamNameLayout.setError("Team name is required");
                return;
            }

            dialog.dismiss();
            showProgress(true);

            // Update the team object
            team.setName(teamName);
            team.setDescription(teamDescription);

            // Save to repository
            teamRepository.updateTeam(team, new TeamRepository.TeamCallback() {
                @Override
                public void onSuccess(Team updatedTeam) {
                    showProgress(false);

                    // Update UI
                    updateUI();

                    Toast.makeText(TeamDetailActivity.this, "Team updated successfully", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String errorMessage) {
                    showProgress(false);
                    Toast.makeText(TeamDetailActivity.this, "Error updating team: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void confirmDeleteTeam() {
        if (team == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Delete Team")
                .setMessage("Are you sure you want to delete the team \"" + team.getName() + "\"? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    showProgress(true);

                    teamRepository.deleteTeam(teamId, new TeamRepository.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            showProgress(false);
                            Toast.makeText(TeamDetailActivity.this, "Team deleted successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            showProgress(false);
                            Toast.makeText(TeamDetailActivity.this, "Error deleting team: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean isEmailValid(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onAthleteClick(User athlete) {
        // Navigate to athlete detail screen
        Intent intent = new Intent(this, AthleteDetailActivity.class);
        intent.putExtra("athlete_id", athlete.getUid());
        startActivity(intent);
    }
}