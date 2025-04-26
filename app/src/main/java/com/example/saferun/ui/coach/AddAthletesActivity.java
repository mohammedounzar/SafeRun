package com.example.saferun.ui.coach;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.saferun.R;
import com.example.saferun.data.model.TeamRequest;
import com.example.saferun.data.model.User;
import com.example.saferun.data.repository.TeamRequestRepository;
import com.example.saferun.data.repository.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AddAthletesActivity extends AppCompatActivity implements AthleteRequestAdapter.OnAthleteRequestListener {

    private static final String TAG = "AddAthletesActivity";

    private ImageButton backButton;
    private EditText searchEditText;
    private RecyclerView athletesRecyclerView;
    private TextView emptyView;
    private ProgressBar progressBar;

    private UserRepository userRepository;
    private TeamRequestRepository teamRequestRepository;
    private AthleteRequestAdapter adapter;

    private List<User> allAthletes = new ArrayList<>();
    private List<User> filteredAthletes = new ArrayList<>();
    private User currentCoach;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_athletes);

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

        // Setup recycler view
        setupRecyclerView();

        // Load the current coach data
        loadCurrentCoach();
    }

    private void initViews() {
        backButton = findViewById(R.id.back_button);
        searchEditText = findViewById(R.id.search_edit_text);
        athletesRecyclerView = findViewById(R.id.athletes_recycler_view);
        emptyView = findViewById(R.id.empty_view);
        progressBar = findViewById(R.id.progress_bar);

        backButton.setOnClickListener(v -> finish());

        // Setup search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterAthletes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRecyclerView() {
        athletesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AthleteRequestAdapter(this, filteredAthletes, this);
        athletesRecyclerView.setAdapter(adapter);
    }

    private void loadCurrentCoach() {
        showLoading(true);

        userRepository.getCurrentUser(new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                Log.d(TAG, "Current coach loaded: " + user.getName());
                currentCoach = user;
                // Load athletes after coach data is loaded
                loadAthletes();
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error loading coach data: " + errorMessage);
                Toast.makeText(AddAthletesActivity.this, "Error loading coach data: " + errorMessage, Toast.LENGTH_SHORT).show();
                showLoading(false);
            }
        });
    }

    private void loadAthletes() {
        showLoading(true);

        userRepository.getAthletes(new UserRepository.UsersCallback() {
            @Override
            public void onSuccess(List<User> users) {
                Log.d(TAG, "Athletes loaded: " + users.size());
                allAthletes.clear();
                allAthletes.addAll(users);

                // Initialize filtered list with all athletes
                filteredAthletes.clear();
                filteredAthletes.addAll(allAthletes);

                adapter.updateData(filteredAthletes);
                updateEmptyView();

                // Load request statuses after athletes are loaded
                loadRequestStatuses();
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error loading athletes: " + errorMessage);
                Toast.makeText(AddAthletesActivity.this, "Error loading athletes: " + errorMessage, Toast.LENGTH_SHORT).show();
                showLoading(false);
                updateEmptyView();
            }
        });
    }

    private void loadRequestStatuses() {
        if (currentCoach == null) {
            Log.e(TAG, "Cannot load request statuses: current coach is null");
            showLoading(false);
            return;
        }

        teamRequestRepository.getRequestsByCoach(currentCoach.getUid(), new TeamRequestRepository.TeamRequestsCallback() {
            @Override
            public void onSuccess(List<TeamRequest> requests) {
                Log.d(TAG, "Team requests loaded: " + requests.size());
                // Create a map of athlete ID to request status
                Map<String, String> statusMap = new HashMap<>();
                for (TeamRequest request : requests) {
                    statusMap.put(request.getAthleteId(), request.getStatus());
                }

                // Update adapter with request statuses
                adapter.setRequestStatuses(statusMap);
                showLoading(false);
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error loading request statuses: " + errorMessage);
                Toast.makeText(AddAthletesActivity.this, "Error loading request statuses: " + errorMessage, Toast.LENGTH_SHORT).show();
                showLoading(false);
            }
        });
    }

    private void filterAthletes(String query) {
        if (query.isEmpty()) {
            filteredAthletes.clear();
            filteredAthletes.addAll(allAthletes);
        } else {
            String lowercaseQuery = query.toLowerCase();

            filteredAthletes.clear();
            filteredAthletes.addAll(allAthletes.stream()
                    .filter(athlete -> athlete.getName().toLowerCase().contains(lowercaseQuery))
                    .collect(Collectors.toList()));
        }

        adapter.updateData(filteredAthletes);
        updateEmptyView();
    }

    private void updateEmptyView() {
        if (filteredAthletes.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            athletesRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            athletesRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            athletesRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSendRequest(User athlete) {
        if (currentCoach == null) {
            Log.e(TAG, "Cannot send request: current coach is null");
            Toast.makeText(this, "Coach data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Sending request from coach " + currentCoach.getName() + " to athlete " + athlete.getName());

        teamRequestRepository.createTeamRequest(currentCoach, athlete, new TeamRequestRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Request sent successfully");
                Toast.makeText(AddAthletesActivity.this, "Request sent to " + athlete.getName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error sending request: " + errorMessage);
                Toast.makeText(AddAthletesActivity.this, "Error sending request: " + errorMessage, Toast.LENGTH_SHORT).show();
                // Reset status in adapter
                adapter.updateRequestStatus(athlete.getUid(), null);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when coming back to this screen
        if (currentCoach != null) {
            loadRequestStatuses();
        }
    }
}