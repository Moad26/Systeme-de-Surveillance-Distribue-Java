package com.monitor.ui.controller;

import com.monitor.model.Alert;
import com.monitor.model.AlertConfig;
import com.monitor.model.Metric;
import com.monitor.model.MetricStatistics;
import com.monitor.model.User;
import com.monitor.rmi.IMonitoringService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the monitoring dashboard.
 * Includes authentication, statistics, filtering, and export features.
 */
public class DashboardController implements Initializable {
    
    // Configuration
    private static final String SERVER_HOST = "localhost";
    private static final int RMI_PORT = 1099;
    private static final String RMI_SERVICE_NAME = "MonitoringService";
    private static final int POLL_INTERVAL_MS = 2000;
    private static final int MAX_DATA_POINTS = 30;
    
    // FXML components - Header
    @FXML private Label statusLabel;
    @FXML private Label userLabel;
    @FXML private Button loginBtn;
    @FXML private Button configBtn;
    
    // FXML components - Toolbar
    @FXML private TextField searchField;
    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private ComboBox<String> severityFilter;
    
    // FXML components - Statistics
    @FXML private Label cpuStatLabel;
    @FXML private Label cpuTrendLabel;
    @FXML private Label ramStatLabel;
    @FXML private Label ramTrendLabel;
    @FXML private Label diskStatLabel;
    @FXML private Label diskTrendLabel;
    @FXML private Label samplesLabel;
    
    // FXML components - Agents list
    @FXML private ListView<String> agentsList;
    
    // FXML components - Charts
    @FXML private LineChart<String, Number> cpuChart;
    @FXML private LineChart<String, Number> ramChart;
    @FXML private LineChart<String, Number> diskChart;
    @FXML private CategoryAxis cpuXAxis;
    @FXML private CategoryAxis ramXAxis;
    @FXML private CategoryAxis diskXAxis;
    
    // FXML components - Alerts table
    @FXML private TableView<Alert> alertsTable;
    @FXML private TableColumn<Alert, String> alertTimeCol;
    @FXML private TableColumn<Alert, String> alertAgentCol;
    @FXML private TableColumn<Alert, String> alertLevelCol;
    @FXML private TableColumn<Alert, String> alertTypeCol;
    @FXML private TableColumn<Alert, String> alertMessageCol;
    
    // RMI service
    private IMonitoringService monitoringService;
    private Timeline pollingTimeline;
    
    // Authentication
    private String sessionToken = null;
    private User currentUser = null;
    
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
        
        // Initialize components
        initializeCharts();
        initializeAlertsTable();
        initializeDatePickers();
        initializeSeverityFilter();
        
        // Connect to RMI service
        connectToServer();
        
