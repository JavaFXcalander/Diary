package com.taskmanager.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import com.taskmanager.database.DiaryDatabase;
import com.taskmanager.models.DiaryModel;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.geometry.Side;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import com.taskmanager.services.MoodleService;

import com.taskmanager.services.UserSession;
import com.taskmanager.models.UserModel;
import com.taskmanager.services.GoogleCalendarService;
import com.taskmanager.services.MoodleService;

import javafx.application.HostServices;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import java.security.GeneralSecurityException;
import com.taskmanager.services.UserManager;
import com.taskmanager.MainApp;
import java.util.List;
import com.taskmanager.services.CalendarEventSyncService;
import com.taskmanager.controllers.MoodleConfigDialog;
import javafx.application.Platform;
import com.taskmanager.services.UserManager;
import java.util.ArrayList;
import com.taskmanager.models.MoodleModel;
import com.taskmanager.database.CalendarEventDatabase;

public class DiaryController implements TodoChangeListener {

    @FXML private Label dateLabel;
    @FXML private TextField dDayField, priorityField, routineField;
    @FXML private TextField breakfastField, lunchField, dinnerField, snackField;
    @FXML private TextArea anynotesArea;
    @FXML private VBox todoContainer;
    @FXML private TodoController todoContainerController; // This will be automatically injected by JavaFX
    @FXML private Button addButton;
    @FXML private ScheduleController scheduleController; // 注入schedule控制器
    @FXML private HabitController habitViewController; // 注入habit控制器
    
    private LocalDate currentDate = LocalDate.now();
    private DiaryDatabase database = DiaryDatabase.getInstance();
    private ContextMenu addMenu;
    private GoogleCalendarService googleCalendarService;
    private MoodleService moodleService;

    private CalendarEventSyncService syncService;
    private HostServices hostServices;
    private MenuItem addGoogleAPI;
    private MenuItem addMoodleAPI;


