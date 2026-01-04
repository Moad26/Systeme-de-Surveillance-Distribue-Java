package com.monitor.model;

import java.io.Serializable;

/**
 * Represents an alert generated when a metric exceeds a threshold.
 */
public class Alert implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Severity levels for alerts.
     */
    public enum Severity {
        INFO,
        WARNING,
        CRITICAL
    }

    private String agentId;
    private String message;
    private Severity level;
    private long timestamp;
    private String metricType; // CPU, RAM, DISK

    public Alert() {
    }

    public Alert(String agentId, String message, Severity level, String metricType) {
        this.agentId = agentId;
        this.message = message;
        this.level = level;
        this.metricType = metricType;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Severity getLevel() {
        return level;
    }

    public void setLevel(Severity level) {
        this.level = level;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    @Override
    public String toString() {
        return String.format("Alert[agent=%s, level=%s, type=%s, message=%s]",
                agentId, level, metricType, message);
    }
}
