package com.taskmanager.models;

import java.time.LocalDateTime;

public class Task {
    private String title;
    private String description;
    private int priority;
    private LocalDateTime dueDate;
    private int xpReward;
    private int goldReward;
    private boolean completed;
    
    public Task(String title, String description, int priority, LocalDateTime dueDate) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.dueDate = dueDate;
        this.completed = false;
        calculateRewards();
    }
    
    private void calculateRewards() {
        // Base rewards
        this.xpReward = 10;
        this.goldReward = 5;
        
        // Adjust rewards based on priority
        switch (priority) {
            case 1: // High priority
                this.xpReward *= 2;
                this.goldReward *= 2;
                break;
            case 2: // Medium priority
                this.xpReward *= 1.5;
                this.goldReward *= 1.5;
                break;
            // Low priority uses base rewards
        }
    }
    
    // Getters and setters
    public String getTitle() {
        return title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public LocalDateTime getDueDate() {
        return dueDate;
    }
    
    public int getXpReward() {
        return xpReward;
    }
    
    public int getGoldReward() {
        return goldReward;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    @Override
    public String toString() {
        return title + " - " + description;
    }
} 