package com.example.saferun.data.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class RunSession implements Serializable {
    private String id;
    private String coachId;
    private String title;
    private String description;
    private long duration; // in minutes
    private double distance; // in kilometers
    private Date date;
    private String status; // "scheduled" | "active" | "completed"
    private Map<String, Map<String, Object>> athletes; // Map of athlete IDs to their session details

    public RunSession() {
        // Required empty constructor for Firebase
        this.athletes = new HashMap<>();
        this.status = "scheduled";
    }

    public RunSession(String id, String coachId, String title, String description,
                      long duration, double distance, Date date) {
        this.id = id;
        this.coachId = coachId;
        this.title = title;
        this.description = description;
        this.duration = duration;
        this.distance = distance;
        this.date = date;
        this.status = "scheduled";
        this.athletes = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCoachId() {
        return coachId;
    }

    public void setCoachId(String coachId) {
        this.coachId = coachId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Map<String, Object>> getAthletes() {
        return athletes;
    }

    public void setAthletes(Map<String, Map<String, Object>> athletes) {
        this.athletes = athletes;
    }

    public void addAthlete(String athleteId) {
        Map<String, Object> athleteData = new HashMap<>();
        athleteData.put("status", "assigned");
        athleteData.put("startTime", null);
        athleteData.put("endTime", null);
        athleteData.put("performanceScore", 0);

        this.athletes.put(athleteId, athleteData);
    }

    public void removeAthlete(String athleteId) {
        this.athletes.remove(athleteId);
    }

    public boolean isScheduled() {
        return "scheduled".equals(status);
    }

    public boolean isActive() {
        return "active".equals(status);
    }

    public boolean isCompleted() {
        return "completed".equals(status);
    }
}