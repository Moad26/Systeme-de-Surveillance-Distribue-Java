package com.monitor.ui.controller;

import com.monitor.model.Alert;
import com.monitor.model.Metric;
import com.monitor.rmi.IMonitoringService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.util.Duration;

import java.net.URL;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the monitoring dashboard.
 * Connects to RMI service and updates UI periodically.
 */
public class DashboardController implements Initializable {
    
    // Configuration
    private static final String SERVER_HOST = "localhost";
    private static final int RMI_PORT = 1099;
    private static final String RMI_SERVICE_NAME = "MonitoringService";
    private static final int POLL_INTERVAL_MS = 2000;
    private static final int MAX_DATA_POINTS = 30;
    
    // FXML components
    @FXML private Label statusLabel;
    @FXML private ListView<String> agentsList;
    
    @FXML private LineChart<String, Number> cpuChart;
    @FXML private LineChart<String, Number> ramChart;
    @FXML private LineChart<String, Number> diskChart;
    
    @FXML private CategoryAxis cpuXAxis;
    @FXML private CategoryAxis ramXAxis;
    @FXML private CategoryAxis diskXAxis;
    
    @FXML private TableView<Alert> alertsTable;
    @FXML private TableColumn<Alert, String> alertTimeCol;
    @FXML private TableColumn<Alert, String> alertAgentCol;
    @FXML private TableColumn<Alert, String> alertLevelCol;
    @FXML private TableColumn<Alert, String> alertTypeCol;
    @FXML private TableColumn<Alert, String> alertMessageCol;
    
    // RMI service
    private IMonitoringService monitoringService;
    private Timeline pollingTimeline;
    
    // Chart data
    private XYChart.Series<String, Number> cpuSeries;
    private XYChart.Series<String, Number> ramSeries;
    private XYChart.Series<String, Number> diskSeries;
    
    // Selected agent
    private String selectedAgentId = null;
    
    // Date formatter
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("[Dashboard] Initializing...");
        
        // Initialize charts
        initializeCharts();
        
        // Initialize alerts table
        initializeAlertsTable();
        
        // Connect to RMI service
        connectToServer();
        
