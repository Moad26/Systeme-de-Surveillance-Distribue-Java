package com.monitor.server.storage;

import com.monitor.model.Metric;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Handles persistence of metrics to JSON files.
 * Saves metrics periodically and loads them on startup.
 */
public class MetricsPersistence {
    
    private static final String DATA_DIR = "data/metrics";
    private static final long SAVE_INTERVAL_MS = 60000; // Save every minute
    
    private final DataManager dataManager;
    private final ScheduledExecutorService scheduler;
    private volatile boolean running = true;
    
    public MetricsPersistence(DataManager dataManager) {
        this.dataManager = dataManager;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        
        // Create data directory
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
        } catch (IOException e) {
            System.err.println("[MetricsPersistence] Failed to create data directory: " + e.getMessage());
        }
    }
    
    /**
     * Start periodic saving.
     */
    public void start() {
        // Load existing data
        loadAllMetrics();
        
        // Schedule periodic saves
        scheduler.scheduleAtFixedRate(
            this::saveAllMetrics,
            SAVE_INTERVAL_MS,
            SAVE_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );
        
        System.out.println("[MetricsPersistence] Started with save interval " + (SAVE_INTERVAL_MS / 1000) + "s");
    }
    
    /**
     * Stop persistence and save final state.
     */
    public void stop() {
        running = false;
        scheduler.shutdown();
        saveAllMetrics();
        System.out.println("[MetricsPersistence] Stopped and saved final state");
    }
    
    /**
     * Save all metrics to files.
     */
    public void saveAllMetrics() {
        Set<String> agentIds = dataManager.getAllAgentIds();
        
        for (String agentId : agentIds) {
            saveAgentMetrics(agentId);
        }
        
        System.out.println("[MetricsPersistence] Saved metrics for " + agentIds.size() + " agents");
    }
    
    /**
     * Save metrics for a specific agent.
     */
    private void saveAgentMetrics(String agentId) {
        List<Metric> metrics = dataManager.getAllMetrics(agentId);
        
        if (metrics.isEmpty()) {
            return;
        }
        
        String filename = sanitizeFilename(agentId) + ".json";
        Path filepath = Paths.get(DATA_DIR, filename);
        
        try (BufferedWriter writer = Files.newBufferedWriter(filepath)) {
            writer.write("[\n");
            
            for (int i = 0; i < metrics.size(); i++) {
                Metric m = metrics.get(i);
                writer.write(String.format(
                    "  {\"agentId\":\"%s\",\"timestamp\":%d,\"cpu\":%.2f,\"ram\":%.2f,\"disk\":%.2f}",
                    m.getAgentId(), m.getTimestamp(), m.getCpuUsage(), m.getRamUsage(), m.getDiskUsage()
                ));
                
                if (i < metrics.size() - 1) {
                    writer.write(",");
                }
                writer.write("\n");
            }
            
            writer.write("]");
            
        } catch (IOException e) {
            System.err.println("[MetricsPersistence] Failed to save metrics for " + agentId + ": " + e.getMessage());
        }
    }
    
    /**
     * Load all metrics from files.
     */
    public void loadAllMetrics() {
        try {
            Path dataPath = Paths.get(DATA_DIR);
            
            if (!Files.exists(dataPath)) {
                System.out.println("[MetricsPersistence] No existing data directory");
                return;
            }
            
            Files.list(dataPath)
                .filter(p -> p.toString().endsWith(".json"))
                .forEach(this::loadAgentMetrics);
                
        } catch (IOException e) {
            System.err.println("[MetricsPersistence] Failed to list data files: " + e.getMessage());
        }
    }
    
    /**
     * Load metrics for a specific agent from file.
     */
    private void loadAgentMetrics(Path filepath) {
        try {
            String content = Files.readString(filepath);
            List<Metric> metrics = parseMetricsJson(content);
            
            for (Metric metric : metrics) {
                dataManager.addMetric(metric);
            }
            
            System.out.println("[MetricsPersistence] Loaded " + metrics.size() + " metrics from " + filepath.getFileName());
            
        } catch (IOException e) {
            System.err.println("[MetricsPersistence] Failed to load " + filepath + ": " + e.getMessage());
        }
    }
    
    /**
     * Parse JSON array of metrics (simple parser, no external dependencies).
     */
    private List<Metric> parseMetricsJson(String json) {
        List<Metric> metrics = new ArrayList<>();
        
        // Simple parsing - find each object
        int start = 0;
        while ((start = json.indexOf("{", start)) != -1) {
            int end = json.indexOf("}", start);
            if (end == -1) break;
            
            String obj = json.substring(start, end + 1);
            Metric metric = parseMetricObject(obj);
            if (metric != null) {
                metrics.add(metric);
            }
            
            start = end + 1;
        }
        
        return metrics;
    }
    
    /**
     * Parse a single metric JSON object.
     */
    private Metric parseMetricObject(String obj) {
        try {
            String agentId = extractStringValue(obj, "agentId");
            long timestamp = extractLongValue(obj, "timestamp");
            double cpu = extractDoubleValue(obj, "cpu");
            double ram = extractDoubleValue(obj, "ram");
            double disk = extractDoubleValue(obj, "disk");
            
            Metric metric = new Metric(agentId, cpu, ram, disk);
            metric.setTimestamp(timestamp);
            return metric;
            
        } catch (Exception e) {
            return null;
        }
    }
    
    private String extractStringValue(String json, String key) {
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex == -1) return null;
        
        int colonIndex = json.indexOf(":", keyIndex);
        int startQuote = json.indexOf("\"", colonIndex);
        int endQuote = json.indexOf("\"", startQuote + 1);
        
        return json.substring(startQuote + 1, endQuote);
    }
    
    private long extractLongValue(String json, String key) {
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex == -1) return 0;
        
        int colonIndex = json.indexOf(":", keyIndex);
        int start = colonIndex + 1;
        
        // Skip whitespace
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        
        return Long.parseLong(json.substring(start, end));
    }
    
    private double extractDoubleValue(String json, String key) {
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex == -1) return 0;
        
        int colonIndex = json.indexOf(":", keyIndex);
        int start = colonIndex + 1;
        
        // Skip whitespace
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-')) {
            end++;
        }
        
        return Double.parseDouble(json.substring(start, end));
    }
    
    /**
     * Sanitize agent ID for use as filename.
     */
    private String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}
