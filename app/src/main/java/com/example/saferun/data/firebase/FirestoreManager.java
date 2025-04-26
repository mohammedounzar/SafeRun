package com.example.saferun.data.firebase;

import android.util.Log;

import com.example.saferun.data.model.Team;
import com.example.saferun.data.model.TeamRequest;
import com.example.saferun.data.model.User;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreManager {
    private FirebaseFirestore db;
    private static FirestoreManager instance;

    // Collection references
    private static final String USERS_COLLECTION = "users";
    private static final String TEAMS_COLLECTION = "teams";
    private static final String RUN_SESSIONS_COLLECTION = "runSessions";
    private static final String SENSOR_DATA_COLLECTION = "sensorData";
    private static final String TEAM_REQUESTS_COLLECTION = "teamRequests";

    private FirestoreManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirestoreManager getInstance() {
        if (instance == null) {
            instance = new FirestoreManager();
        }
        return instance;
    }

    public interface FirestoreCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public interface UserCallback {
        void onUserLoaded(User user);
        void onError(String errorMessage);
    }

    public interface UsersCallback {
        void onUsersLoaded(List<User> users);
        void onError(String errorMessage);
    }

    public interface TeamRequestCallback {
        void onTeamRequestLoaded(TeamRequest request);
        void onError(String errorMessage);
    }

    public interface TeamRequestsCallback {
        void onTeamRequestsLoaded(List<TeamRequest> requests);
        void onError(String errorMessage);
    }

    // User operations
    public void saveUser(User user, FirestoreCallback callback) {
        if (user == null || user.getUid() == null) {
            Log.e("FirestoreManager", "Cannot save user: user or UID is null");
            callback.onError("Invalid user data");
            return;
        }

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("email", user.getEmail());
        userMap.put("name", user.getName());
        userMap.put("profilePic", user.getProfilePic());
        userMap.put("role", user.getRole());

        if (user.isAthlete()) {
            userMap.put("performanceScore", user.getPerformanceScore());
        }

        Log.d("FirestoreManager", "Attempting to save user: " + user.getUid() + " with role: " + user.getRole());

        db.collection(USERS_COLLECTION).document(user.getUid())
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreManager", "User saved successfully: " + user.getUid());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreManager", "Error saving user: " + e.getMessage(), e);
                    callback.onError(e.getMessage());
                });
    }

    public void getUserById(String userId, UserCallback callback) {
        db.collection(USERS_COLLECTION).document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshotToUser(documentSnapshot);
                        callback.onUserLoaded(user);
                    } else {
                        callback.onError("User not found");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getUsersByRole(String role, UsersCallback callback) {
        db.collection(USERS_COLLECTION)
                .whereEqualTo("role", role)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> users = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        users.add(documentSnapshotToUser(document));
                    }
                    callback.onUsersLoaded(users);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateUserField(String userId, String field, Object value, FirestoreCallback callback) {
        db.collection(USERS_COLLECTION).document(userId)
                .update(field, value)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Team operations
    public interface TeamCallback {
        void onTeamLoaded(Team team);
        void onError(String errorMessage);
    }

    public interface TeamsCallback {
        void onTeamsLoaded(List<Team> teams);
        void onError(String errorMessage);
    }

    public void createTeam(Team team, FirestoreCallback callback) {
        Map<String, Object> teamMap = new HashMap<>();
        teamMap.put("name", team.getName());
        teamMap.put("description", team.getDescription());
        teamMap.put("coachId", team.getCoachId());
        teamMap.put("athleteIds", team.getAthleteIds());
        teamMap.put("createdAt", team.getCreatedAt());

        DocumentReference teamRef;
        if (team.getId() != null && !team.getId().isEmpty()) {
            teamRef = db.collection(TEAMS_COLLECTION).document(team.getId());
        } else {
            teamRef = db.collection(TEAMS_COLLECTION).document();
            team.setId(teamRef.getId());
        }

        teamRef.set(teamMap)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getTeamById(String teamId, TeamCallback callback) {
        db.collection(TEAMS_COLLECTION).document(teamId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Team team = documentSnapshotToTeam(documentSnapshot);
                        callback.onTeamLoaded(team);
                    } else {
                        callback.onError("Team not found");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getTeamsByCoach(String coachId, TeamsCallback callback) {
        db.collection(TEAMS_COLLECTION)
                .whereEqualTo("coachId", coachId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Team> teams = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        teams.add(documentSnapshotToTeam(document));
                    }
                    callback.onTeamsLoaded(teams);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getTeamsByAthlete(String athleteId, TeamsCallback callback) {
        db.collection(TEAMS_COLLECTION)
                .whereArrayContains("athleteIds", athleteId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Team> teams = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        teams.add(documentSnapshotToTeam(document));
                    }
                    callback.onTeamsLoaded(teams);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void addAthleteToTeam(String teamId, String athleteId, FirestoreCallback callback) {
        db.collection(TEAMS_COLLECTION).document(teamId)
                .update("athleteIds", FieldValue.arrayUnion(athleteId))
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void removeAthleteFromTeam(String teamId, String athleteId, FirestoreCallback callback) {
        db.collection(TEAMS_COLLECTION).document(teamId)
                .update("athleteIds", FieldValue.arrayRemove(athleteId))
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void deleteTeam(String teamId, FirestoreCallback callback) {
        db.collection(TEAMS_COLLECTION).document(teamId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Team Request operations
    public void createTeamRequest(TeamRequest request, FirestoreCallback callback) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("coachId", request.getCoachId());
        requestMap.put("coachName", request.getCoachName());
        requestMap.put("athleteId", request.getAthleteId());
        requestMap.put("athleteName", request.getAthleteName());
        requestMap.put("status", request.getStatus());
        requestMap.put("timestamp", request.getTimestamp());

        Log.d("FirestoreManager", "Creating team request: Coach=" + request.getCoachId() +
                " to Athlete=" + request.getAthleteId());

        db.collection(TEAM_REQUESTS_COLLECTION)
                .add(requestMap)
                .addOnSuccessListener(documentReference -> {
                    Log.d("FirestoreManager", "Team request created successfully with ID: " +
                            documentReference.getId());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreManager", "Error creating team request: " + e.getMessage(), e);
                    callback.onError(e.getMessage());
                });
    }

    public void getTeamRequestById(String requestId, TeamRequestCallback callback) {
        db.collection(TEAM_REQUESTS_COLLECTION).document(requestId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        TeamRequest request = documentSnapshotToTeamRequest(documentSnapshot);
                        callback.onTeamRequestLoaded(request);
                    } else {
                        callback.onError("Team request not found");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getTeamRequestsByAthlete(String athleteId, String status, TeamRequestsCallback callback) {
        Log.d("FirestoreManager", "Fetching team requests for athlete: " + athleteId + " with status: " + status);

        db.collection(TEAM_REQUESTS_COLLECTION)
                .whereEqualTo("athleteId", athleteId)
                .whereEqualTo("status", status)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<TeamRequest> requests = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        TeamRequest request = documentSnapshotToTeamRequest(document);
                        requests.add(request);
                    }
                    Log.d("FirestoreManager", "Found " + requests.size() + " team requests for athlete");
                    callback.onTeamRequestsLoaded(requests);
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreManager", "Error fetching team requests: " + e.getMessage(), e);
                    callback.onError(e.getMessage());
                });
    }

    public void getTeamRequestsByCoach(String coachId, TeamRequestsCallback callback) {
        Log.d("FirestoreManager", "Fetching team requests for coach: " + coachId);

        db.collection(TEAM_REQUESTS_COLLECTION)
                .whereEqualTo("coachId", coachId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<TeamRequest> requests = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        TeamRequest request = documentSnapshotToTeamRequest(document);
                        requests.add(request);
                    }
                    Log.d("FirestoreManager", "Found " + requests.size() + " team requests for coach");
                    callback.onTeamRequestsLoaded(requests);
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreManager", "Error fetching team requests: " + e.getMessage(), e);
                    callback.onError(e.getMessage());
                });
    }

    public void updateTeamRequestStatus(String requestId, String newStatus, FirestoreCallback callback) {
        Log.d("FirestoreManager", "Updating team request status: " + requestId + " to " + newStatus);

        db.collection(TEAM_REQUESTS_COLLECTION).document(requestId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreManager", "Team request status updated successfully");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreManager", "Error updating team request status: " + e.getMessage(), e);
                    callback.onError(e.getMessage());
                });
    }

    public void deleteTeamRequest(String requestId, FirestoreCallback callback) {
        Log.d("FirestoreManager", "Deleting team request: " + requestId);

        db.collection(TEAM_REQUESTS_COLLECTION).document(requestId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreManager", "Team request deleted successfully");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreManager", "Error deleting team request: " + e.getMessage(), e);
                    callback.onError(e.getMessage());
                });
    }

    // Helper method to convert DocumentSnapshot to User object
    private User documentSnapshotToUser(DocumentSnapshot document) {
        User user = new User();
        user.setUid(document.getId());
        user.setEmail(document.getString("email"));
        user.setName(document.getString("name"));
        user.setProfilePic(document.getString("profilePic"));
        user.setRole(document.getString("role"));

        // Performance score is only for athletes
        if (user.isAthlete() && document.contains("performanceScore")) {
            Double score = document.getDouble("performanceScore");
            user.setPerformanceScore(score != null ? score : 0.0);
        }

        return user;
    }

    // Helper method to convert DocumentSnapshot to Team object
    private Team documentSnapshotToTeam(DocumentSnapshot document) {
        Team team = new Team();
        team.setId(document.getId());
        team.setName(document.getString("name"));
        team.setDescription(document.getString("description"));
        team.setCoachId(document.getString("coachId"));

        // Get athleteIds list
        List<String> athleteIds = (List<String>) document.get("athleteIds");
        if (athleteIds != null) {
            team.setAthleteIds(athleteIds);
        }

        // Get createdAt timestamp
        Long createdAt = document.getLong("createdAt");
        team.setCreatedAt(createdAt != null ? createdAt : 0);

        return team;
    }

    // Helper method to convert DocumentSnapshot to TeamRequest object
    private TeamRequest documentSnapshotToTeamRequest(DocumentSnapshot document) {
        TeamRequest request = new TeamRequest();
        request.setId(document.getId());
        request.setCoachId(document.getString("coachId"));
        request.setCoachName(document.getString("coachName"));
        request.setAthleteId(document.getString("athleteId"));
        request.setAthleteName(document.getString("athleteName"));
        request.setStatus(document.getString("status"));

        // Handle timestamp
        Date timestamp = document.getDate("timestamp");
        request.setTimestamp(timestamp != null ? timestamp : new Date());

        return request;
    }
}