        // Start polling
        startPolling();
    }
    
    /**
     * Initialize chart series.
     */
    private void initializeCharts() {
        cpuSeries = new XYChart.Series<>();
        cpuSeries.setName("CPU");
        cpuChart.getData().add(cpuSeries);
        
        ramSeries = new XYChart.Series<>();
        ramSeries.setName("RAM");
        ramChart.getData().add(ramSeries);
        
        diskSeries = new XYChart.Series<>();
        diskSeries.setName("Disk");
        diskChart.getData().add(diskSeries);
    }
    
    /**
     * Initialize alerts table columns.
     */
    private void initializeAlertsTable() {
        alertTimeCol.setCellValueFactory(data -> 
            new SimpleStringProperty(timeFormat.format(new Date(data.getValue().getTimestamp()))));
        
        alertAgentCol.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getAgentId()));
        
        alertLevelCol.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getLevel().toString()));
        
        alertTypeCol.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getMetricType()));
        
        alertMessageCol.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getMessage()));
        
        // Add severity styling
        alertLevelCol.setCellFactory(column -> new TableCell<Alert, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "CRITICAL":
                            setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                            break;
                        case "WARNING":
                            setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("-fx-text-fill: #3b82f6;");
                    }
                }
            }
        });
    }
    
    /**
     * Connect to the RMI server.
     */
    private void connectToServer() {
        try {
            Registry registry = LocateRegistry.getRegistry(SERVER_HOST, RMI_PORT);
            monitoringService = (IMonitoringService) registry.lookup(RMI_SERVICE_NAME);
            updateStatus("Connected", true);
            System.out.println("[Dashboard] Connected to RMI server");
        } catch (Exception e) {
            updateStatus("Disconnected", false);
            System.err.println("[Dashboard] Failed to connect: " + e.getMessage());
        }
    }
    
    /**
     * Start periodic polling.
     */
    private void startPolling() {
        pollingTimeline = new Timeline(new KeyFrame(
            Duration.millis(POLL_INTERVAL_MS),
            event -> refreshData()
        ));
        pollingTimeline.setCycleCount(Timeline.INDEFINITE);
        pollingTimeline.play();
    }
    
    /**
     * Refresh data from server.
     */
    private void refreshData() {
        if (monitoringService == null) {
            connectToServer();
            return;
        }
        
        try {
            // Update agents list
            List<String> agents = monitoringService.getActiveAgents();
            Platform.runLater(() -> {
                ObservableList<String> items = FXCollections.observableArrayList(agents);
                
                // Preserve selection
                String currentSelection = agentsList.getSelectionModel().getSelectedItem();
                agentsList.setItems(items);
                
                if (currentSelection != null && items.contains(currentSelection)) {
                    agentsList.getSelectionModel().select(currentSelection);
                } else if (!items.isEmpty() && selectedAgentId == null) {
                    agentsList.getSelectionModel().selectFirst();
                    selectedAgentId = items.get(0);
                }
            });
            
            // Update charts if agent selected
            if (selectedAgentId != null) {
                updateCharts();
            }
            
            // Update alerts
            updateAlerts();
            
            updateStatus("Connected", true);
            
        } catch (Exception e) {
            updateStatus("Connection lost", false);
            System.err.println("[Dashboard] Error refreshing data: " + e.getMessage());
            monitoringService = null;
        }
    }
    
    /**
     * Update charts with latest metrics.
     */
    private void updateCharts() {
        try {
            List<Metric> metrics = monitoringService.getMetrics(selectedAgentId, MAX_DATA_POINTS);
            
            Platform.runLater(() -> {
                cpuSeries.getData().clear();
                ramSeries.getData().clear();
                diskSeries.getData().clear();
                
                for (Metric metric : metrics) {
                    String time = timeFormat.format(new Date(metric.getTimestamp()));
                    cpuSeries.getData().add(new XYChart.Data<>(time, metric.getCpuUsage()));
                    ramSeries.getData().add(new XYChart.Data<>(time, metric.getRamUsage()));
                    diskSeries.getData().add(new XYChart.Data<>(time, metric.getDiskUsage()));
                }
            });
            
        } catch (Exception e) {
            System.err.println("[Dashboard] Error updating charts: " + e.getMessage());
        }
    }
    
    /**
     * Update alerts table.
     */
    private void updateAlerts() {
        try {
            List<Alert> alerts = monitoringService.getAllAlerts();
            
            Platform.runLater(() -> {
                // Reverse order to show newest first
                ObservableList<Alert> items = FXCollections.observableArrayList();
                for (int i = alerts.size() - 1; i >= 0; i--) {
                    items.add(alerts.get(i));
                }
                alertsTable.setItems(items);
            });
            
        } catch (Exception e) {
            System.err.println("[Dashboard] Error updating alerts: " + e.getMessage());
        }
    }
    
    /**
     * Update connection status label.
     */
    private void updateStatus(String text, boolean connected) {
        Platform.runLater(() -> {
            statusLabel.setText(text);
            statusLabel.setStyle(connected ? 
                "-fx-text-fill: #10b981;" : 
                "-fx-text-fill: #ef4444;");
        });
    }
    
    /**
     * Handle agent selection.
     */
    @FXML
    private void onAgentSelected() {
        String selected = agentsList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selectedAgentId = selected;
            System.out.println("[Dashboard] Selected agent: " + selectedAgentId);
            updateCharts();
        }
    }
    
    /**
     * Handle refresh button click.
     */
    @FXML
    private void onRefresh() {
        System.out.println("[Dashboard] Manual refresh triggered");
        refreshData();
    }
    
    /**
     * Handle clear alerts button click.
     */
    @FXML
    private void onClearAlerts() {
        try {
            if (monitoringService != null) {
                monitoringService.clearAlerts();
                updateAlerts();
                System.out.println("[Dashboard] Alerts cleared");
            }
        } catch (Exception e) {
            System.err.println("[Dashboard] Error clearing alerts: " + e.getMessage());
        }
    }
    
    /**
     * Cleanup when closing.
     */
    public void shutdown() {
        if (pollingTimeline != null) {
            pollingTimeline.stop();
        }
    }
}
