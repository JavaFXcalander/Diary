package com.taskmanager.auth;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class SendCodeService extends Service<Void> {
    private final String email;
    private final AuthApi api; // Depends on AuthApi interface

    public SendCodeService(String email, AuthApi api) {
        this.email = email;
        this.api = api;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                // This calls the external API
                api.sendCode(email); // Intended to be a POST /send-code
                return null;
            }
        };
    }
}
