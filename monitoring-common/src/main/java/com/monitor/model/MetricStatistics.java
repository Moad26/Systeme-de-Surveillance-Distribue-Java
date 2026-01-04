package com.monitor.model;

import java.io.Serializable;

/**
 * Statistics calculated from a set of metrics.
 */
public class MetricStatistics implements Serializable {
    private static final long serialVersionUID = 1L;

    private String agentId;
    private long fromTime;
    private long toTime;
    private int sampleCount;

    // CPU statistics
    private double cpuAvg;
    private double cpuMin;
    private double cpuMax;
    private double cpuStdDev;
    private String cpuTrend; // "RISING", "FALLING", "STABLE"

    // RAM statistics
    private double ramAvg;
    private double ramMin;
    private double ramMax;
    private double ramStdDev;
    private String ramTrend;

    // Disk statistics
    private double diskAvg;
    private double diskMin;
    private double diskMax;
    private double diskStdDev;
    private String diskTrend;

    public MetricStatistics() {
    }

    public MetricStatistics(String agentId, long fromTime, long toTime) {
        this.agentId = agentId;
        this.fromTime = fromTime;
        this.toTime = toTime;
    }

    // Getters and Setters
    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public long getFromTime() {
        return fromTime;
    }

    public void setFromTime(long fromTime) {
        this.fromTime = fromTime;
    }

    public long getToTime() {
        return toTime;
    }

    public void setToTime(long toTime) {
        this.toTime = toTime;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
    }

    public double getCpuAvg() {
        return cpuAvg;
    }

    public void setCpuAvg(double cpuAvg) {
        this.cpuAvg = cpuAvg;
    }

    public double getCpuMin() {
        return cpuMin;
    }

    public void setCpuMin(double cpuMin) {
        this.cpuMin = cpuMin;
    }

    public double getCpuMax() {
        return cpuMax;
    }

    public void setCpuMax(double cpuMax) {
        this.cpuMax = cpuMax;
    }

    public double getCpuStdDev() {
        return cpuStdDev;
    }

    public void setCpuStdDev(double cpuStdDev) {
        this.cpuStdDev = cpuStdDev;
    }

    public String getCpuTrend() {
        return cpuTrend;
    }

    public void setCpuTrend(String cpuTrend) {
        this.cpuTrend = cpuTrend;
    }

    public double getRamAvg() {
        return ramAvg;
    }

    public void setRamAvg(double ramAvg) {
        this.ramAvg = ramAvg;
    }

    public double getRamMin() {
        return ramMin;
    }

    public void setRamMin(double ramMin) {
        this.ramMin = ramMin;
    }

    public double getRamMax() {
        return ramMax;
    }

    public void setRamMax(double ramMax) {
        this.ramMax = ramMax;
    }

    public double getRamStdDev() {
        return ramStdDev;
    }

    public void setRamStdDev(double ramStdDev) {
        this.ramStdDev = ramStdDev;
    }

    public String getRamTrend() {
        return ramTrend;
    }

    public void setRamTrend(String ramTrend) {
        this.ramTrend = ramTrend;
    }

    public double getDiskAvg() {
        return diskAvg;
    }

    public void setDiskAvg(double diskAvg) {
        this.diskAvg = diskAvg;
    }

    public double getDiskMin() {
        return diskMin;
    }

    public void setDiskMin(double diskMin) {
        this.diskMin = diskMin;
    }

    public double getDiskMax() {
        return diskMax;
    }

    public void setDiskMax(double diskMax) {
        this.diskMax = diskMax;
    }

    public double getDiskStdDev() {
        return diskStdDev;
    }

    public void setDiskStdDev(double diskStdDev) {
        this.diskStdDev = diskStdDev;
    }

    public String getDiskTrend() {
        return diskTrend;
    }

    public void setDiskTrend(String diskTrend) {
        this.diskTrend = diskTrend;
    }

    @Override
    public String toString() {
        return String.format("Stats[agent=%s, samples=%d, cpu=%.1f%%, ram=%.1f%%, disk=%.1f%%]",
                agentId, sampleCount, cpuAvg, ramAvg, diskAvg);
    }
}
