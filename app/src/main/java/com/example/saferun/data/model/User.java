package com.example.saferun.data.model;

import java.io.Serializable;

public class User implements Serializable {
    private String uid;
    private String email;
    private String name;
    private String profilePic;
    private String role;  // "athlete" or "coach"
    private double performanceScore;  // Only relevant for athletes

    public User() {
        // Required empty constructor for Firebase
    }

    public User(String uid, String email, String name, String profilePic, String role) {
        this.uid = uid;
        this.email = email;
        this.name = name;
        this.profilePic = profilePic;
        this.role = role;
        this.performanceScore = 0.0;  // Default value for new users
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public double getPerformanceScore() {
        return performanceScore;
    }

    public void setPerformanceScore(double performanceScore) {
        this.performanceScore = performanceScore;
    }

    public boolean isCoach() {
        return "coach".equals(role);
    }

    public boolean isAthlete() {
        return "athlete".equals(role);
    }
}