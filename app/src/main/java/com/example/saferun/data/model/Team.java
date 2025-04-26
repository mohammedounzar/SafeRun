package com.example.saferun.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Team implements Serializable {
    private String id;
    private String name;
    private String description;
    private String coachId;
    private List<String> athleteIds;
    private long createdAt;

    public Team() {
        // Required empty constructor for Firebase
        athleteIds = new ArrayList<>();
        createdAt = System.currentTimeMillis();
    }

    public Team(String id,String name, String description, String coachId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.coachId = coachId;
        this.athleteIds = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoachId() {
        return coachId;
    }

    public void setCoachId(String coachId) {
        this.coachId = coachId;
    }

    public List<String> getAthleteIds() {
        return athleteIds;
    }

    public void setAthleteIds(List<String> athleteIds) {
        this.athleteIds = athleteIds;
    }

    public void addAthlete(String athleteId) {
        if (athleteIds == null) {
            athleteIds = new ArrayList<>();
        }
        if (!athleteIds.contains(athleteId)) {
            athleteIds.add(athleteId);
        }
    }

    public void removeAthlete(String athleteId) {
        if (athleteIds != null) {
            athleteIds.remove(athleteId);
        }
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public int getAthleteCount() {
        return athleteIds != null ? athleteIds.size() : 0;
    }
}