        // Start polling
        startPolling();
    }
    
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
                        case "CRITICAL" -> setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                        case "WARNING" -> setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                        default -> setStyle("-fx-text-fill: #3b82f6;");
                    }
                }
            }
        });
    }
    
    private void initializeDatePickers() {
        LocalDate today = LocalDate.now();
        fromDate.setValue(today.minusDays(1));
        toDate.setValue(today);
    }
    
    private void initializeSeverityFilter() {
        severityFilter.setItems(FXCollections.observableArrayList(
            "All", "CRITICAL", "WARNING", "INFO"
        ));
        severityFilter.setValue("All");
        severityFilter.setOnAction(e -> updateAlerts());
    }
    
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
    
    private void startPolling() {
        pollingTimeline = new Timeline(new KeyFrame(
            Duration.millis(POLL_INTERVAL_MS),
            event -> refreshData()
        ));
        pollingTimeline.setCycleCount(Timeline.INDEFINITE);
        pollingTimeline.play();
    }
    
    private void refreshData() {
        if (monitoringService == null) {
            connectToServer();
            return;
        }
        
        try {
            updateAgentsList();
            
            if (selectedAgentId != null) {
                updateCharts();
                updateStatistics();
            }
            
            updateAlerts();
            updateStatus("Connected", true);
            
        } catch (Exception e) {
            updateStatus("Connection lost", false);
            System.err.println("[Dashboard] Error refreshing: " + e.getMessage());
            monitoringService = null;
        }
    }
    
    private void updateAgentsList() {
        try {
            String query = searchField.getText();
            List<String> agents = (query != null && !query.isEmpty()) 
                ? monitoringService.searchAgents(query)
                : monitoringService.getActiveAgents();
            
            Platform.runLater(() -> {
                ObservableList<String> items = FXCollections.observableArrayList(agents);
                String currentSelection = agentsList.getSelectionModel().getSelectedItem();
                agentsList.setItems(items);
                
                if (currentSelection != null && items.contains(currentSelection)) {
                    agentsList.getSelectionModel().select(currentSelection);
                } else if (!items.isEmpty() && selectedAgentId == null) {
                    agentsList.getSelectionModel().selectFirst();
                    selectedAgentId = items.get(0);
                }
            });
        } catch (Exception e) {
            System.err.println("[Dashboard] Error updating agents: " + e.getMessage());
        }
    }
    
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
    
    private void updateStatistics() {
        try {
            long fromTime = getFromTimestamp();
            long toTime = getToTimestamp();
            
            MetricStatistics stats = monitoringService.getStatistics(selectedAgentId, fromTime, toTime);
            
            Platform.runLater(() -> {
                cpuStatLabel.setText(String.format("%.1f%%", stats.getCpuAvg()));
                cpuTrendLabel.setText(stats.getCpuTrend());
                setTrendStyle(cpuTrendLabel, stats.getCpuTrend());
                
                ramStatLabel.setText(String.format("%.1f%%", stats.getRamAvg()));
                ramTrendLabel.setText(stats.getRamTrend());
                setTrendStyle(ramTrendLabel, stats.getRamTrend());
                
                diskStatLabel.setText(String.format("%.1f%%", stats.getDiskAvg()));
                diskTrendLabel.setText(stats.getDiskTrend());
                setTrendStyle(diskTrendLabel, stats.getDiskTrend());
                
                samplesLabel.setText(String.valueOf(stats.getSampleCount()));
            });
        } catch (Exception e) {
            System.err.println("[Dashboard] Error updating statistics: " + e.getMessage());
        }
    }
    
    private void setTrendStyle(Label label, String trend) {
        label.getStyleClass().removeAll("trend-rising", "trend-falling", "trend-stable");
        switch (trend) {
            case "RISING" -> label.getStyleClass().add("trend-rising");
            case "FALLING" -> label.getStyleClass().add("trend-falling");
            default -> label.getStyleClass().add("trend-stable");
        }
    }
    
    private void updateAlerts() {
        try {
            String severity = severityFilter.getValue();
            if ("All".equals(severity)) severity = null;
            
            long fromTime = getFromTimestamp();
            long toTime = getToTimestamp();
            
            List<Alert> alerts = monitoringService.getAlertsByFilter(
                selectedAgentId, severity, fromTime, toTime
            );
            
            Platform.runLater(() -> {
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
    
    private long getFromTimestamp() {
        LocalDate date = fromDate.getValue();
        if (date == null) date = LocalDate.now().minusDays(1);
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
    
    private long getToTimestamp() {
        LocalDate date = toDate.getValue();
        if (date == null) date = LocalDate.now();
        return date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
    
    private void updateStatus(String text, boolean connected) {
        Platform.runLater(() -> {
            statusLabel.setText(text);
            statusLabel.setStyle(connected ? 
                "-fx-text-fill: #10b981;" : 
                "-fx-text-fill: #ef4444;");
        });
    }
    
    // ==================== Event Handlers ====================
    
    @FXML
    private void onAgentSelected() {
        String selected = agentsList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selectedAgentId = selected;
            updateCharts();
            updateStatistics();
        }
    }
    
    @FXML
    private void onSearch() {
        updateAgentsList();
    }
    
    @FXML
    private void onApplyDateFilter() {
        updateStatistics();
        updateAlerts();
    }
    
    @FXML
    private void onRefresh() {
        refreshData();
    }
    
    @FXML
    private void onClearAlerts() {
        try {
            if (monitoringService != null) {
                monitoringService.clearAlerts();
                updateAlerts();
            }
        } catch (Exception e) {
            showError("Error", "Failed to clear alerts: " + e.getMessage());
        }
    }
    
    @FXML
    private void onLogin() {
        if (sessionToken != null) {
            // Logout
            try {
                monitoringService.logout(sessionToken);
            } catch (Exception e) {
                // Ignore
            }
            sessionToken = null;
            currentUser = null;
            updateUserUI();
            return;
        }
        
        // Show login dialog
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Login");
        dialog.setHeaderText("Enter your credentials");
        
        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        
        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new String[]{username.getText(), password.getText()};
            }
            return null;
        });
        
        Optional<String[]> result = dialog.showAndWait();
        result.ifPresent(credentials -> {
            try {
                sessionToken = monitoringService.authenticate(credentials[0], credentials[1]);
                if (sessionToken != null) {
                    currentUser = monitoringService.getCurrentUser(sessionToken);
                    updateUserUI();
                } else {
                    showError("Login Failed", "Invalid username or password");
                }
            } catch (Exception e) {
                showError("Login Error", e.getMessage());
            }
        });
    }
    
    private void updateUserUI() {
        Platform.runLater(() -> {
            if (currentUser != null) {
                userLabel.setText(currentUser.getUsername() + " (" + currentUser.getRole() + ")");
                loginBtn.setText("Logout");
                configBtn.setDisable(!currentUser.canConfigure());
            } else {
                userLabel.setText("Not logged in");
                loginBtn.setText("Login");
                configBtn.setDisable(true);
            }
        });
    }
    
    @FXML
    private void onConfigureAlerts() {
        if (currentUser == null || !currentUser.canConfigure()) {
            showError("Access Denied", "You need ADMIN or OPERATOR role to configure alerts");
            return;
        }
        
        try {
            List<AlertConfig> configs = monitoringService.getAlertConfigs();
            
            // Simple config dialog
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Alert Configuration");
            dialog.setHeaderText("Configure alert thresholds");
            
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 20, 10, 10));
            
            grid.add(new Label("Metric"), 0, 0);
            grid.add(new Label("Warning (%)"), 1, 0);
            grid.add(new Label("Critical (%)"), 2, 0);
            
            TextField[] warningFields = new TextField[configs.size()];
            TextField[] criticalFields = new TextField[configs.size()];
            
            for (int i = 0; i < configs.size(); i++) {
                AlertConfig config = configs.get(i);
                grid.add(new Label(config.getMetricType()), 0, i + 1);
                
                warningFields[i] = new TextField(String.valueOf(config.getWarningThreshold()));
                warningFields[i].setPrefWidth(80);
                grid.add(warningFields[i], 1, i + 1);
                
                criticalFields[i] = new TextField(String.valueOf(config.getCriticalThreshold()));
                criticalFields[i].setPrefWidth(80);
                grid.add(criticalFields[i], 2, i + 1);
            }
            
            dialog.getDialogPane().setContent(grid);
            
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Save configurations
                for (int i = 0; i < configs.size(); i++) {
                    AlertConfig config = configs.get(i);
                    try {
                        config.setWarningThreshold(Double.parseDouble(warningFields[i].getText()));
                        config.setCriticalThreshold(Double.parseDouble(criticalFields[i].getText()));
                        monitoringService.updateAlertConfig(sessionToken, config);
                    } catch (NumberFormatException e) {
                        showError("Invalid Input", "Please enter valid numbers for thresholds");
                        return;
                    }
                }
                showInfo("Success", "Alert configurations updated");
            }
        } catch (Exception e) {
            showError("Error", "Failed to load configurations: " + e.getMessage());
        }
    }
    
    @FXML
    private void onExportCSV() {
        exportData("csv");
    }
    
    @FXML
    private void onExportJSON() {
        exportData("json");
    }
    
    private void exportData(String format) {
        if (selectedAgentId == null) {
            showError("Export Error", "Please select an agent first");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Metrics");
        fileChooser.setInitialFileName("metrics_" + selectedAgentId + "." + format);
        
        if ("csv".equals(format)) {
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        } else {
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        }
        
        File file = fileChooser.showSaveDialog(cpuChart.getScene().getWindow());
        
        if (file != null) {
            try {
                long fromTime = getFromTimestamp();
                long toTime = getToTimestamp();
                
                byte[] data;
                if ("csv".equals(format)) {
                    data = monitoringService.exportMetricsCSV(selectedAgentId, fromTime, toTime);
                } else {
                    data = monitoringService.exportMetricsJSON(selectedAgentId, fromTime, toTime);
                }
                
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(data);
                }
                
                showInfo("Export Successful", "Data exported to: " + file.getName());
                
            } catch (Exception e) {
                showError("Export Error", "Failed to export: " + e.getMessage());
            }
        }
    }
    
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    public void shutdown() {
        if (pollingTimeline != null) {
            pollingTimeline.stop();
        }
        if (sessionToken != null && monitoringService != null) {
            try {
                monitoringService.logout(sessionToken);
            } catch (Exception e) {
                // Ignore
            }
        }
    }
}
