package com.taskmanager.timeline;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import java.time.LocalTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import javafx.scene.text.Font;
import java.util.List;
import java.util.ArrayList;
import com.taskmanager.services.GoogleCalendarService;

public class TimeAxisPane extends Pane {

    private final Canvas grid = new Canvas();
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("HH");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    // 儲存要繪製的事件
    private List<GoogleCalendarService.CalendarEvent> events = new ArrayList<>();
    
    public TimeAxisPane() {
        getChildren().add(grid);
        // Ensure redraw when size changes
        widthProperty().addListener((obs, oldW, newW) -> {
            draw();
        });
        heightProperty().addListener((obs, oldH, newH) -> {
            draw();
        });
    }

    public void addTask(TaskView task) {
        getChildren().add(task);
        
        // 設置任務的位置和大小
        positionTask(task);
    }
    
    /**
     * 添加Google Calendar事件到時間軸
     */
    public void addCalendarEvent(GoogleCalendarService.CalendarEvent event) {
        events.add(event);
        draw(); // 重新繪製
        System.out.println("✅ 已添加事件到時間軸繪製列表: " + event.getSummary());
    }
    
    /**
     * 清除所有事件
     */
    public void clearEvents() {
        events.clear();
        draw(); // 重新繪製
        System.out.println("已清除時間軸上的所有事件");
    }
    
    private void positionTask(TaskView task) {
        double paneHeight = getHeight();
        double paneWidth = getWidth();
        
        if (paneHeight <= 0 || paneWidth <= 0) {
            // 如果面板還沒有大小，延遲設置位置
            heightProperty().addListener((obs, oldH, newH) -> {
                if (newH.doubleValue() > 0) {
                    positionTask(task);
                }
            });
            widthProperty().addListener((obs, oldW, newW) -> {
                if (newW.doubleValue() > 0) {
                    positionTask(task);
                }
            });
            return;
        }
        
        // 計算任務的Y位置
        double startY = TimeUtil.toY(task.getStartTime(), paneHeight) + 8;
        
        // 計算任務的高度（基於持續時間）
        long durationMinutes = task.getDuration().toMinutes();
        double heightPerMinute = paneHeight / (TimeUtil.TIMELINE_DURATION_HOURS * 60.0);
        double taskHeight = Math.max(20, durationMinutes * heightPerMinute); // 最小高度20px
        
        // 設置任務位置和大小
        task.setLayoutX(35); // 留出時間標籤的空間
        task.setLayoutY(startY);
        task.setPrefWidth(paneWidth - 40); // 留出左右邊距
        task.setPrefHeight(taskHeight);
        
        System.out.println("設置任務位置: " + task.getTaskName() + 
                          " Y=" + startY + " Height=" + taskHeight);
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

        // 繪製時間軸線條
        LocalTime currentTime = TimeUtil.TIMELINE_VIEW_START_HOUR;
        for (int i = 0; i < TimeUtil.TIMELINE_DURATION_HOURS; i++) { 
            double y = TimeUtil.toY(currentTime, h);

            if (currentTime.getMinute() == 0) {
                // Hour mark
                Font timeFont = Font.font("Times New Roman", 12);
                gc.setFont(timeFont);
                gc.setStroke(Color.GRAY); 
                gc.strokeLine(30, y+12, w, y+12);
                gc.setFill(Color.GRAY); 
                gc.fillText(currentTime.format(HOUR_FORMATTER), 5, y + 15);
            } 
            currentTime = currentTime.plusMinutes(60);
        }
        
        // 繪製Google Calendar事件
        drawCalendarEvents(gc, w, h);
    }
    
    /**
     * 繪製Google Calendar事件
     */
    private void drawCalendarEvents(GraphicsContext gc, double width, double height) {
        for (GoogleCalendarService.CalendarEvent event : events) {            
            try {
                // 轉換時間戳為 LocalTime
                LocalTime startTime = java.time.Instant.ofEpochMilli(event.getStartTime())
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalTime();
                
                LocalTime endTime = java.time.Instant.ofEpochMilli(event.getEndTime())
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalTime();
                
                // 計算位置和大小
                double startY = TimeUtil.toY(startTime, height);
                double endY = TimeUtil.toY(endTime, height);
                double eventHeight = Math.max(endY - startY, 20); // 最小高度20px
                
                // 繪製事件背景
                gc.setFill(Color.web("#637a60", 0.8)); // Google藍色，半透明
                gc.fillRoundRect(35, startY, width - 45, eventHeight, 5, 5);
                
                // 繪製事件邊框
                gc.setLineWidth(1);
                gc.strokeRoundRect(35, startY, width - 45, eventHeight, 5, 5);
                
                // 繪製事件文字
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Times New Roman", 12));
                
                // 事件標題
                String title = truncateText(event.getSummary(), 15);
                gc.fillText(title, 40, startY + 15);
                
                // 時間範圍（如果事件高度足夠）
                if (eventHeight > 30) {
                    String timeRange = startTime.format(TIME_FORMATTER) + "-" + endTime.format(TIME_FORMATTER);
                    gc.setFont(Font.font("Arial", 8));
                    gc.fillText(timeRange, 40, startY + 28);
                }
                
                System.out.println("已繪製事件: " + event.getSummary() + 
                                 " at Y=" + startY + " height=" + eventHeight);
                
            } catch (Exception e) {
                System.err.println("繪製事件時發生錯誤: " + e.getMessage());
            }
        }
    }
    
    /**
     * 截斷文字
     */
    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    public void clearTasks() {
        // 移除所有TaskView，但保留Canvas
        getChildren().removeIf(node -> node instanceof TaskView);
    }

   
}
