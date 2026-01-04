package com.monitor.server.storage;

import com.monitor.model.Alert;
import com.monitor.model.Metric;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton data manager for storing metrics and alerts.
 * Thread-safe with concurrent collections.
 */
public class DataManager {
    
    private static volatile DataManager instance;
    
    // Maximum metrics to store per agent
    private static final int MAX_METRICS_PER_AGENT = 1000;
    
    // Maximum alerts to store
    private static final int MAX_ALERTS = 500;
    
    // Storage: agentId -> list of metrics
    private final Map<String, List<Metric>> metricsStore;
    
    // Storage: all alerts
    private final List<Alert> alertsStore;
    
    // Track active agents with last seen timestamp
    private final Map<String, Long> activeAgents;
    
    private DataManager() {
        this.metricsStore = new ConcurrentHashMap<>();
        this.alertsStore = new CopyOnWriteArrayList<>();
        this.activeAgents = new ConcurrentHashMap<>();
        System.out.println("[DataManager] Singleton instance created");
    }
    
    /**
     * Get the singleton instance.
     */
    public static DataManager getInstance() {
        if (instance == null) {
            synchronized (DataManager.class) {
                if (instance == null) {
                    instance = new DataManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Add a metric to the store.
     */
    public void addMetric(Metric metric) {
        String agentId = metric.getAgentId();
        
        // Update active agents
        activeAgents.put(agentId, System.currentTimeMillis());
        
        // Get or create list for this agent
        List<Metric> agentMetrics = metricsStore.computeIfAbsent(
            agentId, 
            k -> Collections.synchronizedList(new ArrayList<>())
        );
        
        // Add metric
        agentMetrics.add(metric);
        
        // Trim if over limit
        while (agentMetrics.size() > MAX_METRICS_PER_AGENT) {
            agentMetrics.remove(0);
        }
    }
    
    /**
     * Add an alert to the store.
     */
    public void addAlert(Alert alert) {
        alertsStore.add(alert);
        
        // Trim if over limit
        while (alertsStore.size() > MAX_ALERTS) {
            alertsStore.remove(0);
        }
        
        System.out.println("[DataManager] Alert added: " + alert);
    }
    
    /**
     * Get all active agent IDs.
     */
    public List<String> getActiveAgents() {
        // Consider an agent active if seen in last 30 seconds
        long threshold = System.currentTimeMillis() - 30_000;
        List<String> active = new ArrayList<>();
        
        for (Map.Entry<String, Long> entry : activeAgents.entrySet()) {
            if (entry.getValue() > threshold) {
                active.add(entry.getKey());
            }
        }
        
        return active;
    }
    
    /**
     * Get all known agent IDs (active or inactive).
     */
    public Set<String> getAllAgentIds() {
        return metricsStore.keySet();
    }
    
    /**
     * Get recent metrics for an agent.
     */
    public List<Metric> getMetrics(String agentId, int limit) {
        List<Metric> agentMetrics = metricsStore.get(agentId);
        
        if (agentMetrics == null || agentMetrics.isEmpty()) {
            return new ArrayList<>();
        }
        
        synchronized (agentMetrics) {
            int size = agentMetrics.size();
            int start = Math.max(0, size - limit);
            return new ArrayList<>(agentMetrics.subList(start, size));
        }
    }
    
    /**
     * Get all metrics for an agent.
     */
    public List<Metric> getAllMetrics(String agentId) {
        List<Metric> agentMetrics = metricsStore.get(agentId);
        
        if (agentMetrics == null) {
            return new ArrayList<>();
        }
        
        synchronized (agentMetrics) {
            return new ArrayList<>(agentMetrics);
        }
    }
    
    /**
     * Get alerts for a specific agent.
     */
    public List<Alert> getAlerts(String agentId) {
        if (agentId == null) {
            return new ArrayList<>(alertsStore);
        }
        
        List<Alert> result = new ArrayList<>();
        for (Alert alert : alertsStore) {
            if (agentId.equals(alert.getAgentId())) {
                result.add(alert);
            }
        }
        return result;
    }
    
    /**
     * Get all alerts.
     */
    public List<Alert> getAllAlerts() {
        return new ArrayList<>(alertsStore);
    }
    
    /**
     * Clear all alerts.
     */
    public void clearAlerts() {
        alertsStore.clear();
        System.out.println("[DataManager] All alerts cleared");
    }
    
    /**
     * Get statistics for logging.
     */
    public String getStats() {
        int totalMetrics = metricsStore.values().stream()
            .mapToInt(List::size)
            .sum();
        return String.format("Agents: %d, Metrics: %d, Alerts: %d",
            metricsStore.size(), totalMetrics, alertsStore.size());
    }
    
    /**
     * Search agents by name pattern.
     */
    public List<String> searchAgents(String query) {
        if (query == null || query.isEmpty()) {
            return getActiveAgents();
        }
        
        String lowerQuery = query.toLowerCase();
        List<String> result = new ArrayList<>();
        
        for (String agentId : metricsStore.keySet()) {
            if (agentId.toLowerCase().contains(lowerQuery)) {
                result.add(agentId);
            }
        }
        
        return result;
    }
    
    /**
     * Get metrics within a date range.
     */
    public List<Metric> getMetricsByDateRange(String agentId, long fromTime, long toTime) {
        List<Metric> allMetrics = getAllMetrics(agentId);
        
        return allMetrics.stream()
            .filter(m -> m.getTimestamp() >= fromTime && m.getTimestamp() <= toTime)
            .toList();
    }
    
    /**
     * Get alerts with filters.
     */
    public List<Alert> getAlertsByFilter(String agentId, String severity, long fromTime, long toTime) {
        return alertsStore.stream()
            .filter(a -> agentId == null || agentId.isEmpty() || a.getAgentId().equals(agentId))
            .filter(a -> severity == null || severity.isEmpty() || a.getLevel().name().equals(severity))
            .filter(a -> a.getTimestamp() >= fromTime && a.getTimestamp() <= toTime)
            .toList();
    }
}
