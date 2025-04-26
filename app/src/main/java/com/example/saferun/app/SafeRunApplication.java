package com.example.saferun.app;

import android.app.Application;

import com.google.firebase.FirebaseApp;

// In your Application class or MainActivity
public class SafeRunApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
    }
}