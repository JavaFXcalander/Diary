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

    // Moodle 憑證字段
    @DatabaseField
    private String moodleToken;
    
    @DatabaseField
    private String moodleUsername;
    
    @DatabaseField
    private long moodleLastLoginTime;

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
    
    // Moodle 憑證相關方法
    public String getMoodleToken() {
        return moodleToken;
    }
    
    public void setMoodleToken(String moodleToken) {
        this.moodleToken = moodleToken;
    }
    
    public String getMoodleUsername() {
        return moodleUsername;
    }
    
    public void setMoodleUsername(String moodleUsername) {
        this.moodleUsername = moodleUsername;
    }
    
    public long getMoodleLastLoginTime() {
        return moodleLastLoginTime;
    }
    
    public void setMoodleLastLoginTime(long moodleLastLoginTime) {
        this.moodleLastLoginTime = moodleLastLoginTime;
    }
    
    /**
     * 檢查Moodle憑證是否已配置
     */
    public boolean hasMoodleCredentials() {
        return moodleToken != null && !moodleToken.trim().isEmpty();
    }
    
    /**
     * 清除Moodle憑證
     */
    public void clearMoodleCredentials() {
        this.moodleToken = null;
        this.moodleUsername = null;
        this.moodleLastLoginTime = 0;
    }
}
