package com.monitor.rmi;

import com.monitor.model.Alert;
import com.monitor.model.AlertConfig;
import com.monitor.model.Metric;
import com.monitor.model.MetricStatistics;
import com.monitor.model.User;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * RMI interface for the monitoring service.
 * Provides access to metrics, alerts, statistics, and user management.
 */
public interface IMonitoringService extends Remote {

    // ==================== Authentication ====================

    /**
     * Authenticate user and get session token.
     * 
     * @param username The username
     * @param password The password
     * @return Session token if successful, null otherwise
     */
    String authenticate(String username, String password) throws RemoteException;

    /**
     * Logout and invalidate session token.
     */
    void logout(String token) throws RemoteException;

    /**
     * Get current user by session token.
     */
    User getCurrentUser(String token) throws RemoteException;

    // ==================== User Management ====================

    /**
     * Get all users (admin only).
     */
    List<User> getAllUsers(String token) throws RemoteException;

    /**
     * Create a new user (admin only).
     */
    boolean createUser(String token, String username, String password, String role) throws RemoteException;

    /**
     * Delete a user (admin only).
     */
    boolean deleteUser(String token, String username) throws RemoteException;

    /**
     * Change current user's password.
     */
    boolean changePassword(String token, String oldPassword, String newPassword) throws RemoteException;

    // ==================== Agents ====================

    /**
     * Get list of all active agent IDs.
     */
    List<String> getActiveAgents() throws RemoteException;

    /**
     * Search agents by name pattern.
     */
    List<String> searchAgents(String query) throws RemoteException;

    // ==================== Metrics ====================

    /**
     * Get recent metrics for a specific agent.
     * 
     * @param agentId The agent identifier
     * @param limit   Maximum number of metrics to return
     */
    List<Metric> getMetrics(String agentId, int limit) throws RemoteException;

    /**
     * Get all metrics for a specific agent.
     */
    List<Metric> getAllMetrics(String agentId) throws RemoteException;

    /**
     * Get metrics within a date range.
     */
    List<Metric> getMetricsByDateRange(String agentId, long fromTime, long toTime) throws RemoteException;

    // ==================== Statistics ====================

    /**
     * Get statistics for an agent within a time range.
     */
    MetricStatistics getStatistics(String agentId, long fromTime, long toTime) throws RemoteException;

    // ==================== Alerts ====================

    /**
     * Get alerts for a specific agent.
     * 
     * @param agentId The agent identifier (null for all agents)
     */
    List<Alert> getAlerts(String agentId) throws RemoteException;

    /**
     * Get all alerts from all agents.
     */
    List<Alert> getAllAlerts() throws RemoteException;

    /**
     * Get alerts with filters.
     */
    List<Alert> getAlertsByFilter(String agentId, String severity, long fromTime, long toTime) throws RemoteException;

    /**
     * Clear all alerts.
     */
    void clearAlerts() throws RemoteException;

    // ==================== Alert Configuration ====================

    /**
     * Get all alert configurations.
     */
    List<AlertConfig> getAlertConfigs() throws RemoteException;

    /**
     * Update an alert configuration.
     */
    void updateAlertConfig(String token, AlertConfig config) throws RemoteException;

    // ==================== Export ====================

    /**
     * Export metrics to CSV format.
     */
    byte[] exportMetricsCSV(String agentId, long fromTime, long toTime) throws RemoteException;

    /**
     * Export metrics to JSON format.
     */
    byte[] exportMetricsJSON(String agentId, long fromTime, long toTime) throws RemoteException;

    /**
     * Export alerts to CSV format.
     */
    byte[] exportAlertsCSV(long fromTime, long toTime) throws RemoteException;

    /**
     * Export alerts to JSON format.
     */
    byte[] exportAlertsJSON(long fromTime, long toTime) throws RemoteException;
}
