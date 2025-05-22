package com.taskmanager.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * 
 */
@DatabaseTable(tableName = "user")
public class UserModel {
    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(unique = true)
    private String email;

    @DatabaseField
    private String hashedPassword;

    public UserModel() {
        // ORMLite 需要一個無參構造器
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }
}
