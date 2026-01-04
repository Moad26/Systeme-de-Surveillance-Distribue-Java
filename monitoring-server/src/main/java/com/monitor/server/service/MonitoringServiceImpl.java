package com.monitor.server.service;

import com.monitor.model.Alert;
import com.monitor.model.AlertConfig;
import com.monitor.model.Metric;
import com.monitor.model.MetricStatistics;
import com.monitor.model.User;
import com.monitor.model.User.Role;
import com.monitor.rmi.IMonitoringService;
import com.monitor.server.export.DataExporter;
import com.monitor.server.security.UserManager;
import com.monitor.server.storage.AlertConfigManager;
import com.monitor.server.storage.DataManager;

import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

/**
 * RMI implementation of the monitoring service.
 * Provides access to all monitoring features.
 */
public class MonitoringServiceImpl extends UnicastRemoteObject implements IMonitoringService {
    
    private static final long serialVersionUID = 1L;
    
    private final DataManager dataManager;
    private final UserManager userManager;
    private final AlertConfigManager alertConfigManager;
    private final StatisticsService statisticsService;
    private final DataExporter dataExporter;
    
    public MonitoringServiceImpl() throws RemoteException {
        super();
        this.dataManager = DataManager.getInstance();
        this.userManager = UserManager.getInstance();
        this.alertConfigManager = AlertConfigManager.getInstance();
        this.statisticsService = new StatisticsService();
        this.dataExporter = new DataExporter();
        System.out.println("[MonitoringServiceImpl] RMI service created with all features");
    }
    
    // ==================== Authentication ====================
    
    @Override
    public String authenticate(String username, String password) throws RemoteException {
        return userManager.authenticate(username, password);
    }
    
    @Override
    public void logout(String token) throws RemoteException {
        userManager.logout(token);
    }
    
    @Override
    public User getCurrentUser(String token) throws RemoteException {
        return userManager.getUserByToken(token);
    }
    
    // ==================== User Management ====================
    
    @Override
    public List<User> getAllUsers(String token) throws RemoteException {
        return userManager.getAllUsers(token);
    }
    
