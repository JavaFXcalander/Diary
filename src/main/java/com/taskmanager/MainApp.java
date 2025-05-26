package com.taskmanager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.application.HostServices;
import com.taskmanager.services.CalendarEventSyncService;
import com.taskmanager.database.CalendarEventDatabase;
import java.io.IOException;

public class MainApp extends Application {
    private static HostServices hostServices;

    @Override
    public void start(Stage primaryStage) throws IOException {
        hostServices = getHostServices();
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
        Scene scene = new Scene(root, 400, 400);
        primaryStage.setTitle("Login");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        // 清理同步服務資源
        try {
            CalendarEventSyncService syncService = CalendarEventSyncService.getInstance();
            syncService.shutdown();
            System.out.println("同步服務已關閉");
        } catch (Exception e) {
            System.err.println("關閉同步服務時發生錯誤: " + e.getMessage());
        }
        
        // 關閉資料庫連接
        try {
            CalendarEventDatabase.getInstance().close();
            System.out.println("事件資料庫連接已關閉");
        } catch (Exception e) {
            System.err.println("關閉事件資料庫時發生錯誤: " + e.getMessage());
        }
        
        super.stop();
    }

    public static HostServices getHostServicesInstance() {
        return hostServices;
    }

    public static void main(String[] args) {
        launch(args);
    }
}