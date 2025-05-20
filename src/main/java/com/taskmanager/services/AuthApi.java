package com.taskmanager.services; // Placing in services package

public interface AuthApi {
    /**
     * Attempts to log in a user.
     * @param email The user's email.
     * @param password The user's password.
     * @return true if login is successful, false otherwise.
     * @throws Exception if an error occurs during the login process.
     */
    boolean login(String email, String password) throws Exception;

    /**
     * Attempts to register a new user.
     * @param email The user's email.
     * @param password The user's password.
     * @return true if registration is successful, false otherwise.
     * @throws Exception if an error occurs during the registration process.
     */
    boolean register(String email, String password) throws Exception;

    // From MEMORY[17b741d2-cda0-467d-8f92-5c74771847c2]
    // /**
    //  * Sends a verification code to the given email.
    //  * @param toEmail The email address to send the code to.
    //  * @throws Exception if an error occurs during sending.
    //  */
    // void sendCode(String toEmail) throws Exception; // Uncomment if needed
}