    @Override
    public boolean createUser(String token, String username, String password, String role) throws RemoteException {
        try {
            Role userRole = Role.valueOf(role.toUpperCase());
            return userManager.createUser(token, username, password, userRole);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    @Override
    public boolean deleteUser(String token, String username) throws RemoteException {
        return userManager.deleteUser(token, username);
    }
    
    @Override
    public boolean changePassword(String token, String oldPassword, String newPassword) throws RemoteException {
        return userManager.changePassword(token, oldPassword, newPassword);
    }
    
    // ==================== Agents ====================
    
    @Override
    public List<String> getActiveAgents() throws RemoteException {
        List<String> agents = dataManager.getActiveAgents();
        System.out.println("[RMI] getActiveAgents() -> " + agents.size() + " agents");
        return agents;
    }
    
    @Override
    public List<String> searchAgents(String query) throws RemoteException {
        List<String> agents = dataManager.searchAgents(query);
        System.out.println("[RMI] searchAgents('" + query + "') -> " + agents.size() + " agents");
        return agents;
    }
    
    // ==================== Metrics ====================
    
    @Override
    public List<Metric> getMetrics(String agentId, int limit) throws RemoteException {
        List<Metric> metrics = dataManager.getMetrics(agentId, limit);
        System.out.println("[RMI] getMetrics(" + agentId + ", " + limit + ") -> " + metrics.size() + " metrics");
        return metrics;
    }
    
    @Override
    public List<Metric> getAllMetrics(String agentId) throws RemoteException {
        List<Metric> metrics = dataManager.getAllMetrics(agentId);
        System.out.println("[RMI] getAllMetrics(" + agentId + ") -> " + metrics.size() + " metrics");
        return metrics;
    }
    
    @Override
    public List<Metric> getMetricsByDateRange(String agentId, long fromTime, long toTime) throws RemoteException {
        List<Metric> metrics = dataManager.getMetricsByDateRange(agentId, fromTime, toTime);
        System.out.println("[RMI] getMetricsByDateRange(" + agentId + ") -> " + metrics.size() + " metrics");
        return metrics;
    }
    
    // ==================== Statistics ====================
    
    @Override
    public MetricStatistics getStatistics(String agentId, long fromTime, long toTime) throws RemoteException {
        List<Metric> metrics = dataManager.getAllMetrics(agentId);
        MetricStatistics stats = statisticsService.calculateStatistics(agentId, metrics, fromTime, toTime);
        System.out.println("[RMI] getStatistics(" + agentId + ") -> " + stats);
        return stats;
    }
    
    // ==================== Alerts ====================
    
    @Override
    public List<Alert> getAlerts(String agentId) throws RemoteException {
        List<Alert> alerts = dataManager.getAlerts(agentId);
        System.out.println("[RMI] getAlerts(" + agentId + ") -> " + alerts.size() + " alerts");
        return alerts;
    }
    
    @Override
    public List<Alert> getAllAlerts() throws RemoteException {
        List<Alert> alerts = dataManager.getAllAlerts();
        System.out.println("[RMI] getAllAlerts() -> " + alerts.size() + " alerts");
        return alerts;
    }
    
    @Override
    public List<Alert> getAlertsByFilter(String agentId, String severity, long fromTime, long toTime) throws RemoteException {
        List<Alert> alerts = dataManager.getAlertsByFilter(agentId, severity, fromTime, toTime);
        System.out.println("[RMI] getAlertsByFilter() -> " + alerts.size() + " alerts");
        return alerts;
    }
    
    @Override
    public void clearAlerts() throws RemoteException {
        dataManager.clearAlerts();
        System.out.println("[RMI] clearAlerts() executed");
    }
    
    // ==================== Alert Configuration ====================
    
    @Override
    public List<AlertConfig> getAlertConfigs() throws RemoteException {
        return alertConfigManager.getAllConfigs();
    }
    
    @Override
    public void updateAlertConfig(String token, AlertConfig config) throws RemoteException {
        User user = userManager.getUserByToken(token);
        
        if (user == null || !user.canConfigure()) {
            throw new RemoteException("Permission denied: requires ADMIN or OPERATOR role");
        }
        
        alertConfigManager.updateConfig(config);
        System.out.println("[RMI] updateAlertConfig() by " + user.getUsername() + ": " + config);
    }
    
    // ==================== Export ====================
    
    @Override
    public byte[] exportMetricsCSV(String agentId, long fromTime, long toTime) throws RemoteException {
        List<Metric> metrics = dataManager.getMetricsByDateRange(agentId, fromTime, toTime);
        String csv = dataExporter.exportMetricsToCSV(metrics);
        System.out.println("[RMI] exportMetricsCSV(" + agentId + ") -> " + metrics.size() + " rows");
        return csv.getBytes(StandardCharsets.UTF_8);
    }
    
    @Override
    public byte[] exportMetricsJSON(String agentId, long fromTime, long toTime) throws RemoteException {
        List<Metric> metrics = dataManager.getMetricsByDateRange(agentId, fromTime, toTime);
        String json = dataExporter.exportMetricsToJSON(metrics);
        System.out.println("[RMI] exportMetricsJSON(" + agentId + ") -> " + metrics.size() + " rows");
        return json.getBytes(StandardCharsets.UTF_8);
    }
    
    @Override
    public byte[] exportAlertsCSV(long fromTime, long toTime) throws RemoteException {
        List<Alert> alerts = dataManager.getAlertsByFilter(null, null, fromTime, toTime);
        String csv = dataExporter.exportAlertsToCSV(alerts);
        System.out.println("[RMI] exportAlertsCSV() -> " + alerts.size() + " rows");
        return csv.getBytes(StandardCharsets.UTF_8);
    }
    
    @Override
    public byte[] exportAlertsJSON(long fromTime, long toTime) throws RemoteException {
        List<Alert> alerts = dataManager.getAlertsByFilter(null, null, fromTime, toTime);
        String json = dataExporter.exportAlertsToJSON(alerts);
        System.out.println("[RMI] exportAlertsJSON() -> " + alerts.size() + " rows");
        return json.getBytes(StandardCharsets.UTF_8);
    }
}
