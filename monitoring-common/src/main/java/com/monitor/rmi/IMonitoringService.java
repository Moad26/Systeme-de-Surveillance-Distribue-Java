package com.monitor.rmi;

import com.monitor.model.Alert;
import com.monitor.model.Metric;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * RMI interface for the monitoring service.
 * Allows clients to query agents, metrics, and alerts.
 */
public interface IMonitoringService extends Remote {

    /**
     * Get list of all active agent IDs.
     * 
     * @return List of agent identifiers
     */
    List<String> getActiveAgents() throws RemoteException;

    /**
     * Get recent metrics for a specific agent.
     * 
     * @param agentId The agent identifier
     * @param limit   Maximum number of metrics to return
     * @return List of recent metrics
     */
    List<Metric> getMetrics(String agentId, int limit) throws RemoteException;

    /**
     * Get all metrics for a specific agent.
     * 
     * @param agentId The agent identifier
     * @return List of all stored metrics
     */
    List<Metric> getAllMetrics(String agentId) throws RemoteException;

    /**
     * Get alerts for a specific agent.
     * 
     * @param agentId The agent identifier (null for all agents)
     * @return List of alerts
     */
    List<Alert> getAlerts(String agentId) throws RemoteException;

    /**
     * Get all alerts from all agents.
     * 
     * @return List of all alerts
     */
    List<Alert> getAllAlerts() throws RemoteException;

    /**
     * Clear all alerts (for admin purposes).
     */
    void clearAlerts() throws RemoteException;
}
