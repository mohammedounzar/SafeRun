package com.example.saferun.data.model;

import java.io.Serializable;
import java.util.Date;

public class TeamRequest implements Serializable {
    private String id;
    private String coachId;
    private String coachName;
    private String athleteId;
    private String athleteName;
    private String status; // "pending", "accepted", "rejected"
    private Date timestamp;

    public TeamRequest() {
        // Required empty constructor for Firebase
    }

    public TeamRequest(String coachId, String coachName, String athleteId, String athleteName) {
        this.coachId = coachId;
        this.coachName = coachName;
        this.athleteId = athleteId;
        this.athleteName = athleteName;
        this.status = "pending";
        this.timestamp = new Date();
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

    public String getCoachName() {
        return coachName;
    }

    public void setCoachName(String coachName) {
        this.coachName = coachName;
    }

    public String getAthleteId() {
        return athleteId;
    }

    public void setAthleteId(String athleteId) {
        this.athleteId = athleteId;
    }

    public String getAthleteName() {
        return athleteName;
    }

    public void setAthleteName(String athleteName) {
        this.athleteName = athleteName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isPending() {
        return "pending".equals(status);
    }

    public boolean isAccepted() {
        return "accepted".equals(status);
    }

    public boolean isRejected() {
        return "rejected".equals(status);
    }
}