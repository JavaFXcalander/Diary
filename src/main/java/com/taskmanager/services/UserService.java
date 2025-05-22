package com.taskmanager.services;

import com.taskmanager.database.DiaryDatabase;
import com.taskmanager.models.UserModel;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class UserService implements AuthApi { 
    private final EmailValidator validator = new EmailValidator();
    private final PwdValidator pwdvalidator = new PwdValidator();
    private final DiaryDatabase diaryDatabase;

    public UserService() {
        this.diaryDatabase = DiaryDatabase.getInstance();
    }

    public UserService(DiaryDatabase diaryDatabase) {
        this.diaryDatabase = diaryDatabase;
    }

    @Override
    public AuthStatus loginUser(String email, String password) {
        System.out.println("Attempting login for email: " + email);
        if (!validator.isValid(email) || password == null || password.isEmpty()) {
            return AuthStatus.INVALID_INPUT;
        }

        UserModel user = diaryDatabase.getUserEntry(email);
        if (user == null) {
            System.out.println("Login failed: User not found - " + email);
            return AuthStatus.USER_NOT_FOUND;
        }

        if (BCrypt.checkpw(password, user.getHashedPassword())) {
            System.out.println("Login successful for: " + email);
            return AuthStatus.SUCCESS;
        } else {
            System.out.println("Login failed: Incorrect password for - " + email);
            return AuthStatus.INCORRECT_PASSWORD;
        }
    }

    @Override
    public AuthStatus registerUser(String email, String password, String confirmPassword) {
        System.out.println("Attempting registration for email: " + email);
        if (!validator.isValid(email)) {
            System.out.println("Registration failed: Invalid email format.");
            return AuthStatus.INVALID_INPUT;
        }
        if (!pwdvalidator.isValid(password)) {
            System.out.println("Registration failed: Password does not meet criteria.");
            return AuthStatus.INVALID_INPUT;
        }
        if (!password.equals(confirmPassword)) {
            System.out.println("Registration failed: Passwords do not match.");
            return AuthStatus.INVALID_INPUT; 
        }

        if (userExists(email)) {
            System.out.println("Registration failed: Email already exists - " + email);
            return AuthStatus.EMAIL_ALREADY_EXISTS;
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        UserModel newUser = new UserModel();
        newUser.setEmail(email);
        newUser.setHashedPassword(hashedPassword);
        
        try {
            diaryDatabase.saveUserEntry(newUser); 
            System.out.println("Registration successful for: " + email);
            return AuthStatus.SUCCESS;
        } catch (RuntimeException e) { 
            if (e.getCause() instanceof SQLException && ((SQLException)e.getCause()).getSQLState().startsWith("23")) { 
                 System.out.println("Registration failed: Email already exists (database constraint) - " + email);
                 return AuthStatus.EMAIL_ALREADY_EXISTS;
                }
                e.printStackTrace();
                System.err.println("Registration failed due to database error: " + e.getMessage());
            return AuthStatus.DATABASE_ERROR;
        }
    }

    @Override
    public boolean userExists(String email) {
        return diaryDatabase.getUserEntry(email) != null;
    }
    
    

}

class EmailValidator {
    private static final Pattern P = Pattern.compile(
        "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$",
        Pattern.CASE_INSENSITIVE); 
    public boolean isValid(String email){ 
        return email != null && P.matcher(email).matches();
    }
}

class PwdValidator {
    private static final Pattern PWD_PATTERN =
    Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,20}$");

    public boolean isValid(String pwd){
        return pwd != null && PWD_PATTERN.matcher(pwd).matches();
    }
}
