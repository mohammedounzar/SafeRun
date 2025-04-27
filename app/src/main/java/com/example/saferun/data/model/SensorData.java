package com.example.saferun.data.model;

import java.io.Serializable;

/**
 * Model class that represents sensor data collected during a run session.
 * Modified to match the fields used by the Python simulator.
 */
public class SensorData implements Serializable {
    private long timestamp;
    private int heartRate;
    private double temperature;
    private double speed;
    private String status;
    private boolean anomalyDetected;
    private String sessionId;
    private String athleteId;

    public SensorData() {
        // Required empty constructor for Firebase
    }

    public SensorData(long timestamp, int heartRate, double temperature, double speed, String status) {
        this.timestamp = timestamp;
        this.heartRate = heartRate;
        this.temperature = temperature;
        this.speed = speed;
        this.status = status;
        this.anomalyDetected = false;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(int heartRate) {
        this.heartRate = heartRate;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isAnomalyDetected() {
        return anomalyDetected;
    }

    public void setAnomalyDetected(boolean anomalyDetected) {
        this.anomalyDetected = anomalyDetected;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getAthleteId() {
        return athleteId;
    }

    public void setAthleteId(String athleteId) {
        this.athleteId = athleteId;
    }

    @Override
    public String toString() {
        return "SensorData{" +
                "timestamp=" + timestamp +
                ", heartRate=" + heartRate +
                ", temperature=" + temperature +
                ", speed=" + speed +
                ", status='" + status + '\'' +
                ", anomalyDetected=" + anomalyDetected +
                ", sessionId='" + sessionId + '\'' +
                ", athleteId='" + athleteId + '\'' +
                '}';
    }
}