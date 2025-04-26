package com.example.saferun.data.firebase;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.saferun.data.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class FirebaseAuthManager {
    private FirebaseAuth firebaseAuth;
    private FirestoreManager firestoreManager;
    private static FirebaseAuthManager instance;

    private FirebaseAuthManager() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestoreManager = FirestoreManager.getInstance();
    }

    public static synchronized FirebaseAuthManager getInstance() {
        if (instance == null) {
            instance = new FirebaseAuthManager();
        }
        return instance;
    }

    public interface AuthCallback {
        void onSuccess(User user);
        void onError(String errorMessage);
    }

    public boolean isUserLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    public String getCurrentUserId() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public void signOut() {
        firebaseAuth.signOut();
    }

    public void registerUser(String email, String password, String name, String role, AuthCallback callback) {
        Log.d("FirebaseAuthManager", "Attempting to register user: " + email + " with role: " + role);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        Log.d("FirebaseAuthManager", "Firebase user created with UID: " + firebaseUser.getUid());

                        // Update display name
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build();

                        firebaseUser.updateProfile(profileUpdates)
                                .addOnCompleteListener(profileTask -> {
                                    if (profileTask.isSuccessful()) {
                                        Log.d("FirebaseAuthManager", "Profile updated successfully");

                                        // Create user object
                                        User user = new User(
                                                firebaseUser.getUid(),
                                                email,
                                                name,
                                                "",  // No profile pic yet
                                                role
                                        );

                                        // Save user to Firestore
                                        firestoreManager.saveUser(user, new FirestoreManager.FirestoreCallback() {
                                            @Override
                                            public void onSuccess() {
                                                Log.d("FirebaseAuthManager", "User saved to Firestore successfully");
                                                callback.onSuccess(user);
                                            }

                                            @Override
                                            public void onError(String errorMessage) {
                                                Log.e("FirebaseAuthManager", "Failed to save user to Firestore: " + errorMessage);
                                                callback.onError("Failed to save user data: " + errorMessage);
                                            }
                                        });
                                    } else {
                                        Log.e("FirebaseAuthManager", "Failed to update profile: " +
                                                (profileTask.getException() != null ? profileTask.getException().getMessage() : "Unknown error"));
                                        callback.onError("Failed to update profile: " +
                                                (profileTask.getException() != null ?
                                                        profileTask.getException().getMessage() : "Unknown error"));
                                    }
                                });
                    } else {
                        Log.e("FirebaseAuthManager", "Registration failed: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                        callback.onError("Registration failed: " +
                                (task.getException() != null ?
                                        task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    public void signIn(String email, String password, AuthCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

                        // Fetch user data from Firestore
                        firestoreManager.getUserById(firebaseUser.getUid(), new FirestoreManager.UserCallback() {
                            @Override
                            public void onUserLoaded(User user) {
                                callback.onSuccess(user);
                            }

                            @Override
                            public void onError(String errorMessage) {
                                callback.onError("Failed to get user data: " + errorMessage);
                            }
                        });
                    } else {
                        callback.onError("Login failed: " +
                                (task.getException() != null ?
                                        task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    public void updateUserProfile(String name, Uri profilePicUri, OnCompleteListener<Void> listener) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            UserProfileChangeRequest.Builder profileUpdates = new UserProfileChangeRequest.Builder();

            if (name != null && !name.isEmpty()) {
                profileUpdates.setDisplayName(name);
            }

            if (profilePicUri != null) {
                profileUpdates.setPhotoUri(profilePicUri);
            }

            firebaseUser.updateProfile(profileUpdates.build())
                    .addOnCompleteListener(listener);
        }
    }

    public void resetPassword(String email, OnCompleteListener<Void> listener) {
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(listener);
    }
}