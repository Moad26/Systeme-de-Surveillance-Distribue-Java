package com.monitor.server.service;

import com.monitor.model.Metric;
import com.monitor.model.MetricStatistics;

import java.util.List;

/**
 * Service for calculating statistics from metrics.
 */
public class StatisticsService {
    
    /**
     * Calculate statistics for a list of metrics.
     */
    public MetricStatistics calculateStatistics(String agentId, List<Metric> metrics, long fromTime, long toTime) {
        MetricStatistics stats = new MetricStatistics(agentId, fromTime, toTime);
        
        if (metrics.isEmpty()) {
            stats.setSampleCount(0);
            return stats;
        }
        
        // Filter metrics by time range
        List<Metric> filtered = metrics.stream()
            .filter(m -> m.getTimestamp() >= fromTime && m.getTimestamp() <= toTime)
            .toList();
        
        if (filtered.isEmpty()) {
            stats.setSampleCount(0);
            return stats;
        }
        
        stats.setSampleCount(filtered.size());
        
        // Calculate CPU statistics
        double[] cpuValues = filtered.stream().mapToDouble(Metric::getCpuUsage).toArray();
        stats.setCpuAvg(round(average(cpuValues)));
        stats.setCpuMin(round(min(cpuValues)));
        stats.setCpuMax(round(max(cpuValues)));
        stats.setCpuStdDev(round(stdDev(cpuValues)));
        stats.setCpuTrend(calculateTrend(cpuValues));
        
        // Calculate RAM statistics
        double[] ramValues = filtered.stream().mapToDouble(Metric::getRamUsage).toArray();
        stats.setRamAvg(round(average(ramValues)));
        stats.setRamMin(round(min(ramValues)));
        stats.setRamMax(round(max(ramValues)));
        stats.setRamStdDev(round(stdDev(ramValues)));
        stats.setRamTrend(calculateTrend(ramValues));
        
        // Calculate Disk statistics
        double[] diskValues = filtered.stream().mapToDouble(Metric::getDiskUsage).toArray();
        stats.setDiskAvg(round(average(diskValues)));
        stats.setDiskMin(round(min(diskValues)));
        stats.setDiskMax(round(max(diskValues)));
        stats.setDiskStdDev(round(stdDev(diskValues)));
        stats.setDiskTrend(calculateTrend(diskValues));
        
        return stats;
    }
    
    /**
     * Calculate average.
     */
    private double average(double[] values) {
        if (values.length == 0) return 0;
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.length;
    }
    
    /**
     * Calculate minimum.
     */
    private double min(double[] values) {
        if (values.length == 0) return 0;
        double min = values[0];
        for (double v : values) {
            if (v < min) min = v;
        }
        return min;
    }
    
    /**
     * Calculate maximum.
     */
    private double max(double[] values) {
        if (values.length == 0) return 0;
        double max = values[0];
        for (double v : values) {
            if (v > max) max = v;
        }
        return max;
    }
    
    /**
     * Calculate standard deviation.
     */
    private double stdDev(double[] values) {
        if (values.length < 2) return 0;
        
        double avg = average(values);
        double sumSquares = 0;
        
        for (double v : values) {
            sumSquares += (v - avg) * (v - avg);
        }
        
        return Math.sqrt(sumSquares / values.length);
    }
    
    /**
     * Calculate trend (RISING, FALLING, STABLE).
     * Uses linear regression slope.
     */
    private String calculateTrend(double[] values) {
        if (values.length < 2) return "STABLE";
        
        // Simple linear regression
        int n = values.length;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += values[i];
            sumXY += i * values[i];
            sumX2 += i * i;
        }
        
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        
        // Determine trend based on slope
        // Threshold: slope > 0.5 means rising, < -0.5 means falling
        if (slope > 0.5) {
            return "RISING";
        } else if (slope < -0.5) {
            return "FALLING";
        } else {
            return "STABLE";
        }
    }
    
    /**
     * Round to 2 decimal places.
     */
    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
