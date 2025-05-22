package com.taskmanager.services;

/**
 * Singleton class to maintain the current user session across the application.
 * This allows different parts of the application to access the currently logged-in user's email.
 */
public class UserSession {
    private static UserSession instance;
    private String currentUserEmail;

    private UserSession() {
        // Private constructor to enforce singleton pattern
    }

    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public String getCurrentUserEmail() {
        return currentUserEmail;
    }

    public void setCurrentUserEmail(String email) {
        this.currentUserEmail = email;
    }

    public boolean isLoggedIn() {
        return currentUserEmail != null && !currentUserEmail.isEmpty();
    }

    public void logout() {
        currentUserEmail = null;
    }
}
