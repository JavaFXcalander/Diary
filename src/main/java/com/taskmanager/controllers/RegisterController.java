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

public class RegisterController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label errorMessageLabel;

    private UserService userService;

    public void initialize() {
        userService = new UserService();
        errorMessageLabel.setText(""); // Clear error message on init
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            errorMessageLabel.setText("All fields are required.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            errorMessageLabel.setText("Passwords do not match.");
            return;
        }

        boolean registrationSuccess = userService.register(email, password, confirmPassword);

        if (registrationSuccess) {
            errorMessageLabel.setText(""); // Clear error message
            System.out.println("Registration successful! Navigating to login.");
            try {
                // Navigate to the login screen
                switchScene(event, "/fxml/login.fxml", "Login");
            } catch (IOException e) {
                e.printStackTrace();
                errorMessageLabel.setText("Error loading login page.");
            }
        } else {
            // UserService will print more specific errors to console for now
            errorMessageLabel.setText("Registration failed. See console for details."); 
        }
    }

    @FXML
    private void handleLoginLink(ActionEvent event) {
        try {
            // Navigate to the login screen
            switchScene(event, "/fxml/login.fxml", "Login");
        } catch (IOException e) {
            e.printStackTrace();
            errorMessageLabel.setText("Error loading login page.");
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
