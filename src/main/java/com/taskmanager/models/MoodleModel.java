package com.taskmanager.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import java.time.LocalDate;

@DatabaseTable(tableName = "moodle")
public class MoodleModel {
    
    @DatabaseField(generatedId = true)
    private int id;
    
    @DatabaseField
    private String username;
    
    @DatabaseField
    private String password;

    @DatabaseField(canBeNull = false)
    private String userEmail; // 關聯到用戶
    
    @DatabaseField(canBeNull = false)
    private String name; // 事件名稱
    
    
    @DatabaseField(canBeNull = false)
    private long timestart; // 開始時間（毫秒時間戳）
    
    @DatabaseField
    private String url; // 事件URL
    
    @DatabaseField
    private String courseName; // 課程名稱
    
    @DatabaseField
    private int courseId; // 課程ID
    
    @DatabaseField
    private int assignmentId; // 作業ID
    
    @DatabaseField
    private String submissionStatus; // 繳交狀態
    
    
    @DatabaseField(canBeNull = false)
    private String eventDate; // 事件日期（YYYY-MM-DD 格式）
    
    @DatabaseField
    private long createdAt; // 建立時間
    
    @DatabaseField
    private long updatedAt; // 更新時間
    
    // 無參數建構子（ORMLite 需要）
    public MoodleModel() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    // 完整建構子
    public MoodleModel(String userEmail, String name, String description, 
                       long timestart, String url, String courseName, 
                       int courseId, int assignmentId, LocalDate eventDate) {
        this();
        this.userEmail = userEmail;
        this.name = name;
        this.timestart = timestart;
        this.url = url;
        this.courseName = courseName;
        this.courseId = courseId;
        this.assignmentId = assignmentId;
        this.eventDate = eventDate.toString();
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
        this.updatedAt = System.currentTimeMillis();
    }
    
    
    
    public long getTimestart() {
        return timestart;
    }
    
    public void setTimestart(long timestart) {
        this.timestart = timestart;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getCourseName() {
        return courseName;
    }
    
    public void setCourseName(String courseName) {
        this.courseName = courseName;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public int getCourseId() {
        return courseId;
    }
    
    public void setCourseId(int courseId) {
        this.courseId = courseId;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public int getAssignmentId() {
        return assignmentId;
    }
    
    public void setAssignmentId(int assignmentId) {
        this.assignmentId = assignmentId;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getSubmissionStatus() {
        return submissionStatus;
    }
    
    public void setSubmissionStatus(String submissionStatus) {
        this.submissionStatus = submissionStatus;
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
    
    /**
     * 獲取事件的截止日期
     */
    public LocalDate getDueDate() {
        return LocalDate.ofEpochDay(timestart / 86400);
    }
    
    /**
     * 獲取簡短的顯示文本
     */
    public String getDisplayText() {
        return String.format("[%s] %s", courseName, name);
    }
    
    /**
     * 根據繳交狀態獲取顏色
     * 綠色: 已繳交
     * 黃色: 未繳交
     * 紅色: 逾期
     * 灰色: 尚未開放或無需繳交
     */
    public String getStatusColor() {
        
        
        // 如果已繳交
        if ("submitted".equals(submissionStatus)) {
            return "#10b981"; // 綠色
        }
        
        // 如果逾期
        if (timestart * 1000 < System.currentTimeMillis()) {
            return "#10b981"; // 紅色
        }
        
        // 未繳交但未逾期
        return "#f59e0b"; // 黃色
    }
    
    /**
     * 獲取狀態描述
     */
    public String getStatusDescription() {
        
        
        if ("submitted".equals(submissionStatus)) {
            return "已繳交";
        }
        
        if (timestart * 1000 < System.currentTimeMillis()) {
            return "已繳交";
        }
        
        return "未繳交";
    }
    
    @Override
    public String toString() {
        return "MoodleModel{" +
                "id=" + id +
                ", userEmail='" + userEmail + '\'' +
                ", name='" + name + '\'' +
                ", courseName='" + courseName + '\'' +
                ", eventDate='" + eventDate + '\'' +
                ", submissionStatus='" + submissionStatus + '\'' +
                ", timestart=" + timestart +
                '}';
    }
} 