package com.monitor.server.service;

import com.monitor.model.Alert;
import com.monitor.model.Metric;
import com.monitor.rmi.IMonitoringService;
import com.monitor.server.storage.DataManager;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

/**
 * RMI implementation of the monitoring service.
 * Provides access to metrics and alerts stored in DataManager.
 */
public class MonitoringServiceImpl extends UnicastRemoteObject implements IMonitoringService {
    
    private static final long serialVersionUID = 1L;
    
    private final DataManager dataManager;
    
    public MonitoringServiceImpl() throws RemoteException {
        super();
        this.dataManager = DataManager.getInstance();
        System.out.println("[MonitoringServiceImpl] RMI service created");
    }
    
    @Override
    public List<String> getActiveAgents() throws RemoteException {
        List<String> agents = dataManager.getActiveAgents();
        System.out.println("[RMI] getActiveAgents() -> " + agents.size() + " agents");
        return agents;
    }
    
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
    public void clearAlerts() throws RemoteException {
        dataManager.clearAlerts();
        System.out.println("[RMI] clearAlerts() executed");
    }
}
