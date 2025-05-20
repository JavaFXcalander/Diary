package com.taskmanager.auth;

public interface AuthApi {
    void sendCode(String email) throws Exception;
}
