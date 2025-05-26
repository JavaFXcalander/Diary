package com.taskmanager.timeline;

import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import java.time.LocalTime;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import com.taskmanager.services.GoogleCalendarService;

// Placeholder for TaskView. Represents a task displayed on the timeline.
public class TaskView extends Region {
    private String taskName;
    private LocalTime startTime;
    private Duration duration;
    private Label nameLabel;
    private Label timeLabel;

    // Basic constructor
    public TaskView(String taskName, LocalTime startTime, Duration duration) {
        this.taskName = taskName;
        this.startTime = startTime;
        this.duration = duration;
        
        // Create a VBox to hold the labels
        VBox content = new VBox(2);
        content.setAlignment(Pos.CENTER_LEFT);
        
        // Create and style the name label
        nameLabel = new Label(taskName);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
        
        // Create and style the time label
        timeLabel = new Label(formatTimeRange(startTime, duration));
        timeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 0.8em;");
        
        content.getChildren().addAll(nameLabel, timeLabel);
        
        // Style the TaskView
        setStyle("-fx-background-color: #4285f4; -fx-border-color: #3367d6; -fx-border-width: 1; -fx-padding: 5px; -fx-background-radius: 3; -fx-border-radius: 3;");
        
        // Add the content to the TaskView
        getChildren().add(content);
    }

    private String formatTimeRange(LocalTime start, Duration duration) {
        LocalTime end = start.plus(duration);
        return String.format("%02d:%02d - %02d:%02d", 
            start.getHour(), start.getMinute(),
            end.getHour(), end.getMinute());
    }

    public void updateFromGoogleCalendarEvent(GoogleCalendarService.CalendarEvent event) {
        // Convert milliseconds to LocalTime
        LocalTime start = Instant.ofEpochMilli(event.getStartTime())
            .atZone(ZoneId.systemDefault())
            .toLocalTime();
        
        Duration duration = Duration.ofMillis(event.getEndTime() - event.getStartTime());
        
        this.startTime = start;
        this.duration = duration;
        this.taskName = event.getSummary();
        
        // Update the labels
        nameLabel.setText(taskName);
        timeLabel.setText(formatTimeRange(start, duration));
    }

    public String getTaskName() {
        return taskName;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public Duration getDuration() {
        return duration;
    }
}
