package com.monitor.server.export;

import com.monitor.model.Alert;
import com.monitor.model.Metric;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Export metrics and alerts to CSV and JSON formats.
 */
public class DataExporter {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Export metrics to CSV format.
     */
    public String exportMetricsToCSV(List<Metric> metrics) {
        StringBuilder sb = new StringBuilder();
        
        // Header
        sb.append("Agent ID,Timestamp,Date/Time,CPU (%),RAM (%),Disk (%)\n");
        
        // Data rows
        for (Metric m : metrics) {
            sb.append(String.format("%s,%d,%s,%.2f,%.2f,%.2f\n",
                escapeCSV(m.getAgentId()),
                m.getTimestamp(),
                DATE_FORMAT.format(new Date(m.getTimestamp())),
                m.getCpuUsage(),
                m.getRamUsage(),
                m.getDiskUsage()
            ));
        }
        
        return sb.toString();
    }
    
    /**
     * Export metrics to JSON format.
     */
    public String exportMetricsToJSON(List<Metric> metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        
        for (int i = 0; i < metrics.size(); i++) {
            Metric m = metrics.get(i);
            sb.append(String.format(
                "  {\n" +
                "    \"agentId\": \"%s\",\n" +
                "    \"timestamp\": %d,\n" +
                "    \"dateTime\": \"%s\",\n" +
                "    \"cpuUsage\": %.2f,\n" +
                "    \"ramUsage\": %.2f,\n" +
                "    \"diskUsage\": %.2f\n" +
                "  }",
                escapeJSON(m.getAgentId()),
                m.getTimestamp(),
                DATE_FORMAT.format(new Date(m.getTimestamp())),
                m.getCpuUsage(),
                m.getRamUsage(),
                m.getDiskUsage()
            ));
            
            if (i < metrics.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Export alerts to CSV format.
     */
    public String exportAlertsToCSV(List<Alert> alerts) {
        StringBuilder sb = new StringBuilder();
        
        // Header
        sb.append("Agent ID,Timestamp,Date/Time,Severity,Type,Message\n");
        
        // Data rows
        for (Alert a : alerts) {
            sb.append(String.format("%s,%d,%s,%s,%s,%s\n",
                escapeCSV(a.getAgentId()),
                a.getTimestamp(),
                DATE_FORMAT.format(new Date(a.getTimestamp())),
                a.getLevel().toString(),
                a.getMetricType(),
                escapeCSV(a.getMessage())
            ));
        }
        
        return sb.toString();
    }
    
    /**
     * Export alerts to JSON format.
     */
    public String exportAlertsToJSON(List<Alert> alerts) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        
        for (int i = 0; i < alerts.size(); i++) {
            Alert a = alerts.get(i);
            sb.append(String.format(
                "  {\n" +
                "    \"agentId\": \"%s\",\n" +
                "    \"timestamp\": %d,\n" +
                "    \"dateTime\": \"%s\",\n" +
                "    \"severity\": \"%s\",\n" +
                "    \"metricType\": \"%s\",\n" +
                "    \"message\": \"%s\"\n" +
                "  }",
                escapeJSON(a.getAgentId()),
                a.getTimestamp(),
                DATE_FORMAT.format(new Date(a.getTimestamp())),
                a.getLevel().toString(),
                a.getMetricType(),
                escapeJSON(a.getMessage())
            ));
            
            if (i < alerts.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Escape special characters for CSV.
     */
    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
    
    /**
     * Escape special characters for JSON.
     */
    private String escapeJSON(String value) {
        if (value == null) return "";
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
