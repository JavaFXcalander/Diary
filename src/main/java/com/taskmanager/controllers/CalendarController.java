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
import javafx.scene.control.CheckBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.animation.TranslateTransition;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import java.io.IOException;
import com.taskmanager.services.GoogleCalendarService;
import com.taskmanager.services.CalendarEventSyncService;
import com.taskmanager.services.UserManager;
import com.taskmanager.services.MoodleService;
import com.taskmanager.services.OllamaService;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TextArea;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.application.Platform;
import javafx.concurrent.Task;
import com.taskmanager.database.DiaryDatabase;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;

public class CalendarController {

    @FXML
    private GridPane calendarGrid;
    
    @FXML
    private Label monthYearLabel;
    
    @FXML
    private Button previousButton;
    
    @FXML
    private Button nextButton;
    
    // Sidebar elements
    @FXML
    private Button hamburgerButton;
    
    @FXML
    private VBox sidebar;
    
    @FXML
    private BorderPane mainContent;
    
    @FXML
    private Button closeSidebarButton;
    
    @FXML
    private CheckBox googleCalendarCheckBox;
    
    @FXML
    private CheckBox moodleCheckBox;
    
    @FXML
    private Label syncStatusLabel;
    
    @FXML
    private TextArea chatTextArea;
    
    @FXML
    private ScrollPane chatScrollPane;
    
    @FXML
    private HBox chatInputBox;
    
    // Chat interface elements
    @FXML
    private TextArea messageInput;
    
    @FXML
    private VBox chatContainer;
    
    @FXML
    private Button sendMessageButton;
    
    @FXML
    private Button clearChatButton;
    
    @FXML
    private Label modelStatusLabel;
    
    private LocalDate currentDate;
    private GoogleCalendarService googleCalendarService;
    private CalendarEventSyncService syncService;
    private MoodleService moodleService;
    private OllamaService ollamaService;
    
    private boolean sidebarOpen = false;
    
