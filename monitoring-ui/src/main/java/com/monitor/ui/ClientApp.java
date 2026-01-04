package com.monitor.ui;

import com.monitor.ui.controller.DashboardController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main JavaFX application for the monitoring client.
 */
public class ClientApp extends Application {
    
    private DashboardController controller;
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("===========================================");
        System.out.println("   MONITORING CLIENT STARTING");
        System.out.println("===========================================");
        
        // Load FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
        Parent root = loader.load();
        
        // Get controller reference for cleanup
        controller = loader.getController();
        
        // Create scene
        Scene scene = new Scene(root, 1400, 900);
        
        // Configure stage
        primaryStage.setTitle("System Monitoring Dashboard");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(700);
        
        // Handle close
        primaryStage.setOnCloseRequest(event -> {
            if (controller != null) {
                controller.shutdown();
            }
            System.out.println("[ClientApp] Shutting down...");
        });
        
        primaryStage.show();
        
        System.out.println("[ClientApp] Dashboard opened");
    }
    
    @Override
    public void stop() throws Exception {
        super.stop();
        System.out.println("[ClientApp] Application stopped");
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
