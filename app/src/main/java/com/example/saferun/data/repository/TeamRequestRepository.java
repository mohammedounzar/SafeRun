package com.example.saferun.data.repository;

import android.util.Log;

import com.example.saferun.data.firebase.FirestoreManager;
import com.example.saferun.data.model.TeamRequest;
import com.example.saferun.data.model.User;

import java.util.List;

public class TeamRequestRepository {

    private static final String TAG = "TeamRequestRepository";

    private FirestoreManager firestoreManager;
    private static TeamRequestRepository instance;

    private TeamRequestRepository() {
        firestoreManager = FirestoreManager.getInstance();
    }

    public static synchronized TeamRequestRepository getInstance() {
        if (instance == null) {
            instance = new TeamRequestRepository();
        }
        return instance;
    }

    public interface TeamRequestCallback {
        void onSuccess(TeamRequest request);
        void onError(String errorMessage);
    }

    public interface TeamRequestsCallback {
        void onSuccess(List<TeamRequest> requests);
        void onError(String errorMessage);
    }

    public interface OperationCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    // Create a new team request
    public void createTeamRequest(User coach, User athlete, OperationCallback callback) {
        Log.d(TAG, "Creating team request from coach " + coach.getName() + " to athlete " + athlete.getName());

        TeamRequest request = new TeamRequest(
                coach.getUid(),
                coach.getName(),
                athlete.getUid(),
                athlete.getName()
        );

        firestoreManager.createTeamRequest(request, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Team request created successfully");
                callback.onSuccess();
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error creating team request: " + errorMessage);
                callback.onError(errorMessage);
            }
        });
    }

    // Get all pending requests for an athlete
    public void getPendingRequestsForAthlete(String athleteId, TeamRequestsCallback callback) {
        Log.d(TAG, "Getting pending requests for athlete: " + athleteId);

        firestoreManager.getTeamRequestsByAthlete(athleteId, "pending", new FirestoreManager.TeamRequestsCallback() {
            @Override
            public void onTeamRequestsLoaded(List<TeamRequest> requests) {
                Log.d(TAG, "Pending requests loaded: " + (requests != null ? requests.size() : 0));
                callback.onSuccess(requests);
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error getting pending requests: " + errorMessage);
                callback.onError(errorMessage);
            }
        });
    }

    // Get all requests sent by a coach
    public void getRequestsByCoach(String coachId, TeamRequestsCallback callback) {
        Log.d(TAG, "Getting requests by coach: " + coachId);

        firestoreManager.getTeamRequestsByCoach(coachId, new FirestoreManager.TeamRequestsCallback() {
            @Override
            public void onTeamRequestsLoaded(List<TeamRequest> requests) {
                Log.d(TAG, "Coach requests loaded: " + (requests != null ? requests.size() : 0));
                callback.onSuccess(requests);
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error getting coach requests: " + errorMessage);
                callback.onError(errorMessage);
            }
        });
    }

    // Update a team request status (accept or reject)
    public void updateRequestStatus(String requestId, String newStatus, OperationCallback callback) {
        Log.d(TAG, "Updating request status: " + requestId + " to " + newStatus);

        firestoreManager.updateTeamRequestStatus(requestId, newStatus, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Request status updated successfully");
                callback.onSuccess();
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error updating request status: " + errorMessage);
                callback.onError(errorMessage);
            }
        });
    }

    // Delete a team request
    public void deleteRequest(String requestId, OperationCallback callback) {
        Log.d(TAG, "Deleting request: " + requestId);

        firestoreManager.deleteTeamRequest(requestId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Request deleted successfully");
                callback.onSuccess();
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error deleting request: " + errorMessage);
                callback.onError(errorMessage);
            }
        });
    }
}