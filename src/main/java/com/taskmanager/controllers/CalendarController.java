package com.taskmanager.controllers;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import java.io.IOException;
import com.taskmanager.services.GoogleCalendarService;
import com.taskmanager.services.CalendarEventSyncService;
import com.taskmanager.services.UserManager;
import java.security.GeneralSecurityException;
import java.util.List;

public class CalendarController {

    @FXML
    private GridPane calendarGrid;
    
    @FXML
    private Label monthYearLabel;
    
    @FXML
    private Button previousButton;
    
    @FXML
    private Button nextButton;
    
    private LocalDate currentDate;
    private GoogleCalendarService googleCalendarService;
    private CalendarEventSyncService syncService;
    
    public void initialize() {
        // 初始化為當前日期
        currentDate = LocalDate.now();
        
        // 初始化 Google Calendar 服務和同步服務
        try {
            String userEmail = UserManager.getInstance().getCurrentUser().getEmail();
            googleCalendarService = new GoogleCalendarService(userEmail);
            syncService = CalendarEventSyncService.getInstance();
            syncService.initialize(userEmail);
            
            // 如果已授權，啟動同步服務
            if (googleCalendarService.isUserAuthorized()) {
                syncService.startPeriodicSync();
                System.out.println("CalendarController: 已啟動 Google Calendar 同步服務");
            }
        } catch (Exception e) {
            System.err.println("無法初始化 Google Calendar 服務: " + e.getMessage());
        }
        
        // 設置按鈕動態效果
        setupButtonEffects();
        
        // 設置月份切換按鈕
        if (previousButton != null) {
            previousButton.setOnAction(e -> {
                currentDate = currentDate.minusMonths(1);
                updateCalendar();
            });
        }
        
        if (nextButton != null) {
            nextButton.setOnAction(e -> {
                currentDate = currentDate.plusMonths(1);
                updateCalendar();
            });
        }
        
        // 初始顯示日曆
        updateCalendar();
    }
    
    private void setupButtonEffects() {
        // 添加滑鼠懸停和點擊效果
        if (previousButton != null) {
            String baseStyle = previousButton.getStyle();
            String hoverStyle = baseStyle + "-fx-background-color: #4a7da8;";
            String pressedStyle = baseStyle + "-fx-background-color: #3a6d98; -fx-translate-y: 1px;";
            
            previousButton.setOnMouseEntered(e -> previousButton.setStyle(hoverStyle));
            previousButton.setOnMouseExited(e -> previousButton.setStyle(baseStyle));
            previousButton.setOnMousePressed(e -> previousButton.setStyle(pressedStyle));
            previousButton.setOnMouseReleased(e -> {
                if (previousButton.isHover()) {
                    previousButton.setStyle(hoverStyle);
                } else {
                    previousButton.setStyle(baseStyle);
                }
            });
        }
        
        if (nextButton != null) {
            String baseStyle = nextButton.getStyle();
            String hoverStyle = baseStyle + "-fx-background-color: #4a7da8;";
            String pressedStyle = baseStyle + "-fx-background-color: #3a6d98; -fx-translate-y: 1px;";
            
            nextButton.setOnMouseEntered(e -> nextButton.setStyle(hoverStyle));
            nextButton.setOnMouseExited(e -> nextButton.setStyle(baseStyle));
            nextButton.setOnMousePressed(e -> nextButton.setStyle(pressedStyle));
            nextButton.setOnMouseReleased(e -> {
                if (nextButton.isHover()) {
                    nextButton.setStyle(hoverStyle);
                } else {
                    nextButton.setStyle(baseStyle);
                }
            });
        }
    }
    
    private void updateCalendar() {
        // 清除先前的日曆內容
        calendarGrid.getChildren().clear();
        
        // 設置月份年份標題 - 使用英文格式
        if (monthYearLabel != null) {
            String month = currentDate.getMonth().getDisplayName(TextStyle.FULL, Locale.US);
            int year = currentDate.getYear();
            monthYearLabel.setText(month + " " + year);
        }
        
        // 添加星期標題 (0行) - 使用英文格式
        for (int i = 0; i < 7; i++) {
            DayOfWeek day = DayOfWeek.of((i + 1) % 7 + 1); // 從星期日開始
            Label dayLabel = new Label(day.getDisplayName(TextStyle.SHORT, Locale.US));
            dayLabel.setAlignment(Pos.CENTER);
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            dayLabel.getStyleClass().add("day-header");
            calendarGrid.add(dayLabel, i, 0);
        }
        
        // 獲取當月的信息
        YearMonth yearMonth = YearMonth.from(currentDate);
        int daysInMonth = yearMonth.lengthOfMonth();
        
        // 獲取當月第一天是星期幾 (0=星期日, 1=星期一, ..., 6=星期六)
        LocalDate firstDayOfMonth = currentDate.withDayOfMonth(1);
        int dayOfWeekValue = firstDayOfMonth.getDayOfWeek().getValue() % 7; // 調整為星期日為0
        
        // 填充日曆
        int day = 1;
        int row = 1; // 從第1行開始（第0行是星期標題）
        
        // 填充第一週前的空白
        for (int col = 0; col < dayOfWeekValue; col++) {
            VBox cell = createCell("");
            calendarGrid.add(cell, col, row);
        }
        
        // 填充日期
        for (int col = dayOfWeekValue; col < 7; col++) {
            if (day <= daysInMonth) {
                VBox cell = createDateCell(day, currentDate);
                calendarGrid.add(cell, col, row);
                day++;
            }
        }
        
        // 填充剩下的週
        while (day <= daysInMonth) {
            row++;
            for (int col = 0; col < 7 && day <= daysInMonth; col++) {
                VBox cell = createDateCell(day, currentDate);
                calendarGrid.add(cell, col, row);
                day++;
            }
        }
    }
    
