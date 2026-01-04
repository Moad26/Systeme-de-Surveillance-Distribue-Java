package com.monitor.server.storage;

import com.monitor.model.AlertConfig;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages alert configurations with JSON persistence.
 */
public class AlertConfigManager {
    
    private static final String CONFIG_FILE = "data/alert_configs.json";
    private static AlertConfigManager instance;
    
    private final List<AlertConfig> configs;
    
    private AlertConfigManager() {
        this.configs = new CopyOnWriteArrayList<>();
        loadConfigs();
        
        // Initialize default configs if empty
        if (configs.isEmpty()) {
            initializeDefaults();
        }
    }
    
    /**
     * Get singleton instance.
     */
    public static synchronized AlertConfigManager getInstance() {
        if (instance == null) {
            instance = new AlertConfigManager();
        }
        return instance;
    }
    
    /**
     * Initialize default alert configurations.
     */
    private void initializeDefaults() {
        configs.add(new AlertConfig("CPU", 70.0, 90.0));
        configs.add(new AlertConfig("RAM", 80.0, 95.0));
        configs.add(new AlertConfig("DISK", 85.0, 95.0));
        saveConfigs();
        System.out.println("[AlertConfigManager] Initialized default configurations");
    }
    
    /**
     * Get all configurations.
     */
    public List<AlertConfig> getAllConfigs() {
        return new ArrayList<>(configs);
    }
    
    /**
     * Get configuration for a specific metric type.
     */
    public AlertConfig getConfig(String metricType) {
        return configs.stream()
            .filter(c -> c.getMetricType().equalsIgnoreCase(metricType))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Update a configuration.
     */
    public void updateConfig(AlertConfig config) {
        // Find and replace existing config
        for (int i = 0; i < configs.size(); i++) {
            if (configs.get(i).getMetricType().equalsIgnoreCase(config.getMetricType())) {
                configs.set(i, config);
                saveConfigs();
                System.out.println("[AlertConfigManager] Updated: " + config);
                return;
            }
        }
        
        // Add new config
        configs.add(config);
        saveConfigs();
        System.out.println("[AlertConfigManager] Added: " + config);
    }
    
    /**
     * Save configurations to JSON file.
     */
    private void saveConfigs() {
        try {
            Files.createDirectories(Paths.get("data"));
            
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(CONFIG_FILE))) {
                writer.write("[\n");
                
                for (int i = 0; i < configs.size(); i++) {
                    AlertConfig c = configs.get(i);
                    writer.write(String.format(
                        "  {\"metricType\":\"%s\",\"warningThreshold\":%.1f,\"criticalThreshold\":%.1f,\"enabled\":%s}",
                        c.getMetricType(), c.getWarningThreshold(), c.getCriticalThreshold(), c.isEnabled()
                    ));
                    
                    if (i < configs.size() - 1) {
                        writer.write(",");
                    }
                    writer.write("\n");
                }
                
                writer.write("]");
            }
            
        } catch (IOException e) {
            System.err.println("[AlertConfigManager] Failed to save: " + e.getMessage());
        }
    }
    
    /**
     * Load configurations from JSON file.
     */
    private void loadConfigs() {
        Path path = Paths.get(CONFIG_FILE);
        
        if (!Files.exists(path)) {
            return;
        }
        
        try {
            String content = Files.readString(path);
            parseConfigs(content);
            System.out.println("[AlertConfigManager] Loaded " + configs.size() + " configurations");
            
        } catch (IOException e) {
            System.err.println("[AlertConfigManager] Failed to load: " + e.getMessage());
        }
    }
    
    /**
     * Parse JSON content to configs.
     */
    private void parseConfigs(String json) {
        int start = 0;
        while ((start = json.indexOf("{", start)) != -1) {
            int end = json.indexOf("}", start);
            if (end == -1) break;
            
            String obj = json.substring(start, end + 1);
            AlertConfig config = parseConfigObject(obj);
            if (config != null) {
                configs.add(config);
            }
            
            start = end + 1;
        }
    }
    
    /**
     * Parse a single config JSON object.
     */
    private AlertConfig parseConfigObject(String obj) {
        try {
            String metricType = extractStringValue(obj, "metricType");
            double warning = extractDoubleValue(obj, "warningThreshold");
            double critical = extractDoubleValue(obj, "criticalThreshold");
            boolean enabled = extractBooleanValue(obj, "enabled");
            
            AlertConfig config = new AlertConfig(metricType, warning, critical);
            config.setEnabled(enabled);
            return config;
            
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
    
    private double extractDoubleValue(String json, String key) {
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex == -1) return 0;
        
        int colonIndex = json.indexOf(":", keyIndex);
        int start = colonIndex + 1;
        
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-')) {
            end++;
        }
        
        return Double.parseDouble(json.substring(start, end));
    }
    
    private boolean extractBooleanValue(String json, String key) {
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex == -1) return true;
        
        int colonIndex = json.indexOf(":", keyIndex);
        String rest = json.substring(colonIndex + 1).trim();
        
        return rest.startsWith("true");
    }
}
