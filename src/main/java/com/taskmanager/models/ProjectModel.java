package com.taskmanager.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.j256.ormlite.field.DataType;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@DatabaseTable(tableName = "project_entries")
public class ProjectModel {
    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(foreign = true, columnName = "user_id")
    private UserModel user;


    @DatabaseField(canBeNull = false)
    private int year;

    @DatabaseField(canBeNull = false)
    private int month;


    @DatabaseField
    public String projectName1;

    @DatabaseField
    private String about1;

    @DatabaseField
    private String projectName2;

    @DatabaseField
    private String about2;

    @DatabaseField
    private String projectName3;

    @DatabaseField
    private String about3;


    @DatabaseField
    private String projectName4;

    @DatabaseField
    private String about4;

    @DatabaseField
    private String habit1;

    @DatabaseField
    private String habit2;

    @DatabaseField
    private String habit3;

    @DatabaseField
    private String habit4;

    @DatabaseField(dataType = DataType.LONG_STRING)
    private String dailyChecksJson; // 儲存每日勾選狀態的JSON




    public ProjectModel() {
        // ORMLite 需要一個無參構造器
    }

    public ProjectModel(int year, int month) {
        this.month = month;
        this.year = year;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public String getProject1() {
        return projectName1;
    }

    public void setProject1(String projectName1) {
        this.projectName1 = projectName1;
    }

    public String getProject2() {
        return projectName2;
    }

    public void setProject2(String projectName2) {
        this.projectName2 = projectName2;
    }

    public String getProject3() {
        return projectName3;
    }

    public void setProject3(String projectName3) {
        this.projectName3 = projectName3;
    }

    public String getProject4() {
        return projectName4;
    }

    public void setProject4(String projectName4) {
        this.projectName4 = projectName4;
    }

    public String getAbout1() {
        return about1;
    }

    public void setAbout1(String about1) {
        this.about1 = about1;
    }

    public String getAbout2() {
        return about2;
    }

    public void setAbout2(String about2) {
        this.about2 = about2;
    }

    public String getAbout3() {
        return about3;
    }
    public void setAbout3(String about3) {
        this.about3 = about3;
    }
    public String getAbout4() {
        return about4;
    }
    public void setAbout4(String about4) {
        this.about4 = about4;
    }

    public String getHabit1() {
        return habit1;
    }

    public void setHabit1(String habit1) {
        this.habit1 = habit1;
    }

    public String getHabit2() {
        return habit2;
    }

    public void setHabit2(String habit2) {
        this.habit2 = habit2;
    }

    public String getHabit3() {
        return habit3;
    }

    public void setHabit3(String habit3) {
        this.habit3 = habit3;
    }

    public String getHabit4() {
        return habit4;
    }

    public void setHabit4(String habit4) {
        this.habit4 = habit4;
    }

    public Map<Integer, boolean[]> getDailyChecks() {
        if (dailyChecksJson == null || dailyChecksJson.isEmpty()) {
            return new HashMap<>();
        }
        return new Gson().fromJson(dailyChecksJson, new TypeToken<Map<Integer, boolean[]>>(){}.getType());
    }

    // 设置每日勾選狀態
    public void setDailyChecks(Map<Integer, boolean[]> dailyChecks) {
        this.dailyChecksJson = new Gson().toJson(dailyChecks);
    }

    public UserModel getUser() {
        return user;
    }
    
    public void setUser(UserModel user) {
        this.user = user;
    }

}