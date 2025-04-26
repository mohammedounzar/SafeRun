package com.example.saferun.ui.coach;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;
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
import com.example.saferun.data.model.RunSession;
import com.example.saferun.data.model.User;
import com.example.saferun.data.repository.RunSessionRepository;
import com.example.saferun.data.repository.UserRepository;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CreateSessionActivity extends AppCompatActivity {
    private static final String TAG = "CreateSessionActivity";

    private Toolbar toolbar;
    private TextInputLayout titleLayout, descriptionLayout, durationLayout, distanceLayout, dateLayout;
    private TextInputEditText titleEditText, descriptionEditText, durationEditText, distanceEditText, dateEditText;
    private TextView athleteHeader, noAthletesText;
    private RecyclerView athletesRecyclerView;
    private Button createButton;
    private ProgressBar progressBar;

    private UserRepository userRepository;
    private RunSessionRepository runSessionRepository;
    private SessionAthleteAdapter athleteAdapter;
    private List<User> athletes = new ArrayList<>();
    private Calendar selectedDateTime = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault());
    private String teamId; // For future use to filter athletes by team

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_session);

        // Initialize athlete list
        athletes = new ArrayList<>();

        // Log for debugging
        Log.d("CreateSessionActivity", "onCreate called, initializing empty athlete list");

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get team ID from intent (if available)
        teamId = getIntent().getStringExtra("team_id");

        // Initialize repositories
        userRepository = UserRepository.getInstance();
        runSessionRepository = RunSessionRepository.getInstance();

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Setup date picker
        setupDatePicker();

        // Setup RecyclerView
        setupRecyclerView();

        // Load athletes
        loadAthletes();

        // Set up create button
        createButton.setOnClickListener(v -> validateAndCreateSession());
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        titleLayout = findViewById(R.id.title_layout);
        descriptionLayout = findViewById(R.id.description_layout);
        durationLayout = findViewById(R.id.duration_layout);
        distanceLayout = findViewById(R.id.distance_layout);
        dateLayout = findViewById(R.id.date_layout);

        titleEditText = findViewById(R.id.title_edit_text);
        descriptionEditText = findViewById(R.id.description_edit_text);
        durationEditText = findViewById(R.id.duration_edit_text);
        distanceEditText = findViewById(R.id.distance_edit_text);
        dateEditText = findViewById(R.id.date_edit_text);

        athleteHeader = findViewById(R.id.athlete_header);
        noAthletesText = findViewById(R.id.no_athletes_text);
        athletesRecyclerView = findViewById(R.id.athletes_recycler_view);

        createButton = findViewById(R.id.create_button);
        progressBar = findViewById(R.id.progress_bar);

        // Set initial date
        dateEditText.setText(dateFormat.format(selectedDateTime.getTime()));
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupDatePicker() {
        dateEditText.setOnClickListener(v -> showDateTimePicker());
    }

    private void showDateTimePicker() {
        // Show date picker
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDateTime.set(Calendar.YEAR, year);
                    selectedDateTime.set(Calendar.MONTH, month);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    // After date is set, show time picker
                    showTimePicker();
                },
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedDateTime.set(Calendar.MINUTE, minute);

                    // Update the EditText with the selected date and time
                    dateEditText.setText(dateFormat.format(selectedDateTime.getTime()));
                },
                selectedDateTime.get(Calendar.HOUR_OF_DAY),
                selectedDateTime.get(Calendar.MINUTE),
                false
        );
        timePickerDialog.show();
    }

    private void setupRecyclerView() {
        Log.d("CreateSessionActivity", "Setting up RecyclerView");
        athleteAdapter = new SessionAthleteAdapter(this, athletes);
        athletesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        athletesRecyclerView.setAdapter(athleteAdapter);
        athletesRecyclerView.setNestedScrollingEnabled(false);

        // Add this line to make the recycler view visible
        athletesRecyclerView.setVisibility(View.VISIBLE);
        noAthletesText.setVisibility(View.GONE);
    }

    private void loadAthletes() {
        showProgress(true);

        Log.d("CreateSessionActivity", "Starting to load athletes");

        userRepository.getAthletes(new UserRepository.UsersCallback() {
            @Override
            public void onSuccess(List<User> users) {
                showProgress(false);

                Log.d("CreateSessionActivity", "Athletes loaded successfully: " +
                        (users != null ? users.size() : 0) + " athletes");

                if (users != null && !users.isEmpty()) {
                    // Clear and update the list
                    athletes.clear();
                    athletes.addAll(users);

                    Log.d("CreateSessionActivity", "Athletes list updated, now contains " +
                            athletes.size() + " athletes");

                    // Update the adapter with the new list
                    athleteAdapter.updateAthletes(athletes);

                    showNoAthletesMessage(false);
                } else {
                    Log.d("CreateSessionActivity", "No athletes found");
                    showNoAthletesMessage(true);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("CreateSessionActivity", "Error loading athletes: " + errorMessage);
                showProgress(false);
                showNoAthletesMessage(true);
                Toast.makeText(CreateSessionActivity.this,
                        "Error loading athletes: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showNoAthletesMessage(boolean show) {
        Log.d("CreateSessionActivity", "showNoAthletesMessage: " + show +
                ", Athletes count: " + athletes.size());

        if (show) {
            // If there are no athletes, show the message and hide the RecyclerView
            noAthletesText.setVisibility(View.VISIBLE);
            athletesRecyclerView.setVisibility(View.GONE);
            Log.d("CreateSessionActivity", "No athletes message shown, RecyclerView hidden");
        } else {
            // If there are athletes, show the RecyclerView and hide the message
            noAthletesText.setVisibility(View.GONE);
            athletesRecyclerView.setVisibility(View.VISIBLE);

            // Check if adapter is set
            if (athletesRecyclerView.getAdapter() == null) {
                Log.e("CreateSessionActivity", "RecyclerView adapter is null!");
                athletesRecyclerView.setAdapter(athleteAdapter);
            }

            // Refresh the adapter
            athleteAdapter.notifyDataSetChanged();

            Log.d("CreateSessionActivity", "Athletes RecyclerView shown with " +
                    athletes.size() + " athletes, no athletes message hidden");
        }
    }

    private void validateAndCreateSession() {
        // Reset errors
        titleLayout.setError(null);
        durationLayout.setError(null);
        distanceLayout.setError(null);
        dateLayout.setError(null);

        // Get input values
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String durationStr = durationEditText.getText().toString().trim();
        String distanceStr = distanceEditText.getText().toString().trim();
        Date date = selectedDateTime.getTime();

        // Validate inputs
        boolean isValid = true;

        if (TextUtils.isEmpty(title)) {
            titleLayout.setError("Title is required");
            isValid = false;
        }

        long duration = 0;
        if (TextUtils.isEmpty(durationStr)) {
            durationLayout.setError("Duration is required");
            isValid = false;
        } else {
            try {
                duration = Long.parseLong(durationStr);
                if (duration <= 0) {
                    durationLayout.setError("Duration must be greater than 0");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                durationLayout.setError("Invalid number");
                isValid = false;
            }
        }

        double distance = 0;
        if (TextUtils.isEmpty(distanceStr)) {
            distanceLayout.setError("Distance is required");
            isValid = false;
        } else {
            try {
                distance = Double.parseDouble(distanceStr);
                if (distance <= 0) {
                    distanceLayout.setError("Distance must be greater than 0");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                distanceLayout.setError("Invalid number");
                isValid = false;
            }
        }

        // Get selected athletes
        List<String> selectedAthleteIds = athleteAdapter.getSelectedAthleteIds();
        if (selectedAthleteIds.isEmpty()) {
            Toast.makeText(this, "Please select at least one athlete", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (isValid) {
            createRunSession(title, description, duration, distance, date, selectedAthleteIds);
        }
    }

    private void createRunSession(String title, String description, long duration,
                                  double distance, Date date, List<String> athleteIds) {
        showProgress(true);

        runSessionRepository.createRunSession(title, description, duration, distance, date,
                athleteIds, new RunSessionRepository.RunSessionCallback() {
                    @Override
                    public void onSuccess(RunSession session) {
                        showProgress(false);
                        Toast.makeText(CreateSessionActivity.this,
                                "Run session created successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        showProgress(false);
                        Toast.makeText(CreateSessionActivity.this,
                                "Error creating session: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        createButton.setEnabled(!show);
    }
}