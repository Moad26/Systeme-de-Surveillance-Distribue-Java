package com.monitor.server;

import com.monitor.rmi.IMonitoringService;
import com.monitor.server.handler.TcpAlertHandler;
import com.monitor.server.handler.UdpListener;
import com.monitor.server.service.MonitoringServiceImpl;
import com.monitor.server.storage.AlertConfigManager;
import com.monitor.server.storage.DataManager;
import com.monitor.server.storage.MetricsPersistence;
import com.monitor.server.security.UserManager;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main server application.
 * Starts UDP listener, TCP alert handler, persistence, and exports RMI service.
 */
public class ServerApp {
    
    // Configuration
    private static final int UDP_PORT = 9876;
    private static final int TCP_PORT = 9877;
    private static final int RMI_PORT = 1099;
    private static final String RMI_SERVICE_NAME = "MonitoringService";
    
    private UdpListener udpListener;
    private TcpAlertHandler tcpAlertHandler;
    private MetricsPersistence metricsPersistence;
    private Thread udpThread;
    private Thread tcpThread;
    private ScheduledExecutorService statsScheduler;
    
    public void start() throws Exception {
        System.out.println("===========================================");
        System.out.println("   MONITORING SERVER STARTING");
        System.out.println("   UDP Port: " + UDP_PORT);
        System.out.println("   TCP Port: " + TCP_PORT);
        System.out.println("   RMI Port: " + RMI_PORT);
        System.out.println("===========================================");
        
        // Initialize DataManager (singleton)
        DataManager dataManager = DataManager.getInstance();
        
        // Initialize managers
        AlertConfigManager.getInstance();
        UserManager.getInstance();
        
        // Start metrics persistence (loads existing data)
        metricsPersistence = new MetricsPersistence(dataManager);
        metricsPersistence.start();
        
        // Start UDP listener
        udpListener = new UdpListener(UDP_PORT);
        udpThread = new Thread(udpListener, "UdpListener");
        udpThread.start();
        
        // Start TCP alert handler
        tcpAlertHandler = new TcpAlertHandler(TCP_PORT);
        tcpThread = new Thread(tcpAlertHandler, "TcpAlertHandler");
        tcpThread.start();
        
        // Create and export RMI service
        try {
            Registry registry = LocateRegistry.createRegistry(RMI_PORT);
            IMonitoringService service = new MonitoringServiceImpl();
            registry.rebind(RMI_SERVICE_NAME, service);
            System.out.println("[RMI] Service '" + RMI_SERVICE_NAME + "' exported on port " + RMI_PORT);
        } catch (Exception e) {
            System.err.println("[RMI] Failed to export service: " + e.getMessage());
            throw e;
        }
        
        // Start periodic stats logging
        statsScheduler = Executors.newSingleThreadScheduledExecutor();
        statsScheduler.scheduleAtFixedRate(() -> {
            System.out.println("[Stats] " + dataManager.getStats());
        }, 30, 30, TimeUnit.SECONDS);
        
        System.out.println("===========================================");
        System.out.println("   SERVER STARTED SUCCESSFULLY");
        System.out.println("   Features enabled:");
        System.out.println("   - Metrics persistence (JSON)");
        System.out.println("   - Statistics calculation");
        System.out.println("   - Configurable alerts");
        System.out.println("   - User authentication");
        System.out.println("   - Data export (CSV/JSON)");
        System.out.println("   Default admin: admin / admin123");
        System.out.println("   Waiting for agents...");
        System.out.println("===========================================");
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }
    
    public void stop() {
        System.out.println("[Server] Shutting down...");
        
        if (udpListener != null) {
            udpListener.stop();
        }
        
        if (tcpAlertHandler != null) {
            tcpAlertHandler.stop();
        }
        
        if (metricsPersistence != null) {
            metricsPersistence.stop();
        }
        
        if (statsScheduler != null) {
            statsScheduler.shutdown();
        }
        
        System.out.println("[Server] Stopped");
    }
    
    public static void main(String[] args) {
        try {
            ServerApp server = new ServerApp();
            server.start();
            
            // Keep running
            Thread.currentThread().join();
            
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
