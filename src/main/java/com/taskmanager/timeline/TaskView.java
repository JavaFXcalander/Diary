package com.taskmanager.timeline;

import javafx.scene.layout.Region;

// Placeholder for TaskView. Represents a task displayed on the timeline.
public class TaskView extends Region {
    // Basic constructor
    public TaskView(String taskName, java.time.LocalTime startTime, java.time.Duration duration) {
        // Style it or add content as needed
        setStyle("-fx-background-color: lightblue; -fx-border-color: blue; -fx-padding: 5px;");
        // You would typically add a Label or other controls here to display taskName
        // For simplicity, keeping it as a styled region.
        // Actual size and position will be set by the controller using TimeUtil.toY
    }

    // Add methods to get task properties like start time, duration, text if needed by controller
}
