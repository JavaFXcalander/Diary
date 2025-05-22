package com.taskmanager.controllers; // Package updated based on list_dir

import com.taskmanager.services.UserService; // Corrected UserService import
import com.taskmanager.services.AuthApi.AuthStatus; // Import AuthStatus
import com.taskmanager.services.UserSession; // Import UserSession
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorMessageLabel;

    private UserService userService; // Will be initialized to use AuthApi methods

    public void initialize() {
        userService = new UserService();
        errorMessageLabel.setText(""); 
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            errorMessageLabel.setText("Email and password cannot be empty.");
            return;
        }

        AuthStatus loginStatus = userService.loginUser(email, password);

        switch (loginStatus) {
            case SUCCESS:
                errorMessageLabel.setText(""); 
                System.out.println("Login successful! Navigating to calendar.");
                // Set the current user email in the UserSession
                UserSession.getInstance().setCurrentUserEmail(email);
                try {
                    mainScene(event, "/fxml/calendar.fxml", "My Diary Planner");
                } catch (IOException e) {
                    e.printStackTrace();
                    errorMessageLabel.setText("Error loading application.");
                }
                break;
            case USER_NOT_FOUND:
                errorMessageLabel.setText("User not found. Please check your email or register.");
                break;
            case INCORRECT_PASSWORD:
                errorMessageLabel.setText("Incorrect password. Please try again.");
                break;
            case INVALID_INPUT:
                 // UserService logs more specific validation details if any
                errorMessageLabel.setText("Invalid email or password format.");
                break;
            case DATABASE_ERROR:
                errorMessageLabel.setText("Login failed due to a server error. Please try again later.");
                break;
            default:
                errorMessageLabel.setText("Login failed. Please try again.");
                break;
        }
    }

    @FXML
    private void handleRegisterLink(ActionEvent event) {
        try {
            switchScene(event, "/fxml/register.fxml", "Register");
        } catch (IOException e) {
            e.printStackTrace();
            errorMessageLabel.setText("Error loading registration page.");
        }
    }

    
    private void switchScene(ActionEvent event, String fxmlFile, String title) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root, 400, 400); // Match scene size from MainApp
        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
    }



    private void mainScene(ActionEvent event, String fxmlFile, String title) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root, 1200, 1000); // Match scene size from MainApp
        stage.setTitle(title);
        stage.setScene(scene);

        stage.show();            // ① 先顯示，計算好視窗大小
        stage.centerOnScreen();
    }
}
