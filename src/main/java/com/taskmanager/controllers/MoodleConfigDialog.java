package com.taskmanager.controllers;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.Optional;

public class MoodleConfigDialog {
    
    public static class MoodleCredentials {
        private final String username;
        private final String password;
        private final String token;
        private final boolean useToken;
        
        public MoodleCredentials(String username, String password) {
            this.username = username;
            this.password = password;
            this.token = null;
            this.useToken = false;
        }
        
        
        
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getToken() { return token; }
        public boolean isUseToken() { return useToken; }
    }
    
    public static Optional<MoodleCredentials> showConfigDialog() {
        Dialog<MoodleCredentials> dialog = new Dialog<>();
        dialog.setTitle("Moodle 配置");
        dialog.setHeaderText("請輸入您的 Moodle 憑證");
        
        // 設置按鈕類型
        ButtonType loginButtonType = new ButtonType("登錄", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);
        
        // 創建輸入欄位
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField usernameField = new TextField();
        usernameField.setPromptText("學號");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("密碼");
        
        
        
        grid.add(new Label("學號:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("密碼:"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(new Separator(), 0, 2, 2, 1);
        
        
        // 添加說明文字
        VBox content = new VBox(10);
        content.getChildren().add(grid);
        
       
        
        dialog.getDialogPane().setContent(content);
    
        // 設置焦點
        usernameField.requestFocus();
        
        // 轉換結果
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                String username = usernameField.getText().trim();
                String password = passwordField.getText().trim();
                if (!username.isEmpty() && !password.isEmpty()) {
                    return new MoodleCredentials(username, password);
                }
            }
            return null;
        });
        
        return dialog.showAndWait();
    }
    
    
} 