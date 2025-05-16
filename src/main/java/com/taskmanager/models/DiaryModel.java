package com.taskmanager.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.table.DatabaseTable;

import java.time.LocalDate;

@DatabaseTable(tableName = "diary_entries")
public class DiaryModel {
    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(canBeNull = false, dataType = DataType.SERIALIZABLE)
    private LocalDate date;

    @DatabaseField
    private String dDay;

    @DatabaseField
    private String priority;

    @DatabaseField
    private String routine;

    @DatabaseField
    private String budget;

    @DatabaseField
    private String todo;

    @DatabaseField
    private String photoCollage;

    @DatabaseField
    private String breakfast;

    @DatabaseField
    private String lunch;

    @DatabaseField
    private String dinner;

    @DatabaseField
    private String snack;

    @DatabaseField
    private String anynotes;

    public DiaryModel() {
        // ORMLite 需要一個無參構造器
    }

    public DiaryModel(LocalDate date) {
        this.date = date;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDDay() {
        return dDay;
    }

    public void setDDay(String dDay) {
        this.dDay = dDay;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getRoutine() {
        return routine;
    }

    public void setRoutine(String routine) {
        this.routine = routine;
    }

    // Getter and setter for todo field
    public String getTodo() {
        return todo;
    }
    
    public void setTodo(String todo) {
        this.todo = todo;
    }
    
    public String getBudget() {
        return budget;
    }

    public void setBudget(String budget) {
        this.budget = budget;
    }

    public String getPhotoCollage() {
        return photoCollage;
    }

    public void setPhotoCollage(String photoCollage) {
        this.photoCollage = photoCollage;
    }

    public String getBreakfast() {
        return breakfast;
    }

    public void setBreakfast(String breakfast) {
        this.breakfast = breakfast;
    }

    public String getLunch() {
        return lunch;
    }

    public void setLunch(String lunch) {
        this.lunch = lunch;
    }

    public String getDinner() {
        return dinner;
    }

    public void setDinner(String dinner) {
        this.dinner = dinner;
    }

    public String getSnack() {
        return snack;
    }

    public void setSnack(String snack) {
        this.snack = snack;
    }

    public String getAnynotes() {
        return anynotes;
    }

    public void setAnynotes(String anynotes) {
        this.anynotes = anynotes;
    }
}
