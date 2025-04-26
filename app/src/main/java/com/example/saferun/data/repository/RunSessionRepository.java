package com.example.saferun.data.repository;

import android.util.Log;

import com.example.saferun.data.firebase.FirebaseAuthManager;
import com.example.saferun.data.firebase.FirestoreManager;
import com.example.saferun.data.model.RunSession;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RunSessionRepository {
    private static final String TAG = "RunSessionRepository";
    private static final String COLLECTION_RUN_SESSIONS = "runSessions";

    private FirebaseAuthManager authManager;
    private FirebaseFirestore db;
    private static RunSessionRepository instance;

    private RunSessionRepository() {
        authManager = FirebaseAuthManager.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized RunSessionRepository getInstance() {
        if (instance == null) {
            instance = new RunSessionRepository();
        }
        return instance;
    }

    public interface RunSessionCallback {
        void onSuccess(RunSession session);
        void onError(String errorMessage);
    }

    public interface RunSessionsCallback {
        void onSuccess(List<RunSession> sessions);
        void onError(String errorMessage);
    }

    public interface OperationCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public void createRunSession(String title, String description, long duration,
                                 double distance, Date date, List<String> athleteIds,
                                 RunSessionCallback callback) {
        String coachId = authManager.getCurrentUserId();
        if (coachId == null) {
            callback.onError("You must be logged in to create a session");
            return;
        }

        String sessionId = UUID.randomUUID().toString();
        RunSession session = new RunSession(sessionId, coachId, title, description,
                duration, distance, date);

        // Add selected athletes to the session
        for (String athleteId : athleteIds) {
            session.addAthlete(athleteId);
        }

        // Convert session to Map for Firestore
        Map<String, Object> sessionMap = sessionToMap(session);

        // Save to Firestore
        db.collection(COLLECTION_RUN_SESSIONS).document(sessionId)
                .set(sessionMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Run session created with ID: " + sessionId);
                    callback.onSuccess(session);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating run session", e);
                    callback.onError("Failed to create run session: " + e.getMessage());
                });
    }

    public void getRunSession(String sessionId, RunSessionCallback callback) {
        db.collection(COLLECTION_RUN_SESSIONS).document(sessionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        RunSession session = mapToRunSession(documentSnapshot);
                        callback.onSuccess(session);
                    } else {
                        callback.onError("Run session not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting run session", e);
                    callback.onError("Failed to get run session: " + e.getMessage());
                });
    }

    public void getRunSessionsByCoach(RunSessionsCallback callback) {
        String coachId = authManager.getCurrentUserId();
        if (coachId == null) {
            callback.onError("You must be logged in to view sessions");
            return;
        }

        db.collection(COLLECTION_RUN_SESSIONS)
                .whereEqualTo("coachId", coachId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<RunSession> sessions = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        RunSession session = mapToRunSession(document);
                        sessions.add(session);
                    }
                    callback.onSuccess(sessions);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting run sessions", e);
                    callback.onError("Failed to get run sessions: " + e.getMessage());
                });
    }

    public void getRunSessionsByAthlete(String athleteId, RunSessionsCallback callback) {
        db.collection(COLLECTION_RUN_SESSIONS)
                .whereArrayContains("athleteIds", athleteId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<RunSession> sessions = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        RunSession session = mapToRunSession(document);
                        sessions.add(session);
                    }
                    callback.onSuccess(sessions);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting run sessions", e);
                    callback.onError("Failed to get run sessions: " + e.getMessage());
                });
    }

    public void updateSessionStatus(String sessionId, String status, OperationCallback callback) {
        db.collection(COLLECTION_RUN_SESSIONS).document(sessionId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Run session status updated to: " + status);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating run session status", e);
                    callback.onError("Failed to update session status: " + e.getMessage());
                });
    }

    public void updateAthleteStatus(String sessionId, String athleteId, String status,
                                    OperationCallback callback) {
        db.collection(COLLECTION_RUN_SESSIONS).document(sessionId)
                .update("athleteStatuses." + athleteId, status)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Athlete status updated to: " + status);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating athlete status", e);
                    callback.onError("Failed to update athlete status: " + e.getMessage());
                });
    }

    public void updateRunSession(RunSession session, RunSessionCallback callback) {
        if (session == null || session.getId() == null) {
            callback.onError("Invalid session data");
            return;
        }

        // Convert session to Map for Firestore
        Map<String, Object> sessionMap = sessionToMap(session);

        // Update in Firestore
        db.collection(COLLECTION_RUN_SESSIONS).document(session.getId())
                .set(sessionMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Run session updated with ID: " + session.getId());
                    callback.onSuccess(session);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating run session", e);
                    callback.onError("Failed to update run session: " + e.getMessage());
                });
    }

    public void deleteRunSession(String sessionId, OperationCallback callback) {
        if (sessionId == null || sessionId.isEmpty()) {
            callback.onError("Invalid session ID");
            return;
        }

        db.collection(COLLECTION_RUN_SESSIONS).document(sessionId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Run session deleted with ID: " + sessionId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting run session", e);
                    callback.onError("Failed to delete run session: " + e.getMessage());
                });
    }

    public void startSession(String sessionId, OperationCallback callback) {
        updateSessionStatus(sessionId, "active", callback);
    }

    public void completeSession(String sessionId, OperationCallback callback) {
        updateSessionStatus(sessionId, "completed", callback);
    }

    private Map<String, Object> sessionToMap(RunSession session) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", session.getId());
        map.put("coachId", session.getCoachId());
        map.put("title", session.getTitle());
        map.put("description", session.getDescription());
        map.put("duration", session.getDuration());
        map.put("distance", session.getDistance());
        map.put("date", session.getDate());
        map.put("status", session.getStatus());
        map.put("athleteStatuses", session.getAthleteStatuses());
        map.put("athletes", session.getAthletes());  // Add the athletes list
        return map;
    }

    private RunSession mapToRunSession(DocumentSnapshot document) {
        RunSession session = new RunSession();
        session.setId(document.getId());
        session.setCoachId(document.getString("coachId"));
        session.setTitle(document.getString("title"));
        session.setDescription(document.getString("description"));

        // Handle numeric values
        if (document.contains("duration")) {
            session.setDuration(document.getLong("duration"));
        }
        if (document.contains("distance")) {
            session.setDistance(document.getDouble("distance"));
        }

        // Handle date
        if (document.contains("date")) {
            session.setDate(document.getDate("date"));
        }

        // Handle status
        session.setStatus(document.getString("status"));

        // Handle athlete statuses
        Map<String, String> athleteStatuses = new HashMap<>();
        Map<String, Object> athleteStatusesMap = (Map<String, Object>) document.get("athleteStatuses");
        if (athleteStatusesMap != null) {
            for (Map.Entry<String, Object> entry : athleteStatusesMap.entrySet()) {
                athleteStatuses.put(entry.getKey(), (String) entry.getValue());
            }
        }
        session.setAthleteStatuses(athleteStatuses);

        // Handle athletes list
        List<String> athletes = (List<String>) document.get("athletes");
        if (athletes != null) {
            session.setAthletes(athletes);
        }

        return session;
    }
}