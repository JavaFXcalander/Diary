package com.taskmanager.controllers; // Package updated based on list_dir

import com.taskmanager.services.UserService; // Corrected UserService import
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

    private UserService userService;

    public void initialize() {
        userService = new UserService();
        errorMessageLabel.setText(""); // Clear error message on init
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            errorMessageLabel.setText("Email and password cannot be empty.");
            return;
        }

        boolean loginSuccess = userService.login(email, password);

        if (loginSuccess) {
            errorMessageLabel.setText(""); // Clear error message
            System.out.println("Login successful! Navigating to calendar.");
            try {
                // Navigate to the main application screen (calendar.fxml)
                switchScene(event, "/fxml/calendar.fxml", "My Diary Planner");
            } catch (IOException e) {
                e.printStackTrace();
                errorMessageLabel.setText("Error loading application.");
            }
        } else {
            errorMessageLabel.setText("Invalid email or password. Please try again.");
        }
    }

    @FXML
    private void handleRegisterLink(ActionEvent event) {
        try {
            // Navigate to the registration screen
            switchScene(event, "/fxml/register.fxml", "Register");
        } catch (IOException e) {
            e.printStackTrace();
            errorMessageLabel.setText("Error loading registration page.");
        }
    }

    // Inline scene switching method (alternative to SceneUtil)
    private void switchScene(ActionEvent event, String fxmlFile, String title) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root, 1200, 1000); // Match scene size from MainApp
        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
    }
}
