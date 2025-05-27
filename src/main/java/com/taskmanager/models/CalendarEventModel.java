package com.taskmanager.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import java.time.LocalDate;

@DatabaseTable(tableName = "calendar")
public class CalendarEventModel {
    
    @DatabaseField(generatedId = true)
    private int id;
    
    @DatabaseField(canBeNull = false)
    private String googleEventId; // Google Calendar 事件的唯一 ID
    
    @DatabaseField(canBeNull = false)
    private String userEmail; // 關聯到用戶
    
    @DatabaseField(canBeNull = false)
    private String summary; // 事件標題
    
    @DatabaseField
    private String description; // 事件描述
    
    @DatabaseField(canBeNull = false)
    private long startTime; // 開始時間（毫秒時間戳）
    
    @DatabaseField(canBeNull = false)
    private long endTime; // 結束時間（毫秒時間戳）
    
    @DatabaseField(canBeNull = false)
    private String eventDate; // 事件日期（YYYY-MM-DD 格式）
    
    @DatabaseField
    private long createdAt; // 建立時間
    
    @DatabaseField
    private long updatedAt; // 更新時間
    
    
    // 無參數建構子（ORMLite 需要）
    public CalendarEventModel() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    // 完整建構子
    public CalendarEventModel(String googleEventId, String userEmail, String summary, 
                             String description, long startTime, long endTime, LocalDate eventDate) {
        this();
        this.googleEventId = googleEventId;
        this.userEmail = userEmail;
        this.summary = summary;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.eventDate = eventDate.toString();
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getGoogleEventId() {
        return googleEventId;
    }
    
    public void setGoogleEventId(String googleEventId) {
        this.googleEventId = googleEventId;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(long startTime) {
        this.startTime = startTime;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    public void setEndTime(long endTime) {
        this.endTime = endTime;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getEventDate() {
        return eventDate;
    }
    
    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public LocalDate getEventDateAsLocalDate() {
        return LocalDate.parse(eventDate);
    }
    
    public void setEventDateFromLocalDate(LocalDate date) {
        this.eventDate = date.toString();
        this.updatedAt = System.currentTimeMillis();
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
   
    
    @Override
    public String toString() {
        return "CalendarEventModel{" +
                "id=" + id +
                ", googleEventId='" + googleEventId + '\'' +
                ", userEmail='" + userEmail + '\'' +
                ", summary='" + summary + '\'' +
                ", eventDate='" + eventDate + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
} 