    private VBox createCell(String text) {
        VBox cell = new VBox();
        cell.setPrefSize(100, 80);
        cell.getStyleClass().add("calendar-cell");
        cell.setAlignment(Pos.TOP_LEFT);
        
        if (!text.isEmpty()) {
            Label label = new Label(text);
            cell.getChildren().add(label);
        }
        
        return cell;
    }
    
    private VBox createDateCell(int day, LocalDate baseDate) {
        String text = String.valueOf(day);
        VBox cell = createCell(text);
        
        // 調整日期位置
        cell.getChildren().clear(); // 清除預設標籤
        
        Label dateLabel = new Label(text);
        dateLabel.getStyleClass().add("date-label");
        
        VBox dateContainer = new VBox(dateLabel);
        dateContainer.setAlignment(Pos.TOP_RIGHT);
        dateContainer.setPrefWidth(Double.MAX_VALUE);
        
        cell.getChildren().add(dateContainer);
        
        // 高亮顯示當天
        LocalDate cellDate = baseDate.withDayOfMonth(day);
        LocalDate today = LocalDate.now();
        
        if (cellDate.equals(today)) {
            cell.getStyleClass().add("today-cell");
            dateLabel.getStyleClass().add("today-label");
        }

        // 檢查並顯示 Google Calendar 事件
        addGoogleCalendarEventsToCell(cell, cellDate);
        
        // 添加滑鼠懸停效果
        String baseStyle = cell.getStyle();
        String hoverStyle = baseStyle.isEmpty() ? 
            "-fx-border-color: lightgray; -fx-background-color: rgba(200, 220, 240, 0.3);" : 
            baseStyle + "-fx-background-color: rgba(200, 220, 240, 0.5);";
        
        cell.setOnMouseEntered(e -> cell.setStyle(hoverStyle));
        cell.setOnMouseExited(e -> cell.setStyle(baseStyle));
        
        // 添加點擊事件（可擴展為添加事件等功能）
        cell.setOnMouseClicked(e -> handleDateClick(cellDate));
        
        return cell;
    }

    /**
     * 在日曆格子中添加 Google Calendar 事件指示器
     */
    private void addGoogleCalendarEventsToCell(VBox cell, LocalDate date) {
        if (syncService == null) return;

        try {
            List<GoogleCalendarService.CalendarEvent> events = syncService.getEventsForDate(date);
            
            if (!events.isEmpty()) {
                // 創建事件指示器容器
                VBox eventContainer = new VBox(1);
                eventContainer.setAlignment(Pos.BOTTOM_LEFT);
                eventContainer.setPrefWidth(Double.MAX_VALUE);
                
                // 最多顯示 3 個事件，如果更多則顯示 "..."
                int maxEventsToShow = Math.min(events.size(), 3);
                
                for (int i = 0; i < maxEventsToShow; i++) {
                    GoogleCalendarService.CalendarEvent event = events.get(i);
                    Label eventLabel = new Label(truncateText(event.getSummary(), 12));
                    eventLabel.setStyle("-fx-background-color: #637a60; -fx-text-fill: white; " +
                                      "-fx-font-size: 8px; -fx-padding: 1px 3px; " +
                                      "-fx-background-radius: 2px; -fx-max-width: 90px;");
                    eventContainer.getChildren().add(eventLabel);
                }
                
                // 如果有更多事件，顯示 "..."
                if (events.size() > 3) {
                    Label moreLabel = new Label("+" + (events.size() - 3) + " more");
                    moreLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 7px;");
                    eventContainer.getChildren().add(moreLabel);
                }
                
                cell.getChildren().add(eventContainer);
            }
        } catch (Exception e) {
            // 靜默處理錯誤，不影響日曆顯示
            System.err.println("載入日曆事件時發生錯誤 (" + date + "): " + e.getMessage());
        }
    }


    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    private void handleDateClick(LocalDate date) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DiaryView.fxml"));
            Parent diaryRoot = loader.load();
            
            // 獲取 DiaryController 並設置選中的日期
            DiaryController diaryController = loader.getController();
            if (diaryController != null) {
                // 假設 DiaryController 有一個設置日期的方法
                diaryController.setSelectedDate(date);
            }
            
            // 切換場景
            Scene scene = calendarGrid.getScene();
            scene.setRoot(diaryRoot);
        } catch (IOException e) {
            System.err.println("無法載入日記視圖: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDiaryButton(ActionEvent event) throws IOException {
        Parent diaryRoot = FXMLLoader.load(getClass().getResource("/fxml/DiaryView.fxml"));
        Scene scene = ((Node) event.getSource()).getScene();
        scene.setRoot(diaryRoot);
    }

    @FXML
    private void handleProjectButton(ActionEvent event) throws IOException {
        Parent diaryRoot = FXMLLoader.load(getClass().getResource("/fxml/project.fxml"));
        Scene scene = ((Node) event.getSource()).getScene();
        scene.setRoot(diaryRoot);
    }

    public void setMonthAndYear(int month, int year) {
        this.currentDate = LocalDate.of(year, month, 1); // 確保 currentDate 被正確設置為完整的 LocalDate
        updateCalendar();
    }

}