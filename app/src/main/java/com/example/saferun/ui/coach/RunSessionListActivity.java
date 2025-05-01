package com.example.saferun.ui.coach;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.saferun.R;
import com.example.saferun.data.model.User;
import com.example.saferun.data.repository.RunSessionRepository;
import com.example.saferun.data.repository.UserRepository;
import com.example.saferun.ui.common.adapters.AthleteAdapter;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class RunSessionListActivity extends AppCompatActivity implements AthleteAdapter.OnAthleteClickListener {

    private Toolbar toolbar;
    private RecyclerView athletesRecyclerView;
    private TextView emptyView;
    private ProgressBar progressBar;
    private MaterialButton globalPerformanceButton;

    private UserRepository userRepository;
    private RunSessionRepository runSessionRepository;
    private AthleteAdapter athleteAdapter;
    private List<User> athletes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_run_session_list);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize repositories
        userRepository = UserRepository.getInstance();
        runSessionRepository = RunSessionRepository.getInstance();

        // Initialize views
        initViews();

        // Set up RecyclerView
        setupRecyclerView();

        // Load data
        loadAthletes();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Run Sessions");

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        athletesRecyclerView = findViewById(R.id.athletes_recycler_view);
        emptyView = findViewById(R.id.empty_view);
        progressBar = findViewById(R.id.progress_bar);
        globalPerformanceButton = findViewById(R.id.global_performance_button);

        // Set click listener for global performance button
        globalPerformanceButton.setOnClickListener(v -> {
            Intent intent = new Intent(RunSessionListActivity.this, GlobalPerformanceActivity.class);
            startActivity(intent);
        });
    }

    private void setupRecyclerView() {
        athleteAdapter = new AthleteAdapter(this, athletes, this);
        athletesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        athletesRecyclerView.setAdapter(athleteAdapter);
    }

    private void loadAthletes() {
        showProgress(true);

        // Get all athletes associated with this coach
        userRepository.getAthletes(new UserRepository.UsersCallback() {
            @Override
            public void onSuccess(List<User> users) {
                showProgress(false);

                athletes.clear();
                athletes.addAll(users);
                athleteAdapter.notifyDataSetChanged();

                // Show empty view if there are no athletes
                updateEmptyView();
            }

            @Override
            public void onError(String errorMessage) {
                showProgress(false);
                Toast.makeText(RunSessionListActivity.this,
                        "Error loading athletes: " + errorMessage,
                        Toast.LENGTH_SHORT).show();
                updateEmptyView();
            }
        });
    }

    private void updateEmptyView() {
        if (athletes.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            athletesRecyclerView.setVisibility(View.GONE);
            globalPerformanceButton.setEnabled(false);
        } else {
            emptyView.setVisibility(View.GONE);
            athletesRecyclerView.setVisibility(View.VISIBLE);
            globalPerformanceButton.setEnabled(true);
        }
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onAthleteClick(User athlete) {
        // Navigate to athlete performance history
        Intent intent = new Intent(this, AthletePerformanceActivity.class);
        intent.putExtra("athlete_id", athlete.getUid());
        intent.putExtra("athlete_name", athlete.getName());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this screen
        loadAthletes();
    }
}