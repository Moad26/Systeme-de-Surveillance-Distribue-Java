package com.monitor.agent;

import com.monitor.agent.collectors.CpuCollector;
import com.monitor.agent.collectors.DiskCollector;
import com.monitor.agent.collectors.ICollector;
import com.monitor.agent.collectors.MemoryCollector;
import com.monitor.agent.net.TcpClient;
import com.monitor.agent.net.UdpSender;
import com.monitor.model.Alert;
import com.monitor.model.Metric;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main application for the monitoring agent.
 * Collects system metrics and sends them to the server.
 */
public class AgentApp {
    
    // Configuration
    private static final String SERVER_HOST = "localhost";
    private static final int UDP_PORT = 9876;
    private static final int TCP_PORT = 9877;
    private static final int COLLECTION_INTERVAL_SECONDS = 5;
    
    // Alert thresholds (percentage)
    private static final double CPU_WARNING_THRESHOLD = 70.0;
    private static final double CPU_CRITICAL_THRESHOLD = 90.0;
    private static final double RAM_WARNING_THRESHOLD = 80.0;
    private static final double RAM_CRITICAL_THRESHOLD = 95.0;
    private static final double DISK_WARNING_THRESHOLD = 85.0;
    private static final double DISK_CRITICAL_THRESHOLD = 95.0;
    
    private final String agentId;
    private final ICollector cpuCollector;
    private final ICollector memoryCollector;
    private final ICollector diskCollector;
    private final UdpSender udpSender;
    private final TcpClient tcpClient;
    private final ScheduledExecutorService scheduler;
    
    public AgentApp() throws IOException {
        // Generate unique agent ID based on hostname
        this.agentId = InetAddress.getLocalHost().getHostName() + "-" + 
                       ProcessHandle.current().pid();
        
        // Initialize collectors
        this.cpuCollector = new CpuCollector();
        this.memoryCollector = new MemoryCollector();
        this.diskCollector = new DiskCollector();
        
        // Initialize network clients
        this.udpSender = new UdpSender(SERVER_HOST, UDP_PORT);
        this.tcpClient = new TcpClient(SERVER_HOST, TCP_PORT);
        
        // Initialize scheduler
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        
        System.out.println("===========================================");
        System.out.println("   MONITORING AGENT STARTED");
        System.out.println("   Agent ID: " + agentId);
        System.out.println("   Server: " + SERVER_HOST);
        System.out.println("   UDP Port: " + UDP_PORT);
        System.out.println("   TCP Port: " + TCP_PORT);
        System.out.println("   Interval: " + COLLECTION_INTERVAL_SECONDS + "s");
        System.out.println("===========================================");
    }
    
    /**
     * Start the agent.
     */
    public void start() throws IOException {
        udpSender.init();
        
        // Wait a moment for initial CPU measurement
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Schedule periodic collection
        scheduler.scheduleAtFixedRate(
            this::collectAndSend,
            0,
            COLLECTION_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
        
        System.out.println("[Agent] Collection started...");
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }
    
    /**
     * Collect metrics and send them.
     */
    private void collectAndSend() {
        try {
            // Collect metrics
            double cpu = cpuCollector.collect();
            double ram = memoryCollector.collect();
            double disk = diskCollector.collect();
            
            // Create and send metric via UDP
            Metric metric = new Metric(agentId, cpu, ram, disk);
            udpSender.sendMetric(metric);
            
            // Check thresholds and send alerts via TCP
            checkAndSendAlerts(cpu, ram, disk);
            
        } catch (Exception e) {
            System.err.println("[Agent] Error during collection: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Check thresholds and send alerts if needed.
     */
    private void checkAndSendAlerts(double cpu, double ram, double disk) {
        // CPU alerts
        if (cpu >= CPU_CRITICAL_THRESHOLD) {
            sendAlert("CPU", cpu, Alert.Severity.CRITICAL);
        } else if (cpu >= CPU_WARNING_THRESHOLD) {
            sendAlert("CPU", cpu, Alert.Severity.WARNING);
        }
        
        // RAM alerts
        if (ram >= RAM_CRITICAL_THRESHOLD) {
            sendAlert("RAM", ram, Alert.Severity.CRITICAL);
        } else if (ram >= RAM_WARNING_THRESHOLD) {
            sendAlert("RAM", ram, Alert.Severity.WARNING);
        }
        
        // Disk alerts
        if (disk >= DISK_CRITICAL_THRESHOLD) {
            sendAlert("DISK", disk, Alert.Severity.CRITICAL);
        } else if (disk >= DISK_WARNING_THRESHOLD) {
            sendAlert("DISK", disk, Alert.Severity.WARNING);
        }
    }
    
    /**
     * Send an alert via TCP.
     */
    private void sendAlert(String metricType, double value, Alert.Severity severity) {
        String message = String.format("%s usage at %.1f%% (threshold exceeded)", metricType, value);
        Alert alert = new Alert(agentId, message, severity, metricType);
        tcpClient.sendAlert(alert);
    }
    
    /**
     * Stop the agent.
     */
    public void stop() {
        System.out.println("[Agent] Shutting down...");
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        udpSender.close();
        System.out.println("[Agent] Stopped");
    }
    
    public static void main(String[] args) {
        try {
            AgentApp agent = new AgentApp();
            agent.start();
            
            // Keep running
            Thread.currentThread().join();
            
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to start agent: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
