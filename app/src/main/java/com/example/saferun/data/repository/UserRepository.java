package com.example.saferun.data.repository;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.example.saferun.data.firebase.FirebaseAuthManager;
import com.example.saferun.data.firebase.FirestoreManager;
import com.example.saferun.data.model.User;
import com.google.android.gms.tasks.OnCompleteListener;

import java.util.List;

public class UserRepository {
    private FirebaseAuthManager authManager;
    private FirestoreManager firestoreManager;
    private static UserRepository instance;

    private UserRepository() {
        authManager = FirebaseAuthManager.getInstance();
        firestoreManager = FirestoreManager.getInstance();
    }

    public static synchronized UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    public interface UserCallback {
        void onSuccess(User user);
        void onError(String errorMessage);
    }

    public interface UsersCallback {
        void onSuccess(List<User> users);
        void onError(String errorMessage);
    }

    public interface OperationCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public boolean isUserLoggedIn() {
        return authManager.isUserLoggedIn();
    }

    public String getCurrentUserId() {
        return authManager.getCurrentUserId();
    }

    public void registerUser(String email, String password, String name, String role, UserCallback callback) {
        authManager.registerUser(email, password, name, role, new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                callback.onSuccess(user);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    public void signIn(String email, String password, UserCallback callback) {
        authManager.signIn(email, password, new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                callback.onSuccess(user);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    public void signOut() {
        authManager.signOut();
    }

    public void getCurrentUser(UserCallback callback) {
        String userId = authManager.getCurrentUserId();
        if (userId != null) {
            firestoreManager.getUserById(userId, new FirestoreManager.UserCallback() {
                @Override
                public void onUserLoaded(User user) {
                    callback.onSuccess(user);
                }

                @Override
                public void onError(String errorMessage) {
                    callback.onError(errorMessage);
                }
            });
        } else {
            callback.onError("No user is currently logged in");
        }
    }

    public void getUserById(String userId, UserCallback callback) {
        firestoreManager.getUserById(userId, new FirestoreManager.UserCallback() {
            @Override
            public void onUserLoaded(User user) {
                callback.onSuccess(user);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    public void getAthletes(UsersCallback callback) {
        firestoreManager.getUsersByRole("athlete", new FirestoreManager.UsersCallback() {
            @Override
            public void onUsersLoaded(List<User> users) {
                callback.onSuccess(users);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    public void getCoaches(UsersCallback callback) {
        firestoreManager.getUsersByRole("coach", new FirestoreManager.UsersCallback() {
            @Override
            public void onUsersLoaded(List<User> users) {
                callback.onSuccess(users);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    public void updateUserProfile(String name, Uri profilePicUri, OperationCallback callback) {
        authManager.updateUserProfile(name, profilePicUri, task -> {
            if (task.isSuccessful()) {
                String userId = authManager.getCurrentUserId();

                // Update name in Firestore if provided
                if (name != null && !name.isEmpty()) {
                    firestoreManager.updateUserField(userId, "name", name, new FirestoreManager.FirestoreCallback() {
                        @Override
                        public void onSuccess() {
                            // If profile pic is also provided, update that as well
                            if (profilePicUri != null) {
                                firestoreManager.updateUserField(userId, "profilePic", profilePicUri.toString(),
                                        new FirestoreManager.FirestoreCallback() {
                                            @Override
                                            public void onSuccess() {
                                                callback.onSuccess();
                                            }

                                            @Override
                                            public void onError(String errorMessage) {
                                                callback.onError("Failed to update profile picture: " + errorMessage);
                                            }
                                        });
                            } else {
                                callback.onSuccess();
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            callback.onError("Failed to update name: " + errorMessage);
                        }
                    });
                }
                // If only profile pic is provided
                else if (profilePicUri != null) {
                    firestoreManager.updateUserField(userId, "profilePic", profilePicUri.toString(),
                            new FirestoreManager.FirestoreCallback() {
                                @Override
                                public void onSuccess() {
                                    callback.onSuccess();
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    callback.onError("Failed to update profile picture: " + errorMessage);
                                }
                            });
                }
                else {
                    callback.onSuccess();
                }
            } else {
                callback.onError("Failed to update profile: " +
                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
            }
        });
    }

    public void resetPassword(String email, OperationCallback callback) {
        authManager.resetPassword(email, task -> {
            if (task.isSuccessful()) {
                callback.onSuccess();
            } else {
                callback.onError("Failed to send password reset email: " +
                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
            }
        });
    }
}