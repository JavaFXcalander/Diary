package com.taskmanager.timeline;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javafx.scene.text.Font;

public class TimeAxisPane extends Pane {

    private final Canvas grid = new Canvas();
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("HH"); // 或者 "HH时"
    public TimeAxisPane() {
        getChildren().add(grid);
        // Ensure redraw when size changes
        widthProperty().addListener((obs, oldW, newW) -> draw());
        heightProperty().addListener((obs, oldH, newH) -> draw());
    }

    public void addTask(TaskView task) {
        getChildren().add(task);
        // TODO: Position the task view based on its time and duration using TimeUtil.toY
        // This might involve task.setLayoutY() and task.setPrefHeight().
        // This logic will likely be managed by ScheduleController when a task is added.
    }

    private void draw() {
        double w = getWidth();
        double h = getHeight();

        if (w <= 0 || h <= 0) { // Avoid drawing if no space or invalid dimensions
            grid.setWidth(0);
            grid.setHeight(0);
            return;
        }

        grid.setWidth(w);
        grid.setHeight(h);
        GraphicsContext gc = grid.getGraphicsContext2D();

        gc.clearRect(0, 0, w, h);
        gc.setLineWidth(0.2);

        LocalTime currentTime = TimeUtil.TIMELINE_VIEW_START_HOUR;
        // Loop for the number of half-hour intervals in the defined timeline duration
        for (int i = 0; i < TimeUtil.TIMELINE_DURATION_HOURS; i++) { 
            double y = TimeUtil.toY(currentTime, h);

            if (currentTime.getMinute() == 0) {
                // Hour mark
                Font timeFont = Font.font("Times New Roman", 12); // 您可以替换 "Arial" 和 10 为您想要的字体和大小
                gc.setFont(timeFont);
                gc.setStroke(Color.GRAY); 
                gc.strokeLine(30, y+12, w, y+12);
                gc.setFill(Color.GRAY); 
                gc.fillText(currentTime.format(HOUR_FORMATTER), 5, y + 15); // Adjust text position as needed
            } 
            currentTime = currentTime.plusMinutes(60);
        }
    }
}
