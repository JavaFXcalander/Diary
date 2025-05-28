package com.taskmanager.controllers;

import com.taskmanager.timeline.TimeAxisPane;
import com.taskmanager.timeline.TaskView;
import com.google.api.services.calendar.model.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ResourceBundle;
import java.util.List;

import com.taskmanager.services.CalendarEventSyncService;
import com.taskmanager.services.GoogleCalendarService;
import com.taskmanager.timeline.TimeUtil;
import com.taskmanager.database.DiaryDatabase;
import com.taskmanager.models.DiaryModel;
import com.taskmanager.services.UserSession;

public class ScheduleController implements Initializable {

    @FXML
    private TimeAxisPane timeAxisPane;
    
    @FXML
    private HBox buttonContainer;
    
    private CalendarEventSyncService syncService;
    private DiaryDatabase database = DiaryDatabase.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        syncService = CalendarEventSyncService.getInstance();
    }
    
    public TimeAxisPane getTimeAxisPane() {
        return timeAxisPane;
    }
    
    /**
     * 從已獲取的事件列表中載入非整日事件到時間軸（避免重複調用API）
     */
    public void loadGoogleCalendarEventsFromList(List<GoogleCalendarService.CalendarEvent> events) {
        if (timeAxisPane == null) {
            System.out.println("timeAxisPane 為 null");
            return;
        }

        try {
            // 清除時間軸上的舊事件
            timeAxisPane.clearEvents();
            System.out.println("已清除時間軸上的舊事件");
            
            System.out.println("從傳遞的事件列表處理 " + events.size() + " 個事件");
            
            // 只處理非整日事件
            int addedCount = 0;
            for (GoogleCalendarService.CalendarEvent event : events) {
                // 添加詳細的調試信息
                LocalTime startTime = Instant.ofEpochMilli(event.getStartTime())
                        .atZone(ZoneId.systemDefault())
                        .toLocalTime();
                LocalTime endTime = Instant.ofEpochMilli(event.getEndTime())
                        .atZone(ZoneId.systemDefault())
                        .toLocalTime();
                
                System.out.println("ScheduleController 檢查事件: " + event.getSummary() + 
                                 " | 開始: " + startTime + 
                                 " | 結束: " + endTime + 
                                 " | 整日: " + event.isAllDay());
                
                if (!event.isAllDay()) {
                    // 有時間的事件加到時間軸
                    addEventToTimeline(event);
                    addedCount++;
                } else {
                    System.out.println("ScheduleController 跳過整日事件: " + event.getSummary());
                }
            }
            
            System.out.println("已添加 " + addedCount + " 個非整日事件到時間軸");
            
        } catch (Exception e) {
            System.err.println("載入 Google Calendar 事件時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 載入指定日期的 Google Calendar 事件到時間軸（舊方法，保留向後兼容）
     */
    public void loadGoogleCalendarEvents(LocalDate date) {
        if (timeAxisPane == null || syncService == null) {
            System.out.println("timeAxisPane 或 syncService 為 null");
            return;
        }

        try {
            // 清除時間軸上的舊事件
            timeAxisPane.clearEvents();
            System.out.println("已清除時間軸上的舊事件");
            
            // 先檢查是否已經標記為空
            DiaryModel entry = database.getDiaryEntry(date, UserSession.getInstance().getCurrentUserEmail());
            if (entry != null && entry.isCalendarEmpty()) {
                System.out.println("該日期已標記為空，跳過API呼叫");
                return;
            }

            List<GoogleCalendarService.CalendarEvent> events = syncService.getEventsForDate(date);
            System.out.println("從同步服務獲取到 " + events.size() + " 個事件");
            
            // 只處理有時間的事件（非整日事件由DiaryController處理）
            int addedCount = 0;
            for (GoogleCalendarService.CalendarEvent event : events) {
                // 添加詳細的調試信息
                LocalTime startTime = Instant.ofEpochMilli(event.getStartTime())
                        .atZone(ZoneId.systemDefault())
                        .toLocalTime();
                LocalTime endTime = Instant.ofEpochMilli(event.getEndTime())
                        .atZone(ZoneId.systemDefault())
                        .toLocalTime();
                
                System.out.println("ScheduleController 檢查事件: " + event.getSummary() + 
                                 " | 開始: " + startTime + 
                                 " | 結束: " + endTime + 
                                 " | 整日: " + event.isAllDay());
                
                if (!event.isAllDay()) {
                    // 有時間的事件加到時間軸
                    addEventToTimeline(event);
                    addedCount++;
                } else {
                    System.out.println("ScheduleController 跳過整日事件: " + event.getSummary());
                }
            }
            
            System.out.println("已添加 " + addedCount + " 個非整日事件到時間軸");
            
        } catch (Exception e) {
            System.err.println("載入 Google Calendar 事件時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 將 Google Calendar 事件添加到時間軸
     */
    private void addEventToTimeline(GoogleCalendarService.CalendarEvent event) {
        if (timeAxisPane == null) {
            System.out.println("timeAxisPane 為 null，無法添加事件");
            return;
        }

        try {
            // 轉換時間戳為 LocalTime
            LocalTime startTime = Instant.ofEpochMilli(event.getStartTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalTime();
            
            System.out.println("處理事件: " + event.getSummary() + " 開始時間: " + startTime);
            
            // 檢查事件是否在時間軸顯示範圍內
            if (isTimeInTimelineRange(startTime)) {
                // 直接添加事件到時間軸進行繪製
                timeAxisPane.addCalendarEvent(event);
                
                System.out.println("✅ 已添加事件到時間軸: " + event.getSummary() + " at " + startTime);
            } else {
                System.out.println("❌ 事件時間超出時間軸範圍: " + event.getSummary() + " at " + startTime);
            }
        } catch (Exception e) {
            System.err.println("添加事件到時間軸時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 檢查時間是否在時間軸顯示範圍內
     */
    private boolean isTimeInTimelineRange(LocalTime time) {
        LocalTime startHour = TimeUtil.TIMELINE_VIEW_START_HOUR; // 5:00
        
        // 計算結束時間：5:00 + 19小時 = 24:00 (午夜)
        // 時間軸顯示範圍是 5:00 到 23:59:59
        LocalTime endHour = LocalTime.of(23, 59, 59);
        
        System.out.println("時間軸範圍: " + startHour + " 到 " + endHour + 
                          ", 檢查時間: " + time);
        
        // 檢查時間是否在範圍內
            boolean inRange = !time.isBefore(startHour) && !time.isAfter(endHour);
        System.out.println("範圍檢查: " + startHour + " <= " + time + " <= " + endHour + " = " + inRange);
            return inRange;
    }
}
