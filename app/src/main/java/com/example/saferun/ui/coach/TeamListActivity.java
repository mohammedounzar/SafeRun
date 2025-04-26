package com.example.saferun.ui.coach;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.saferun.R;
import com.example.saferun.data.model.Team;
import com.example.saferun.data.repository.TeamRepository;
import com.example.saferun.ui.common.adapters.TeamAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class TeamListActivity extends AppCompatActivity implements TeamAdapter.OnTeamClickListener {

    private RecyclerView teamsRecyclerView;
    private TextView emptyView;
    private ProgressBar progressBar;
    private Toolbar toolbar;
    private FloatingActionButton addTeamFab;

    private TeamAdapter teamAdapter;
    private TeamRepository teamRepository;
    private List<Team> teams = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_list);

        // Initialize repository
        teamRepository = TeamRepository.getInstance();

        // Initialize views
        initViews();

        // Set up the RecyclerView
        setupRecyclerView();

        // Load teams
        loadTeams();

        // Set up FloatingActionButton
        setupFab();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up back button
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        teamsRecyclerView = findViewById(R.id.teams_recycler_view);
        emptyView = findViewById(R.id.empty_view);
        progressBar = findViewById(R.id.progress_bar);
        addTeamFab = findViewById(R.id.fab_add_team);
    }

    private void setupRecyclerView() {
        teamAdapter = new TeamAdapter(this, teams, this);
        teamsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        teamsRecyclerView.setAdapter(teamAdapter);
    }

    private void loadTeams() {
        showProgress(true);

        teamRepository.getTeamsForCoach(new TeamRepository.TeamsCallback() {
            @Override
            public void onSuccess(List<Team> loadedTeams) {
                showProgress(false);

                teams.clear();
                teams.addAll(loadedTeams);
                teamAdapter.notifyDataSetChanged();

                // Show empty view if there are no teams
                if (teams.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                    teamsRecyclerView.setVisibility(View.GONE);
                } else {
                    emptyView.setVisibility(View.GONE);
                    teamsRecyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String errorMessage) {
                showProgress(false);
                Toast.makeText(TeamListActivity.this, "Error loading teams: " + errorMessage, Toast.LENGTH_SHORT).show();

                // Show empty view
                emptyView.setVisibility(View.VISIBLE);
                teamsRecyclerView.setVisibility(View.GONE);
            }
        });
    }

    private void setupFab() {
        addTeamFab.setOnClickListener(v -> {
            // Create a dialog to create a new team
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_team, null);

            // Create the dialog
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
            builder.setView(dialogView);

            androidx.appcompat.app.AlertDialog dialog = builder.create();
            dialog.show();

            // Get dialog views
            com.google.android.material.textfield.TextInputLayout teamNameLayout = dialogView.findViewById(R.id.team_name_text_input_layout);
            com.google.android.material.textfield.TextInputEditText teamNameEditText = dialogView.findViewById(R.id.team_name_edit_text);
            com.google.android.material.textfield.TextInputEditText teamDescriptionEditText = dialogView.findViewById(R.id.team_description_edit_text);
            android.widget.Button cancelButton = dialogView.findViewById(R.id.cancel_button);
            android.widget.Button createTeamButton = dialogView.findViewById(R.id.create_team_button);

            // Set click listeners
            cancelButton.setOnClickListener(view -> dialog.dismiss());

            createTeamButton.setOnClickListener(view -> {
                String teamName = teamNameEditText.getText().toString().trim();
                String teamDescription = teamDescriptionEditText.getText().toString().trim();

                // Validate input
                if (teamName.isEmpty()) {
                    teamNameLayout.setError("Team name is required");
                    return;
                }

                // Show loading indicator
                dialog.dismiss();
                showProgress(true);

                // Create the team
                teamRepository.createTeam(teamName, teamDescription, new TeamRepository.TeamCallback() {
                    @Override
                    public void onSuccess(Team team) {
                        // Add the new team to the list
                        teams.add(team);
                        teamAdapter.notifyItemInserted(teams.size() - 1);

                        // Update visibility
                        if (!teams.isEmpty()) {
                            emptyView.setVisibility(View.GONE);
                            teamsRecyclerView.setVisibility(View.VISIBLE);
                        }

                        showProgress(false);

                        // Show success message
                        Toast.makeText(TeamListActivity.this, "Team created successfully", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        showProgress(false);

                        // Show error message
                        Toast.makeText(TeamListActivity.this, "Failed to create team: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onTeamClick(Team team) {
        // Navigate to team detail screen
        Intent intent = new Intent(this, TeamDetailActivity.class);
        intent.putExtra("team_id", team.getId());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload teams when returning to this activity
        loadTeams();
    }
}