    @FXML
    public void initialize() {
        // 設定今天日期格式
        currentDate = LocalDate.now();
        updateDateDisplay();
        
        // 設置 TodoController 的變更監聽器
        if (todoContainerController != null) {
            todoContainerController.setChangeListener(this);
        }
        
        // 檢查 HabitController 是否正確注入
        System.err.println("HabitController 注入狀態: " + (habitViewController != null ? "成功" : "失敗"));
        if (habitViewController != null) {
            System.err.println("手動調用 HabitController.loadHabitData()");
            habitViewController.loadHabitData(currentDate);
        }
        
        // 載入當天的日記內容
        loadDiaryContent(currentDate);
        
        // 設定所有輸入欄位的失焦事件處理
        setupBlurEventHandlers();

        // 初始化 Google Calendar 服務和同步服務
        String userEmail = UserManager.getInstance().getCurrentUser().getEmail();
        googleCalendarService = new GoogleCalendarService(userEmail);
        syncService = CalendarEventSyncService.getInstance();
        syncService.initialize(userEmail);
        moodleService = MoodleService.getInstance();
        
        // 嘗試自動恢復 Moodle 登錄狀態
        try {
            if (moodleService.autoRestoreLogin(userEmail)) {
                System.out.println("自動恢復Moodle登錄成功");
                
                // 檢查是否需要同步（24小時內未同步過）
                CalendarEventDatabase eventDb = CalendarEventDatabase.getInstance();
                long lastUpdate = eventDb.getMoodleLastUpdateTime(userEmail);
                long timeSinceLastUpdate = System.currentTimeMillis() - lastUpdate;
                long CACHE_DURATION = 24 * 60 * 60 * 1000; // 24小時
                
                if (timeSinceLastUpdate > CACHE_DURATION) {
                    System.out.println("開始背景同步 Moodle 事件 (上次更新: " + (timeSinceLastUpdate / (60 * 60 * 1000)) + " 小時前)");
                    
                    // 在背景同步當月的 Moodle 事件
                    LocalDate now = LocalDate.now();
                    LocalDate monthStart = now.withDayOfMonth(1);
                    LocalDate monthEnd = now.withDayOfMonth(now.lengthOfMonth());
                    
                    // 使用新線程避免阻塞UI
                    new Thread(() -> {
                        try {
                            moodleService.forceSyncMoodleEvents(userEmail, monthStart, monthEnd);
                            System.out.println("Moodle事件背景同步完成");
                            
                            // 在UI線程中重新載入當前日期的事件
                            Platform.runLater(() -> {
                                loadMoodleEvents(currentDate);
                                updateMoodleStatus();
                            });
                        } catch (Exception e) {
                            System.err.println("Moodle事件背景同步失敗: " + e.getMessage());
                        }
                    }).start();
                } else {
                    System.out.println("Moodle 事件緩存仍然有效，跳過同步");
                    // 直接載入當前日期的事件
                    Platform.runLater(() -> loadMoodleEvents(currentDate));
                }
            }
        } catch (Exception e) {
            System.err.println("自動恢復Moodle登錄失敗: " + e.getMessage());
        }
        
        setHostServices(MainApp.getHostServicesInstance());
        
        // 建立 ContextMenu
        addMenu = new ContextMenu();
        addGoogleAPI = new MenuItem("新增Google行事曆");
        addMoodleAPI = new MenuItem("新增Moodle行事曆");
        addGoogleAPI.setOnAction(ev -> {
            try {
                if (googleCalendarService.isUserAuthorized()) {
                    Alert alert = new Alert(AlertType.INFORMATION);
                    alert.setTitle("Google行事曆");
                    alert.setHeaderText(null);
                    alert.setContentText("您已經連結Google行事曆");
                    alert.showAndWait();
                } else {
                    // Perform the full authorization flow
                    googleCalendarService.authorizeUser();
                    // Update status after authorization attempt
                    updateGoogleCalendarStatus();
                    // Optionally, confirm to the user if successful
                    if (googleCalendarService.isUserAuthorized()) {
                        Alert alert = new Alert(AlertType.INFORMATION);
                        alert.setTitle("Google行事曆");
                        alert.setHeaderText(null);
                        alert.setContentText("已成功連結Google行事曆！");
                        alert.showAndWait();
                    } else {
                        Alert alert = new Alert(AlertType.WARNING);
                        alert.setTitle("Google行事曆");
                        alert.setHeaderText(null);
                        alert.setContentText("未完成Google行事曆連結。");
                        alert.showAndWait();
                    }
                }
            } catch (IOException | GeneralSecurityException e) {
                e.printStackTrace();
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("錯誤");
                alert.setHeaderText(null);
                alert.setContentText("無法連接到Google行事曆: " + e.getMessage());
                alert.showAndWait();
            }
        });
        
        // 設置 Moodle API 按鈕事件處理
        addMoodleAPI.setOnAction(ev -> {
            showMoodleConfigDialog();
        });
        
        addMenu.getItems().addAll(addGoogleAPI, addMoodleAPI);

        updateGoogleCalendarStatus();
        updateMoodleStatus();

        addButton.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                if (addMenu.isShowing()) {
                    addMenu.hide();
                } else {
                    addMenu.show(addButton, Side.BOTTOM, -addButton.getWidth()-40, 0);
                }
            }
        });
        
        // 延遲一點載入Moodle事件，讓自動恢復登錄有時間完成
        Platform.runLater(() -> {
            if (moodleService != null && moodleService.getWstoken() != null) {
                loadMoodleEvents(currentDate);
            }
        });
    }

    

    private void setupBlurEventHandlers() {
        // 為每個輸入欄位添加失焦事件處理器
        dDayField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // 當失去焦點時
                saveDiaryEntry();
            }
        });
        
        priorityField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                saveDiaryEntry();
            }
        });
        
        routineField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                saveDiaryEntry();
            }
        });
        
        breakfastField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                saveDiaryEntry();
            }
        });
        
        lunchField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                saveDiaryEntry();
            }
        });
        
        dinnerField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                saveDiaryEntry();
            }
        });
        
        snackField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                saveDiaryEntry();
            }
        });
        
        anynotesArea.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                saveDiaryEntry();
            }
        });
    }
    
    private void saveDiaryEntry() {
        // 創建或更新日記條目
        DiaryModel entry = new DiaryModel(currentDate);
        entry.setDDay(dDayField.getText());
        entry.setPriority(priorityField.getText());
        entry.setRoutine(routineField.getText());
        entry.setBreakfast(breakfastField.getText());
        entry.setLunch(lunchField.getText());
        entry.setDinner(dinnerField.getText());
        entry.setSnack(snackField.getText());
        entry.setAnynotes(anynotesArea.getText());
        
        // 保存待辦事項
        if (todoContainerController != null) {
            String serializedTodos = todoContainerController.serializeTodoList();
            entry.setTodo(serializedTodos);
        }
        
        // 設置當前用戶
        UserSession userSession = UserSession.getInstance();
        if (userSession.isLoggedIn()) {
            String email = userSession.getCurrentUserEmail();
            UserModel user = database.getUserEntry(email);
            if (user != null) {
                entry.setUser(user);
            }
        }
        
        // 保存到數據庫
        database.saveDiaryEntry(entry);
    }

    @FXML
    private void showPreviousDate() {
        // 先保存當前日期的數據
        saveDiaryEntry();
        
        // 切換到前一天
        currentDate = currentDate.minusDays(1);
        updateDateDisplay();
        loadDiaryContent(currentDate);
    }

    @FXML
    private void showNextDate() {
        // 先保存當前日期的數據
        saveDiaryEntry();
        
        // 切換到後一天
        currentDate = currentDate.plusDays(1);
        updateDateDisplay();
        loadDiaryContent(currentDate);
    }
    
    private void updateDateDisplay() {
        // 更新日期顯示
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy / MM / dd - EEEE", Locale.ENGLISH);
        String formattedDate = currentDate.format(formatter);
        dateLabel.setText(formattedDate);
        dateLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: normal;");
        dateLabel.setPadding(Insets.EMPTY);
    }

    @FXML
    private void handleMonthButton(ActionEvent event) throws IOException {
        Parent diaryRoot = FXMLLoader.load(getClass().getResource("/fxml/calendar.fxml"));
        Scene scene = ((Node) event.getSource()).getScene();
        scene.setRoot(diaryRoot);
    }

    @FXML
    private void handleProjectButton(ActionEvent event) throws IOException {
        Parent diaryRoot = FXMLLoader.load(getClass().getResource("/fxml/project.fxml"));
        Scene scene = ((Node) event.getSource()).getScene();
        scene.setRoot(diaryRoot);
    }

    public void setSelectedDate(LocalDate date) {
        // 先保存當前日期的數據
        saveDiaryEntry();
        
        // 格式化並顯示選擇的日期
        currentDate = date;
        updateDateDisplay();
        
        // 加載該日期的日記內容
        loadDiaryContent(currentDate);
    }
    
    @Override
    public void onTodoChanged() {
        // 當Todo數據變更時自動保存
        saveDiaryEntry();
    }
    
    private void loadDiaryContent(LocalDate date) {
        try {
            // 從數據庫中加載選定日期的日記內容
            DiaryModel entry;
            UserSession userSession = UserSession.getInstance();
            
            if (userSession.isLoggedIn()) {
                // 如果用戶已登錄，加載該用戶的日記
                entry = database.getDiaryEntry(date, userSession.getCurrentUserEmail());
            } else {
                return;
            }
            
            if (entry != null) {
                // 填充UI元素
                dDayField.setText(entry.getDDay() != null ? entry.getDDay() : "");
                priorityField.setText(entry.getPriority() != null ? entry.getPriority() : "");
                routineField.setText(entry.getRoutine() != null ? entry.getRoutine() : "");
                breakfastField.setText(entry.getBreakfast() != null ? entry.getBreakfast() : "");
                lunchField.setText(entry.getLunch() != null ? entry.getLunch() : "");
                dinnerField.setText(entry.getDinner() != null ? entry.getDinner() : "");
                snackField.setText(entry.getSnack() != null ? entry.getSnack() : "");
                anynotesArea.setText(entry.getAnynotes() != null ? entry.getAnynotes() : "");
                
                // 載入待辦事項
                if (todoContainerController != null && entry.getTodo() != null) {
                    todoContainerController.loadTodoList(entry.getTodo());
                }
                
            } else {
                // 如果沒有找到該日期的條目，清空所有欄位
                dDayField.clear();
                priorityField.clear();
                routineField.clear();
                breakfastField.clear();
                lunchField.clear();
                dinnerField.clear();
                snackField.clear();
                anynotesArea.clear();
                
                // 清空待辦事項
                if (todoContainerController != null) {
                    todoContainerController.loadTodoList("");
                }
            }

            // 載入 Google Calendar 事件到時間軸
            loadGoogleCalendarEvents(date);
            
            // 載入 Moodle 事件
            loadMoodleEvents(date);
            
            // 載入 Habit Tracker 數據
            if (habitViewController != null) {
                habitViewController.loadHabitData(date);
            }
            
        } catch (Exception e) {
            // 如果加载日记数据时出错，清空所有字段并显示空白页面
            e.printStackTrace();
            dDayField.clear();
            priorityField.clear();
            routineField.clear();
            breakfastField.clear();
            lunchField.clear();
            dinnerField.clear();
            snackField.clear();
            anynotesArea.clear();
            
            if (todoContainerController != null) {
                todoContainerController.loadTodoList("");
            }
        }
    }

    /**
     * 載入指定日期的 Google Calendar 事件到時間軸
     */
    private void loadGoogleCalendarEvents(LocalDate date) {
        if (syncService == null) {
            return;
        }

        try {
            // 先檢查是否已經標記為空
            DiaryModel entry = database.getDiaryEntry(date, UserSession.getInstance().getCurrentUserEmail());
            if (entry != null && entry.isCalendarEmpty()) {
                System.out.println("該日期已標記為空，跳過API呼叫");
                return;
            }

            List<GoogleCalendarService.CalendarEvent> events = syncService.getEventsForDate(date);
            
            // 只處理整日事件，非整日事件交給ScheduleController處理
            StringBuilder allDayEvents = new StringBuilder();
            String currentAllDayText = priorityField.getText();
            if (currentAllDayText != null && !currentAllDayText.trim().isEmpty()) {
                allDayEvents.append(currentAllDayText).append("\n");
            }
            
            for (GoogleCalendarService.CalendarEvent event : events) {
                // 添加調試信息
                java.time.LocalTime startTime = java.time.Instant.ofEpochMilli(event.getStartTime())
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalTime();
                java.time.LocalTime endTime = java.time.Instant.ofEpochMilli(event.getEndTime())
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalTime();
                
                System.out.println("事件: " + event.getSummary() + 
                                 " | 開始: " + startTime + 
                                 " | 結束: " + endTime + 
                                 " | 整日: " + event.isAllDay());
                
                if (event.isAllDay()) {
                    // 整日事件加到 All day 欄位
                    System.out.println("✅ 整日事件：" + event.getSummary());
                    if (allDayEvents.length() > 0 && !allDayEvents.toString().endsWith("\n")) {
                        allDayEvents.append("\n");
                    }
                    allDayEvents.append(event.getSummary());
                } else {
                    System.out.println("⏰ 非整日事件：" + event.getSummary() + " (" + startTime + "-" + endTime + ")");
                }
            }
            
            // 委託ScheduleController處理非整日事件（只調用一次）
            if (scheduleController != null) {
                scheduleController.loadGoogleCalendarEvents(date);
            }
            
            // 更新整日行程欄位
            if (allDayEvents.length() > 0) {
                String finalText = allDayEvents.toString();
                if (finalText.endsWith("\n")) {
                    finalText = finalText.substring(0, finalText.length() - 2);
                }
                priorityField.setText(finalText);
            }
            
            
            // 如果沒有事件，標記為空
            if (events.isEmpty()) {
                if (entry == null) {
                    entry = new DiaryModel(date);
                    entry.setUser(database.getUserEntry(UserSession.getInstance().getCurrentUserEmail()));
                }
                entry.setCalendarEmpty(true);
                database.saveDiaryEntry(entry);
                System.out.println("該日期沒有事件，已標記為空");
            }
            
            // 在控制台顯示載入的事件數量
            System.out.println("DiaryController 已處理 " + events.size() + " 個 Google Calendar 事件");
            
        } catch (Exception e) {
            System.err.println("載入 Google Calendar 事件時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 載入指定日期的 Moodle 事件
     */
    private void loadMoodleEvents(LocalDate date) {
        try {
            String userEmail = UserManager.getInstance().getCurrentUser().getEmail();
            List<MoodleService.MoodleEvent> events = new ArrayList<>();
            
            // 如果 Moodle 已配置，嘗試從API載入
            if (moodleService != null && moodleService.getWstoken() != null) {
                events = moodleService.getCalendarEventsWithCache(date, userEmail);
            } else {
                // 即使沒有配置，也嘗試從本地資料庫載入已緩存的事件
                if (moodleService != null) {
                    List<MoodleModel> localEvents = moodleService.getLocalMoodleEvents(userEmail, date);
                    if (!localEvents.isEmpty()) {
                        // 轉換為 MoodleEvent
                        for (MoodleModel model : localEvents) {
                            MoodleService.MoodleEvent event = new MoodleService.MoodleEvent();
                            event.setId(model.getId());
                            event.setName(model.getName());
                            event.setTimestart(model.getTimestart());
                            event.setUrl(model.getUrl());
                            event.setCourseName(model.getCourseName());
                            event.setCourseId(model.getCourseId());
                            event.setAssignmentId(model.getAssignmentId());
                            event.setSubmissionStatus(model.getSubmissionStatus());
                            events.add(event);
                        }
                        System.out.println("從本地資料庫載入 " + events.size() + " 個 Moodle 事件");
                    }
                }
            }
            
            if (!events.isEmpty()) {
                // 將 Moodle 事件添加到 All day 欄位
                StringBuilder moodleEvents = new StringBuilder();
                String currentAllDayText = priorityField.getText();
                if (currentAllDayText != null && !currentAllDayText.trim().isEmpty()) {
                    moodleEvents.append(currentAllDayText).append("\n");
                }
                
                for (MoodleService.MoodleEvent event : events) {
                    // 檢查事件是否在指定日期
                    LocalDate eventDate = LocalDate.ofEpochDay(event.getTimestart() / 86400);
                    if (eventDate.equals(date)) {
                        if (moodleEvents.length() > 0 && !moodleEvents.toString().endsWith("\n")) {
                            moodleEvents.append("\n");
                        }
                        
                        // 添加事件信息，包含課程名稱和繳交狀態
                        String eventText = String.format("[%s] %s", event.getCourseName(), event.getName());
                        if (event.getSubmissionStatus() != null) {
                            String statusText = "submitted".equals(event.getSubmissionStatus()) ? "✅" : "❌";
                            eventText += " " + statusText;
                        }
                        moodleEvents.append(eventText);
                        
                        System.out.println("🎓 Moodle事件：" + eventText);
                    }
                }
                
                // 更新 All day 欄位
                if (moodleEvents.length() > 0) {
                    String finalText = moodleEvents.toString();
                    if (finalText.endsWith("\n")) {
                        finalText = finalText.substring(0, finalText.length() - 2);
                    }
                    priorityField.setText(finalText);
                }
                
                System.out.println("DiaryController 已處理 " + events.size() + " 個 Moodle 事件");
            }
            
        } catch (Exception e) {
            System.err.println("載入 Moodle 事件時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateGoogleCalendarStatus() {
        boolean isAuthorized = googleCalendarService.isUserAuthorized();
        addGoogleAPI.setText(isAuthorized ? "已連結Google行事曆" : "新增Google行事曆");
        
        // 如果已授權，啟動同步服務並載入當前日期的事件
        if (isAuthorized) {
            // 啟動定期同步
            syncService.startPeriodicSync();
            // 載入當前日期的事件
            loadGoogleCalendarEvents(currentDate);
        }
    }

    private void updateMoodleStatus() {
        boolean isConfigured = isMoodleConfigured();
        addMoodleAPI.setText(isConfigured ? "已連結Moodle" : "新增Moodle行事曆");
    }

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    /**
     * 顯示 Moodle 配置對話框
     */
    public void showMoodleConfigDialog() {
        Optional<MoodleConfigDialog.MoodleCredentials> result = MoodleConfigDialog.showConfigDialog();
        
        if (result.isPresent()) {
            MoodleConfigDialog.MoodleCredentials credentials = result.get();
            
            // 只支援用戶名/密碼登錄
            if (!credentials.isUseToken()) {
                String userEmail = UserManager.getInstance().getCurrentUser().getEmail();
                final boolean success = configureMoodle(credentials.getUsername(), credentials.getPassword(), userEmail);
                
                // 顯示結果
                Platform.runLater(() -> {
                    Alert alert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
                    alert.setTitle("Moodle 配置");
                    alert.setHeaderText(success ? "配置成功" : "配置失敗");
                    alert.setContentText(success ? 
                        "Moodle 已成功配置並且憑證已保存，下次啟動時會自動登錄。" : 
                        "無法配置 Moodle，請檢查您的用戶名和密碼。");
                    alert.showAndWait();
                });
                
                // 如果成功，更新狀態
                if (success) {
                    updateMoodleStatus();
                }
            } else {
                // 顯示錯誤訊息，不支援token登錄
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("配置錯誤");
                    alert.setHeaderText("不支援的登錄方式");
                    alert.setContentText("系統目前只支援用戶名/密碼登錄方式。");
                    alert.showAndWait();
                });
            }
        }
    }

    /**
     * 配置 Moodle 登錄憑證
     */
    public boolean configureMoodle(String username, String password) {
        return configureMoodle(username, password, null);
    }
    
    /**
     * 配置 Moodle 登錄憑證（帶用戶郵箱）
     */
    public boolean configureMoodle(String username, String password, String userEmail) {
        if (moodleService == null) {
            moodleService = MoodleService.getInstance();
        }
        
        try {
            boolean success = moodleService.login(username, password, userEmail);
            if (success) {
                System.out.println("Moodle 登錄成功，憑證已保存");
                
                // 在背景同步當月的 Moodle 事件
                LocalDate now = LocalDate.now();
                LocalDate monthStart = now.withDayOfMonth(1);
                LocalDate monthEnd = now.withDayOfMonth(now.lengthOfMonth());
                
                new Thread(() -> {
                    try {
                        moodleService.forceSyncMoodleEvents(userEmail, monthStart, monthEnd);
                        System.out.println("Moodle事件初始同步完成");
                        
                        // 在UI線程中重新載入當前日期
                        Platform.runLater(() -> loadDiaryContent(currentDate));
                    } catch (Exception e) {
                        System.err.println("Moodle事件初始同步失敗: " + e.getMessage());
                    }
                }).start();
                
                // 重新載入當前日期以顯示 Moodle 事件
                loadDiaryContent(currentDate);
                return true;
            } else {
                System.err.println("Moodle 登錄失敗");
                return false;
            }
        } catch (Exception e) {
            System.err.println("Moodle 登錄時發生錯誤: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 檢查 Moodle 是否已配置
     */
    public boolean isMoodleConfigured() {
        return moodleService != null && moodleService.getWstoken() != null;
    }
}

   