    public void initialize() {
        // 初始化為當前日期
        currentDate = LocalDate.now();
        
        // 初始化 Google Calendar 服務和同步服務
        try {
            String userEmail = UserManager.getInstance().getCurrentUser().getEmail();
            googleCalendarService = new GoogleCalendarService(userEmail);
            syncService = CalendarEventSyncService.getInstance();
            syncService.initialize(userEmail);
            
            // 初始化 Moodle 服務
            moodleService = MoodleService.getInstance();
            
            // 初始化 Ollama 服務
            ollamaService = OllamaService.getInstance();
            
            // 嘗試自動恢復 Moodle 登錄狀態
            try {
                if (moodleService.autoRestoreLogin(userEmail)) {
                    System.out.println("CalendarController: 自動恢復Moodle登錄成功");
                }
            } catch (Exception e) {
                System.err.println("CalendarController: 自動恢復Moodle登錄失敗: " + e.getMessage());
            }
            
            // 如果已授權，啟動同步服務
            if (googleCalendarService.isUserAuthorized()) {
                syncService.startPeriodicSync();
                System.out.println("CalendarController: 已啟動 Google Calendar 同步服務");
            }
        } catch (Exception e) {
            System.err.println("無法初始化 Calendar 服務: " + e.getMessage());
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
        
        // 初始化聊天界面
        initializeChatInterface();
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
        
        // 重新載入這一週的anynotes數據（當用戶切換月份時）
        loadWeeklyAnynotesToOllama();
        
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

        // 檢查並顯示 Google Calendar 和 Moodle 事件
        addEventsToCell(cell, cellDate);
        
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
     * 在日曆格子中添加 Google Calendar 和 Moodle 事件指示器
     */
    private void addEventsToCell(VBox cell, LocalDate date) {
        List<GoogleCalendarService.CalendarEvent> googleEvents = null;
        List<MoodleService.MoodleEvent> moodleEvents = null;
        
        // 獲取 Google Calendar 事件
        if (syncService != null) {
            try {
                googleEvents = syncService.getEventsForDate(date);
            } catch (Exception e) {
                System.err.println("載入 Google Calendar 事件時發生錯誤 (" + date + "): " + e.getMessage());
            }
        }
        
        // 獲取 Moodle 事件
        if (moodleService != null) {
            try {
                String userEmail = UserManager.getInstance().getCurrentUser().getEmail();
                moodleEvents = moodleService.getCalendarEventsWithCache(date, userEmail);
                // 過濾出當天的事件
                if (moodleEvents != null) {
                    moodleEvents = moodleEvents.stream()
                            .filter(event -> {
                                LocalDate eventDate = LocalDate.ofEpochDay(event.getTimestart() / 86400);
                                return eventDate.equals(date);
                            })
                            .collect(java.util.stream.Collectors.toList());
                }
            } catch (Exception e) {
                System.err.println("載入 Moodle 事件時發生錯誤 (" + date + "): " + e.getMessage());
            }
        }
        
        // 計算總事件數
        int googleEventCount = (googleEvents != null) ? googleEvents.size() : 0;
        int moodleEventCount = (moodleEvents != null) ? moodleEvents.size() : 0;
        int totalEvents = googleEventCount + moodleEventCount;
        
        if (totalEvents > 0) {
            // 創建事件指示器容器
            VBox eventContainer = new VBox(1);
            eventContainer.setAlignment(Pos.BOTTOM_LEFT);
            eventContainer.setPrefWidth(Double.MAX_VALUE);
            
            // 最多顯示 3 個事件
            int maxEventsToShow = Math.min(totalEvents, 3);
            int eventsShown = 0;
            
            // 先顯示 Google Calendar 事件（綠色）
            if (googleEvents != null && eventsShown < maxEventsToShow) {
                for (GoogleCalendarService.CalendarEvent event : googleEvents) {
                    if (eventsShown >= maxEventsToShow) break;
                    
                    Label eventLabel = new Label(truncateText(event.getSummary(), 12));
                    eventLabel.setStyle("-fx-background-color: #637a60; -fx-text-fill: white; " +
                                      "-fx-font-size: 8px; -fx-padding: 1px 3px; " +
                                      "-fx-background-radius: 2px; -fx-max-width: 90px;");
                    eventContainer.getChildren().add(eventLabel);
                    eventsShown++;
                }
            }
            
            // 再顯示 Moodle 事件（根據繳交狀態顯示不同顏色）
            if (moodleEvents != null && eventsShown < maxEventsToShow) {
                for (MoodleService.MoodleEvent event : moodleEvents) {
                    if (eventsShown >= maxEventsToShow) break;
                    
                    Label eventLabel = new Label(truncateText(event.getName(), 12));
                    String backgroundColor = event.getStatusColor();
                    eventLabel.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: white; " +
                                      "-fx-font-size: 8px; -fx-padding: 1px 3px; " +
                                      "-fx-background-radius: 2px; -fx-max-width: 90px;", backgroundColor));
                    
                    // 添加工具提示顯示狀態
                    Tooltip tooltip = new Tooltip(String.format("%s - %s", event.getDisplayText(), event.getStatusDescription()));
                    Tooltip.install(eventLabel, tooltip);
                    
                    eventContainer.getChildren().add(eventLabel);
                    eventsShown++;
                }
            }
            
            // 如果有更多事件，顯示 "..."
            if (totalEvents > 3) {
                Label moreLabel = new Label("+" + (totalEvents - 3) + " more");
                moreLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 7px;");
                eventContainer.getChildren().add(moreLabel);
            }
            
            cell.getChildren().add(eventContainer);
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
    private void handleProjectButton(ActionEvent event) throws IOException {
        Parent diaryRoot = FXMLLoader.load(getClass().getResource("/fxml/project.fxml"));
        Scene scene = ((Node) event.getSource()).getScene();
        scene.setRoot(diaryRoot);
    }

    @FXML
    private void handleDiaryButton(ActionEvent event) throws IOException {
        Parent diaryRoot = FXMLLoader.load(getClass().getResource("/fxml/DiaryView.fxml"));
        Scene scene = ((Node) event.getSource()).getScene();
        scene.setRoot(diaryRoot);
    }

    public void setMonthAndYear(int month, int year) {
        this.currentDate = LocalDate.of(year, month, 1); // 確保 currentDate 被正確設置為完整的 LocalDate
        updateCalendar();
    }
    
    /**
     * 檢查 Moodle 是否已配置
     */
    public boolean isMoodleConfigured() {
        return moodleService != null && moodleService.getWstoken() != null;
    }

    /**
     * 切換側邊欄的顯示/隱藏
     */
    @FXML
    private void toggleSidebar() {
        if (sidebarOpen) {
            closeSidebar();
        } else {
            openSidebar();
        }
    }
    
    /**
     * 打開側邊欄
     */
    private void openSidebar() {
        sidebar.setVisible(true);
        
        // 滑動動畫
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), sidebar);
        slideIn.setFromX(250);
        slideIn.setToX(0);
        
        // 淡入動畫
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), sidebar);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        
        slideIn.play();
        fadeIn.play();
        
        sidebarOpen = true;
        
        // 更新同步狀態
        updateSyncStatus();
        
        // 更新模型狀態
        updateModelStatus();
    }
    
    /**
     * 關閉側邊欄
     */
    private void closeSidebar() {
        // 滑動動畫
        TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), sidebar);
        slideOut.setFromX(0);
        slideOut.setToX(250);
        
        // 淡出動畫
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), sidebar);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        
        slideOut.setOnFinished(e -> sidebar.setVisible(false));
        
        slideOut.play();
        fadeOut.play();
        
        sidebarOpen = false;
    }
    
    

    
    
    
    
    
    
    
    
    /**
     * 更新同步狀態顯示
     */
    private void updateSyncStatus() {
        try {
            String status = "Last sync: Never";
            if (syncService != null) {
                status = syncService.getSyncStatus();
            }
            
            if (syncStatusLabel != null) {
                syncStatusLabel.setText(status);
            }
        } catch (Exception e) {
            System.err.println("更新同步狀態時發生錯誤: " + e.getMessage());
        }
    }
    
    // ===== 聊天功能方法 =====
    
    /**
     * 發送消息給AI
     */
    @FXML
    private void sendMessage() {
        if (messageInput == null || messageInput.getText().trim().isEmpty()) {
            return;
        }
        
        String userMessage = messageInput.getText().trim();
        messageInput.clear();
        
        // 添加用戶消息到聊天界面
        addMessageToChat(userMessage, true);
        
        // 檢查Ollama服務狀態
        if (!ollamaService.isAvailable()) {
            addMessageToChat("❌ Ollama服務未運行，請確保Ollama已啟動並運行在localhost:11434", false);
            return;
        }
        
        if (!ollamaService.isModelAvailable()) {
            addMessageToChat("❌ llama3.2模型未安裝，請運行: ollama pull llama3.2", false);
            return;
        }
        
        // 創建動態"正在思考"標籤
        Label thinkingLabel = new Label(".");
        thinkingLabel.getStyleClass().add("ai-message");
        HBox thinkingContainer = new HBox(thinkingLabel);
        thinkingContainer.setAlignment(Pos.CENTER_LEFT);
        thinkingContainer.getStyleClass().add("message-container");
        chatContainer.getChildren().add(thinkingContainer);
        
        // 滾動到底部
        Platform.runLater(() -> {
            chatScrollPane.setVvalue(1.0);
        });
        
        // 創建動態點數效果的Timeline
        final int[] dots = {1}; // 使用陣列來允許在lambda中修改
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                Duration.millis(500),
                e -> {
                    dots[0] = (dots[0] % 3) + 1;
                    StringBuilder dotText = new StringBuilder();
                    for (int i = 0; i < dots[0]; i++) {
                        dotText.append(".");
                    }
                    thinkingLabel.setText(dotText.toString());
                }
            )
        );
        timeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        timeline.play();
        
        // 異步發送消息給AI
        Task<String> sendTask = ollamaService.sendMessageAsync(userMessage);
        
        sendTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                // 停止動畫並移除"正在思考"
                timeline.stop();
                chatContainer.getChildren().remove(thinkingContainer);
                
                // 添加AI回應
                String aiResponse = sendTask.getValue();
                addMessageToChat(aiResponse, false);
            });
        });
        
        sendTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                // 停止動畫並移除"正在思考"
                timeline.stop();
                chatContainer.getChildren().remove(thinkingContainer);
                
                // 顯示錯誤消息
                Throwable exception = sendTask.getException();
                String errorMessage = "❌ 發送消息失敗: " + 
                    (exception != null ? exception.getMessage() : "未知錯誤");
                addMessageToChat(errorMessage, false);
            });
        });
        
        // 在新線程中執行
        new Thread(sendTask).start();
    }
    
    /**
     * 清除聊天記錄
     */
    @FXML
    private void clearChat() {
        if (chatContainer != null) {
            chatContainer.getChildren().clear();
            ollamaService.clearHistory();
            
            // 重新加載週anynotes數據
            loadWeeklyAnynotesToOllama();
            
            // 添加歡迎消息
            addMessageToChat("有什麼想聊的嗎？", false);
        }
    }
    
    /**
     * 添加消息到聊天界面
     */
    private void addMessageToChat(String message, boolean isUser) {
        if (chatContainer == null) return;
        
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add(isUser ? "user-message" : "ai-message");
        
        HBox messageContainer = new HBox(messageLabel);
        messageContainer.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messageContainer.getStyleClass().add("message-container");
        
        Platform.runLater(() -> {
            chatContainer.getChildren().add(messageContainer);
            
            // 滾動到底部
            chatScrollPane.setVvalue(1.0);
        });
    }
    
    /**
     * 更新模型狀態
     */
    private void updateModelStatus() {
        if (modelStatusLabel != null && ollamaService != null) {
            Platform.runLater(() -> {
                String status = ollamaService.getModelStatus();
                modelStatusLabel.setText(status);
            });
        }
    }
    
    /**
     * 初始化聊天界面
     */
    private void initializeChatInterface() {
        // 清除舊的對話歷史
        if (ollamaService != null) {
            ollamaService.clearHistory();
            ollamaService.clearWeeklyAnynotes();
        }
        
        // 設置Enter鍵發送消息
        if (messageInput != null) {
            messageInput.setOnKeyPressed(event -> {
                if (event.getCode().toString().equals("ENTER") && !event.isShiftDown()) {
                    event.consume();
                    sendMessage();
                }
            });
        }
        
        // 加載這一週的anynotes數據
        loadWeeklyAnynotesToOllama();
        
        // 檢查並更新模型狀態
        Task<Void> statusTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateModelStatus();
                return null;
            }
        };
        new Thread(statusTask).start();
        
        // 添加歡迎消息
        Platform.runLater(() -> {
            addMessageToChat("有什麼想聊的嗎？", false);
        });
    }
    
    /**
     * 載入這一週的anynotes數據到Ollama服務
     */
    private void loadWeeklyAnynotesToOllama() {
        try {
            if (ollamaService != null) {
                // 先清除舊的週數據
                ollamaService.clearWeeklyAnynotes();
                
                String userEmail = UserManager.getInstance().getCurrentUser().getEmail();
                DiaryDatabase diaryDatabase = DiaryDatabase.getInstance();
                
                // 獲取這一週的anynotes數據
                List<String> weeklyAnynotes = diaryDatabase.getWeeklyAnynotes(userEmail, LocalDate.now());
                
                // 設置到Ollama服務
                ollamaService.setWeeklyAnynotes(weeklyAnynotes);
                
                System.out.println("已載入 " + weeklyAnynotes.size() + " 條本週anynotes記錄到AI助手");
                
                // 調試：打印加載的數據
                if (!weeklyAnynotes.isEmpty()) {
                    System.out.println("本週anynotes內容：");
                    for (String note : weeklyAnynotes) {
                        System.out.println("  - " + note);
                    }
                } else {
                    System.out.println("本週沒有anynotes記錄");
                }
            }
        } catch (Exception e) {
            System.err.println("載入週anynotes數據時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }
}