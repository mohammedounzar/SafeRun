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
import com.example.saferun.data.model.User;
import com.example.saferun.data.repository.UserRepository;
import com.example.saferun.ui.common.adapters.AthleteAdapter;

import java.util.ArrayList;
import java.util.List;

public class AthleteListActivity extends AppCompatActivity implements AthleteAdapter.OnAthleteClickListener {

    private RecyclerView athletesRecyclerView;
    private TextView emptyView;
    private ProgressBar progressBar;
    private Toolbar toolbar;

    private AthleteAdapter athleteAdapter;
    private UserRepository userRepository;
    private List<User> athletes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_athlete_list);

        // Initialize repository
        userRepository = UserRepository.getInstance();

        // Initialize views
        initViews();

        // Set up the RecyclerView
        setupRecyclerView();

        // Load athletes
        loadAthletes();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up back button
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        athletesRecyclerView = findViewById(R.id.athletes_recycler_view);
        emptyView = findViewById(R.id.empty_view);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupRecyclerView() {
        athleteAdapter = new AthleteAdapter(this, athletes, this);
        athletesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        athletesRecyclerView.setAdapter(athleteAdapter);
    }

    private void loadAthletes() {
        showProgress(true);

        userRepository.getAthletes(new UserRepository.UsersCallback() {
            @Override
            public void onSuccess(List<User> users) {
                showProgress(false);

                athletes.clear();
                athletes.addAll(users);
                athleteAdapter.notifyDataSetChanged();

                // Show empty view if there are no athletes
                if (athletes.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                    athletesRecyclerView.setVisibility(View.GONE);
                } else {
                    emptyView.setVisibility(View.GONE);
                    athletesRecyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String errorMessage) {
                showProgress(false);
                Toast.makeText(AthleteListActivity.this, "Error loading athletes: " + errorMessage, Toast.LENGTH_SHORT).show();

                // Show empty view
                emptyView.setVisibility(View.VISIBLE);
                athletesRecyclerView.setVisibility(View.GONE);
            }
        });
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