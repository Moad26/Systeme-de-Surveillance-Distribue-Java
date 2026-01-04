package com.monitor.agent.collectors;

/**
 * Strategy interface for metric collectors.
 * Allows easy addition of new collectors (CPU, RAM, Disk, Network, etc.)
 */
public interface ICollector {
    
    /**
     * Collect the current value of the metric.
     * @return The metric value as a percentage (0-100)
     */
    double collect();
    
    /**
     * Get the name of this collector.
     * @return Collector name (e.g., "CPU", "RAM", "DISK")
     */
    String getName();
}
