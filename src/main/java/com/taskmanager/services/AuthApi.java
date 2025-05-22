package com.taskmanager.services;

public interface AuthApi {

    enum AuthStatus {
        SUCCESS,
        EMAIL_ALREADY_EXISTS,
        USER_NOT_FOUND,
        INCORRECT_PASSWORD,
        INVALID_INPUT,
        DATABASE_ERROR,
        REGISTRATION_FAILED_UNKNOWN // Generic registration failure
    }

    AuthStatus registerUser(String email, String password, String confirmPassword);

    AuthStatus loginUser(String email, String password);

    boolean userExists(String email);
    
    
}
