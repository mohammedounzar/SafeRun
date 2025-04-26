package com.example.saferun.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RunSession implements Serializable {
    private String id;
    private String coachId;
    private String title;
    private String description;
    private long duration; // in minutes
    private double distance; // in km
    private Date date;
    private String status; // "scheduled", "active", "completed"
    private Map<String, String> athleteStatuses; // Map of athlete ID to status ("assigned", "active", "completed")
    private List<String> athletes; // List of athlete IDs participating in this session

    public RunSession() {
        // Required empty constructor for Firebase
        athleteStatuses = new HashMap<>();
        athletes = new ArrayList<>();
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
        this.athleteStatuses = new HashMap<>();
        this.athletes = new ArrayList<>();
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

    public Map<String, String> getAthleteStatuses() {
        return athleteStatuses;
    }

    public void setAthleteStatuses(Map<String, String> athleteStatuses) {
        this.athleteStatuses = athleteStatuses;
    }

    public List<String> getAthletes() {
        return athletes;
    }

    public void setAthletes(List<String> athletes) {
        this.athletes = athletes;
    }

    public void addAthlete(String athleteId) {
        if (athleteStatuses == null) {
            athleteStatuses = new HashMap<>();
        }
        athleteStatuses.put(athleteId, "assigned");

        if (athletes == null) {
            athletes = new ArrayList<>();
        }
        if (!athletes.contains(athleteId)) {
            athletes.add(athleteId);
        }
    }

    public void removeAthlete(String athleteId) {
        if (athleteStatuses != null) {
            athleteStatuses.remove(athleteId);
        }

        if (athletes != null) {
            athletes.remove(athleteId);
        }
    }

    public void updateAthleteStatus(String athleteId, String status) {
        if (athleteStatuses == null) {
            athleteStatuses = new HashMap<>();
        }
        athleteStatuses.put(athleteId, status);

        // Make sure athlete is in the athletes list
        if (athletes == null) {
            athletes = new ArrayList<>();
        }
        if (!athletes.contains(athleteId)) {
            athletes.add(athleteId);
        }
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

    public int getAthleteCount() {
        return athletes != null ? athletes.size() : 0;
    }
}