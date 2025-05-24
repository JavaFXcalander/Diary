package com.taskmanager.services;

import com.taskmanager.models.UserModel;

public class UserManager {
    private static UserManager instance;
    private UserModel currentUser;

    private UserManager() {}

    public static UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    public void setCurrentUser(UserModel user) {
        this.currentUser = user;
    }

    public UserModel getCurrentUser() {
        return currentUser;
    }
} 