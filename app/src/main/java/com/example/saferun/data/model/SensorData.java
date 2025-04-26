package com.example.saferun.data.model;

import java.io.Serializable;

public class SensorData implements Serializable {
    private long timestamp;
    private int heartRate;
    private double temperature;
    private double speed;
    private String status;
    private boolean anomalyDetected;

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
}