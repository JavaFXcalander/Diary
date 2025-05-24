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
import com.taskmanager.services.UserSession;
import com.taskmanager.models.UserModel;
import com.taskmanager.services.GoogleCalendarService;
import javafx.application.HostServices;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import java.security.GeneralSecurityException;
import com.taskmanager.services.UserManager;
import com.taskmanager.MainApp;

public class DiaryController implements TodoChangeListener {

    @FXML private Label dateLabel;
    @FXML private TextField dDayField, priorityField, routineField;
    @FXML private TextField breakfastField, lunchField, dinnerField, snackField;
    @FXML private TextArea anynotesArea;
    @FXML private VBox todoContainer;
    @FXML private TodoController todoContainerController; // This will be automatically injected by JavaFX
    @FXML private Button addButton;
    
    private LocalDate currentDate = LocalDate.now();
    private DiaryDatabase database = DiaryDatabase.getInstance();
    private ContextMenu addMenu;
    private GoogleCalendarService googleCalendarService;
    private HostServices hostServices;
    private MenuItem addGoodleAPI;

    @FXML
    public void initialize() {
        // 設定今天日期格式
        currentDate = LocalDate.now();
        updateDateDisplay();
        
        // 設置 TodoController 的變更監聽器
        if (todoContainerController != null) {
            todoContainerController.setChangeListener(this);
        }
        
        // 載入當天的日記內容
        loadDiaryContent(currentDate);
        
        // 設定所有輸入欄位的失焦事件處理
        setupBlurEventHandlers();

        // 建立 ContextMenu
        addMenu = new ContextMenu();
        setupContextMenu();

        addButton.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                if (addMenu.isShowing()) {
                    addMenu.hide();
                } else {
                    addMenu.show(addButton, Side.BOTTOM, -addButton.getWidth()-40, 0);
                }
            }
        });
        // 可加上各選項的事件處理
        addGoodleAPI.setOnAction(ev -> {
            try {
                if (googleCalendarService.isUserAuthorized()) {
                    Alert alert = new Alert(AlertType.INFORMATION);
                    alert.setTitle("Google行事曆");
                    alert.setHeaderText(null);
                    alert.setContentText("您已經連結Google行事曆");
                    alert.showAndWait();
                } else {
                    String authUrl = googleCalendarService.getAuthorizationUrl();
                    if (hostServices != null) {
                        hostServices.showDocument(authUrl);
                    }
                    // 等待用戶授權後更新狀態
                    updateGoogleCalendarStatus();
                }
            } catch (IOException | GeneralSecurityException e) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("錯誤");
                alert.setHeaderText(null);
                alert.setContentText("無法連接到Google行事曆: " + e.getMessage());
                alert.showAndWait();
            }
        });
        
        googleCalendarService = new GoogleCalendarService(UserManager.getInstance().getCurrentUser().getEmail());
        setHostServices(MainApp.getHostServicesInstance());
        updateGoogleCalendarStatus();
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

    private void updateGoogleCalendarStatus() {
        boolean isAuthorized = googleCalendarService.isUserAuthorized();
        addGoodleAPI.setText(isAuthorized ? "已連結Google行事曆" : "新增Google行事曆");
    }

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    private void setupContextMenu() {
        addMenu = new ContextMenu();
        MenuItem addTask = new MenuItem("新增待辦事項");
        MenuItem addEvent = new MenuItem("新增事件");
        addGoodleAPI = new MenuItem("新增Google行事曆");

        addMenu.getItems().addAll(addTask, addEvent, addGoodleAPI);
    }
}

   

