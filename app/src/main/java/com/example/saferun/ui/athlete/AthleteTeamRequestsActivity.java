package com.example.saferun.ui.athlete;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;

import com.example.saferun.R;

public class AthleteTeamRequestsActivity extends AppCompatActivity {

    private static final String TAG = "AthleteTeamRequests";
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_athlete_team_requests);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Log.d(TAG, "Activity created");

        // Initialize views
        backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            Log.d(TAG, "Back button clicked, finishing activity");
            finish();
        });

        // Set up the fragment if this is the first creation
        if (savedInstanceState == null) {
            Log.d(TAG, "Setting up TeamRequestsFragment");
            TeamRequestsFragment fragment = TeamRequestsFragment.newInstance();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Activity resumed");
    }
}