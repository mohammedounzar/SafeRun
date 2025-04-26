package com.example.saferun.data.repository;

import android.util.Log;

import com.example.saferun.data.firebase.FirebaseAuthManager;
import com.example.saferun.data.firebase.FirestoreManager;
import com.example.saferun.data.model.RunSession;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RunSessionRepository {
    private static final String TAG = "RunSessionRepository";

    private FirebaseFirestore db;
    private FirebaseAuthManager authManager;
    private static RunSessionRepository instance;

    private static final String RUN_SESSIONS_COLLECTION = "runSessions";

    private RunSessionRepository() {
        db = FirebaseFirestore.getInstance();
        authManager = FirebaseAuthManager.getInstance();
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

        // Add athletes to the session
        for (String athleteId : athleteIds) {
            session.addAthlete(athleteId);
        }

        // Convert session to a map for Firestore
        Map<String, Object> sessionMap = convertSessionToMap(session);

        Log.d(TAG, "Creating run session with ID: " + sessionId);
        db.collection(RUN_SESSIONS_COLLECTION).document(sessionId)
                .set(sessionMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Run session created successfully");
                    callback.onSuccess(session);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating run session", e);
                    callback.onError("Failed to create session: " + e.getMessage());
                });
    }

    public void getRunSession(String sessionId, RunSessionCallback callback) {
        db.collection(RUN_SESSIONS_COLLECTION).document(sessionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        RunSession session = convertDocumentToSession(documentSnapshot);
                        callback.onSuccess(session);
                    } else {
                        callback.onError("Session not found");
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onError("Error getting session: " + e.getMessage());
                });
    }

    public void getSessionsForCoach(RunSessionsCallback callback) {
        String coachId = authManager.getCurrentUserId();
        if (coachId == null) {
            callback.onError("You must be logged in to view sessions");
            return;
        }

        db.collection(RUN_SESSIONS_COLLECTION)
                .whereEqualTo("coachId", coachId)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<RunSession> sessions = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        RunSession session = convertDocumentToSession(document);
                        sessions.add(session);
                    }
                    callback.onSuccess(sessions);
                })
                .addOnFailureListener(e -> {
                    callback.onError("Error getting sessions: " + e.getMessage());
                });
    }

    public void getSessionsForAthlete(String athleteId, RunSessionsCallback callback) {
        db.collection(RUN_SESSIONS_COLLECTION)
                .whereArrayContains("athleteIds", athleteId)  // Using a helper field to query
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<RunSession> sessions = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        RunSession session = convertDocumentToSession(document);
                        sessions.add(session);
                    }
                    callback.onSuccess(sessions);
                })
                .addOnFailureListener(e -> {
                    callback.onError("Error getting sessions: " + e.getMessage());
                });
    }

    public void updateRunSession(RunSession session, RunSessionCallback callback) {
        Map<String, Object> sessionMap = convertSessionToMap(session);

        db.collection(RUN_SESSIONS_COLLECTION).document(session.getId())
                .update(sessionMap)
                .addOnSuccessListener(aVoid -> {
                    callback.onSuccess(session);
                })
                .addOnFailureListener(e -> {
                    callback.onError("Error updating session: " + e.getMessage());
                });
    }

    public void deleteRunSession(String sessionId, OperationCallback callback) {
        db.collection(RUN_SESSIONS_COLLECTION).document(sessionId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    callback.onError("Error deleting session: " + e.getMessage());
                });
    }

    private Map<String, Object> convertSessionToMap(RunSession session) {
        Map<String, Object> sessionMap = new HashMap<>();
        sessionMap.put("id", session.getId());
        sessionMap.put("coachId", session.getCoachId());
        sessionMap.put("title", session.getTitle());
        sessionMap.put("description", session.getDescription());
        sessionMap.put("duration", session.getDuration());
        sessionMap.put("distance", session.getDistance());
        sessionMap.put("date", session.getDate());
        sessionMap.put("status", session.getStatus());
        sessionMap.put("athletes", session.getAthletes());

        // Create a helper field with athlete IDs for querying
        List<String> athleteIds = new ArrayList<>(session.getAthletes().keySet());
        sessionMap.put("athleteIds", athleteIds);

        return sessionMap;
    }

    private RunSession convertDocumentToSession(QueryDocumentSnapshot document) {
        return convertDocumentToSession(document.getId(), document.getData());
    }

    private RunSession convertDocumentToSession(com.google.firebase.firestore.DocumentSnapshot document) {
        return convertDocumentToSession(document.getId(), document.getData());
    }

    private RunSession convertDocumentToSession(String id, Map<String, Object> data) {
        RunSession session = new RunSession();
        session.setId(id);
        session.setCoachId((String) data.get("coachId"));
        session.setTitle((String) data.get("title"));
        session.setDescription((String) data.get("description"));

        if (data.get("duration") instanceof Long) {
            session.setDuration((Long) data.get("duration"));
        }

        if (data.get("distance") instanceof Double) {
            session.setDistance((Double) data.get("distance"));
        } else if (data.get("distance") instanceof Long) {
            session.setDistance(((Long) data.get("distance")).doubleValue());
        }

        if (data.get("date") instanceof Date) {
            session.setDate((Date) data.get("date"));
        } else if (data.get("date") instanceof com.google.firebase.Timestamp) {
            session.setDate(((com.google.firebase.Timestamp) data.get("date")).toDate());
        }

        session.setStatus((String) data.get("status"));

        // Convert athletes map
        Map<String, Map<String, Object>> athletes = new HashMap<>();
        if (data.get("athletes") instanceof Map) {
            Map<String, Object> athletesMap = (Map<String, Object>) data.get("athletes");
            for (Map.Entry<String, Object> entry : athletesMap.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    athletes.put(entry.getKey(), (Map<String, Object>) entry.getValue());
                }
            }
        }
        session.setAthletes(athletes);

        return session;
    }
}