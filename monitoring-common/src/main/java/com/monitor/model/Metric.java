package com.monitor.model;

import java.io.Serializable;

/**
 * Represents a system metric collected by an agent.
 * Contains CPU, RAM, and Disk usage information.
 */
public class Metric implements Serializable {
    private static final long serialVersionUID = 1L;

    private String agentId;
    private long timestamp;
    private double cpuUsage;
    private double ramUsage;
    private double diskUsage;

    public Metric() {
    }

    public Metric(String agentId, double cpuUsage, double ramUsage, double diskUsage) {
        this.agentId = agentId;
        this.timestamp = System.currentTimeMillis();
        this.cpuUsage = cpuUsage;
        this.ramUsage = ramUsage;
        this.diskUsage = diskUsage;
    }

    // Getters and Setters
    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public double getRamUsage() {
        return ramUsage;
    }

    public void setRamUsage(double ramUsage) {
        this.ramUsage = ramUsage;
    }

    public double getDiskUsage() {
        return diskUsage;
    }

    public void setDiskUsage(double diskUsage) {
        this.diskUsage = diskUsage;
    }

    @Override
    public String toString() {
        return String.format("Metric[agent=%s, cpu=%.1f%%, ram=%.1f%%, disk=%.1f%%, time=%d]",
                agentId, cpuUsage, ramUsage, diskUsage, timestamp);
    }
}
