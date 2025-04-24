package com.example.saferun.data.firebase;

import com.example.saferun.data.model.User;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
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

    // User operations
    public void saveUser(User user, FirestoreCallback callback) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("email", user.getEmail());
        userMap.put("name", user.getName());
        userMap.put("profilePic", user.getProfilePic());
        userMap.put("role", user.getRole());

        if (user.isAthlete()) {
            userMap.put("performanceScore", user.getPerformanceScore());
        }

        db.collection(USERS_COLLECTION).document(user.getUid())
                .set(userMap)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
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
}