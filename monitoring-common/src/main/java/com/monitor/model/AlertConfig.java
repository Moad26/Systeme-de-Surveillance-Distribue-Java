package com.monitor.model;

import java.io.Serializable;

/**
 * Configuration for alert thresholds.
 */
public class AlertConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private String metricType; // CPU, RAM, DISK
    private double warningThreshold;
    private double criticalThreshold;
    private boolean enabled;

    public AlertConfig() {
        this.enabled = true;
    }

    public AlertConfig(String metricType, double warningThreshold, double criticalThreshold) {
        this.metricType = metricType;
        this.warningThreshold = warningThreshold;
        this.criticalThreshold = criticalThreshold;
        this.enabled = true;
    }

    // Getters and Setters
    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public double getWarningThreshold() {
        return warningThreshold;
    }

    public void setWarningThreshold(double warningThreshold) {
        this.warningThreshold = warningThreshold;
    }

    public double getCriticalThreshold() {
        return criticalThreshold;
    }

    public void setCriticalThreshold(double criticalThreshold) {
        this.criticalThreshold = criticalThreshold;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return String.format("AlertConfig[%s: warning=%.1f%%, critical=%.1f%%, enabled=%s]",
                metricType, warningThreshold, criticalThreshold, enabled);
    }
}
