package com.example.saferun.data.repository;

import com.example.saferun.data.firebase.FirebaseAuthManager;
import com.example.saferun.data.firebase.FirestoreManager;
import com.example.saferun.data.model.Team;

import java.util.List;
import java.util.UUID;

public class TeamRepository {
    private FirebaseAuthManager authManager;
    private FirestoreManager firestoreManager;
    private static TeamRepository instance;

    private TeamRepository() {
        authManager = FirebaseAuthManager.getInstance();
        firestoreManager = FirestoreManager.getInstance();
    }

    public static synchronized TeamRepository getInstance() {
        if (instance == null) {
            instance = new TeamRepository();
        }
        return instance;
    }

    public interface TeamCallback {
        void onSuccess(Team team);
        void onError(String errorMessage);
    }

    public interface TeamsCallback {
        void onSuccess(List<Team> teams);
        void onError(String errorMessage);
    }

    public interface OperationCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public void createTeam(String name, String description, TeamCallback callback) {
        String coachId = authManager.getCurrentUserId();
        if (coachId == null) {
            callback.onError("You must be logged in to create a team");
            return;
        }

        String teamId = UUID.randomUUID().toString();
        Team team = new Team(teamId, name, description, coachId);

        firestoreManager.createTeam(team, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess(team);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    public void getTeam(String teamId, TeamCallback callback) {
        firestoreManager.getTeamById(teamId, new FirestoreManager.TeamCallback() {
            @Override
            public void onTeamLoaded(Team team) {
                callback.onSuccess(team);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    public void getTeamsForCoach(TeamsCallback callback) {
        String coachId = authManager.getCurrentUserId();
        if (coachId == null) {
            callback.onError("You must be logged in to view teams");
            return;
        }

        firestoreManager.getTeamsByCoach(coachId, new FirestoreManager.TeamsCallback() {
            @Override
            public void onTeamsLoaded(List<Team> teams) {
                callback.onSuccess(teams);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    public void getTeamsForAthlete(String athleteId, TeamsCallback callback) {
        firestoreManager.getTeamsByAthlete(athleteId, new FirestoreManager.TeamsCallback() {
            @Override
            public void onTeamsLoaded(List<Team> teams) {
                callback.onSuccess(teams);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    public void addAthleteToTeam(String teamId, String athleteId, OperationCallback callback) {
        firestoreManager.addAthleteToTeam(teamId, athleteId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess();
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    public void removeAthleteFromTeam(String teamId, String athleteId, OperationCallback callback) {
        firestoreManager.removeAthleteFromTeam(teamId, athleteId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess();
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    public void deleteTeam(String teamId, OperationCallback callback) {
        firestoreManager.deleteTeam(teamId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess();
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    public void updateTeam(Team team, TeamCallback callback) {
        firestoreManager.createTeam(team, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess(team);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }
}