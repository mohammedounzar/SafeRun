package com.example.saferun.ui.athlete;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.saferun.R;
import com.example.saferun.data.model.TeamRequest;
import com.example.saferun.data.repository.TeamRequestRepository;
import com.example.saferun.data.repository.UserRepository;
import com.example.saferun.ui.common.adapters.TeamRequestAdapter;

import java.util.ArrayList;
import java.util.List;

public class TeamRequestsFragment extends Fragment implements TeamRequestAdapter.OnRequestActionListener {

    private static final String TAG = "TeamRequestsFragment";

    private RecyclerView requestsRecyclerView;
    private TextView emptyView;
    private ProgressBar progressBar;
    private TeamRequestAdapter adapter;
    private List<TeamRequest> requestsList = new ArrayList<>();

    private TeamRequestRepository requestRepository;
    private UserRepository userRepository;
    private String currentUserId;

    public TeamRequestsFragment() {
        // Required empty public constructor
    }

    public static TeamRequestsFragment newInstance() {
        Log.d(TAG, "Creating new instance of TeamRequestsFragment");
        return new TeamRequestsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Fragment onCreate");

        requestRepository = TeamRequestRepository.getInstance();
        userRepository = UserRepository.getInstance();
        currentUserId = userRepository.getCurrentUserId();

        Log.d(TAG, "Current user ID: " + currentUserId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "Fragment onCreateView");
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_team_requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "Fragment onViewCreated");

        // Initialize views
        requestsRecyclerView = view.findViewById(R.id.requests_recycler_view);
        emptyView = view.findViewById(R.id.empty_view);
        progressBar = view.findViewById(R.id.progress_bar);

        // Setup recycler view
        requestsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TeamRequestAdapter(getContext(), requestsList, true, this);
        requestsRecyclerView.setAdapter(adapter);

        // Load team requests
        loadTeamRequests();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "Fragment onResume");
        // Refresh data when fragment resumes
        loadTeamRequests();
    }

    private void loadTeamRequests() {
        Log.d(TAG, "Loading team requests for athlete: " + currentUserId);
        showLoading(true);

        requestRepository.getPendingRequestsForAthlete(currentUserId, new TeamRequestRepository.TeamRequestsCallback() {
            @Override
            public void onSuccess(List<TeamRequest> requests) {
                if (!isAdded()) {
                    Log.d(TAG, "Fragment not attached, skipping UI update");
                    return;
                }

                Log.d(TAG, "Team requests loaded: " + (requests != null ? requests.size() : 0));
                requestsList.clear();
                if (requests != null) {
                    requestsList.addAll(requests);
                }

                adapter.notifyDataSetChanged();
                updateEmptyView();
                showLoading(false);
            }

            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) {
                    Log.d(TAG, "Fragment not attached, skipping UI update");
                    return;
                }

                Log.e(TAG, "Error loading team requests: " + errorMessage);
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                updateEmptyView();
                showLoading(false);
            }
        });
    }

    private void updateEmptyView() {
        if (requestsList.isEmpty()) {
            Log.d(TAG, "No team requests found, showing empty view");
            emptyView.setVisibility(View.VISIBLE);
            requestsRecyclerView.setVisibility(View.GONE);
        } else {
            Log.d(TAG, "Team requests found, showing recycler view");
            emptyView.setVisibility(View.GONE);
            requestsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            requestsRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onAcceptRequest(TeamRequest request) {
        Log.d(TAG, "Accepting request from coach: " + request.getCoachName());
        showLoading(true);

        requestRepository.updateRequestStatus(request.getId(), "accepted", new TeamRequestRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) {
                    return;
                }

                Log.d(TAG, "Request accepted successfully");
                Toast.makeText(getContext(), "You've accepted the request from " + request.getCoachName(), Toast.LENGTH_SHORT).show();
                loadTeamRequests(); // Refresh list
            }

            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) {
                    return;
                }

                Log.e(TAG, "Error accepting request: " + errorMessage);
                Toast.makeText(getContext(), "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                showLoading(false);
            }
        });
    }

    @Override
    public void onRejectRequest(TeamRequest request) {
        Log.d(TAG, "Rejecting request from coach: " + request.getCoachName());
        showLoading(true);

        requestRepository.updateRequestStatus(request.getId(), "rejected", new TeamRequestRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) {
                    return;
                }

                Log.d(TAG, "Request rejected successfully");
                Toast.makeText(getContext(), "You've rejected the request from " + request.getCoachName(), Toast.LENGTH_SHORT).show();
                loadTeamRequests(); // Refresh list
            }

            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) {
                    return;
                }

                Log.e(TAG, "Error rejecting request: " + errorMessage);
                Toast.makeText(getContext(), "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                showLoading(false);
            }
        });
    }
}