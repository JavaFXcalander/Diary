package com.taskmanager.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.table.DatabaseTable;

import java.time.LocalDate;

@DatabaseTable(tableName = "projects")
public class ProjectModel {
    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(canBeNull = false)
    private String name;

    @DatabaseField
    private String description;

    @DatabaseField(dataType = DataType.SERIALIZABLE)
    private LocalDate startDate;

    @DatabaseField(dataType = DataType.SERIALIZABLE)
    private LocalDate endDate;

    @DatabaseField
    private String status;

    public ProjectModel() {
        // ORMLite 需要一個無參構造器
    }

    public ProjectModel(String name) {
        this.name = name;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
