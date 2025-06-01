package com.taskmanager.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.checkerframework.checker.units.qual.C;

import com.taskmanager.database.DiaryDatabase;
import com.taskmanager.models.ProjectModel;
import com.taskmanager.models.UserModel;
import com.taskmanager.services.UserSession;

public class HabitController {
    private static final double PADDING = 4;

    @FXML
    private VBox habitTracker;
    
    // 每日勾選用的 VBox 容器
    @FXML private VBox dayContainer1;
    @FXML private VBox dayContainer2;
    @FXML private VBox dayContainer3;
    @FXML private VBox dayContainer4;
    @FXML private VBox dayContainer5;
    @FXML private VBox dayContainer6;
    @FXML private VBox dayContainer7;
    @FXML private VBox dayContainer8;
    @FXML private VBox dayContainer9;
    @FXML private VBox dayContainer10;
    @FXML private VBox dayContainer11;
    @FXML private VBox dayContainer12;
    @FXML private VBox dayContainer13;
    @FXML private VBox dayContainer14;
    @FXML private VBox dayContainer15;
    @FXML private VBox dayContainer16;
    @FXML private VBox dayContainer17;
    @FXML private VBox dayContainer18;
    @FXML private VBox dayContainer19;
    @FXML private VBox dayContainer20;
    @FXML private VBox dayContainer21;
    @FXML private VBox dayContainer22;
    @FXML private VBox dayContainer23;
    @FXML private VBox dayContainer24;
    @FXML private VBox dayContainer25;
    @FXML private VBox dayContainer26;
    @FXML private VBox dayContainer27;
    @FXML private VBox dayContainer28;
    @FXML private VBox dayContainer29;
    @FXML private VBox dayContainer30;
    @FXML private VBox dayContainer31;
    
    private DiaryDatabase database = DiaryDatabase.getInstance();
    private int currentMonth;
    private int currentYear;
    private int currentDay;
    private ProjectModel projectEntry;

    @FXML
    public void initialize() {
        habitTracker.setPadding(new Insets(PADDING));
        
        // 获取当前日期的月份和年份
        LocalDate now = LocalDate.now();
        currentMonth = now.getMonthValue();
        currentYear = now.getYear();
        currentDay = now.getDayOfMonth();
        
        // 加载habit数据
        loadHabitData();
    }
    
    public void loadHabitData() {
        loadHabitData(LocalDate.now());
    }
    
    public void loadHabitData(LocalDate date) {
        // 更新當前日期信息
        currentDay = date.getDayOfMonth();
        currentMonth = date.getMonthValue();
        currentYear = date.getYear();
        
        // 清空现有的行
        habitTracker.getChildren().clear();
        
        // 从数据库获取当前月份的项目数据
        projectEntry = null;
        UserSession userSession = UserSession.getInstance();
        
        if (userSession.isLoggedIn()) {
            projectEntry = database.getProjectEntry(currentYear, currentMonth, userSession.getCurrentUserEmail());
        }
        
        // 创建4行habit追踪器
        String[] habitTexts = new String[4];
        if (projectEntry != null) {
            habitTexts[0] = projectEntry.getHabit1() != null ? projectEntry.getHabit1() : "";
            habitTexts[1] = projectEntry.getHabit2() != null ? projectEntry.getHabit2() : "";
            habitTexts[2] = projectEntry.getHabit3() != null ? projectEntry.getHabit3() : "";
            habitTexts[3] = projectEntry.getHabit4() != null ? projectEntry.getHabit4() : "";
        } else {
            // 如果没有数据，使用空字符串
            for (int i = 0; i < 4; i++) {
                habitTexts[i] = "";
            }
        }
        
        // 创建habit行
        Map<Integer, boolean[]> dailyChecks = (projectEntry != null) ? projectEntry.getDailyChecks() : new HashMap<>();
        for (int i = 0; i < 4; i++) {
            habitTracker.getChildren().add(createHabitRow(habitTexts[i], i, dailyChecks));
        }
    }
    
    public void setMonth(int month, int year) {
        this.currentMonth = month;
        this.currentYear = year;
        loadHabitData();
    }

    private HBox createHabitRow(String habitText, int i, Map<Integer, boolean[]> dailyChecks ) {
        CheckBox checkBox = new CheckBox();
        
        // 從 dailyChecks 中獲取當天的勾選狀態
        if (dailyChecks != null) {
            boolean[] checks = dailyChecks.getOrDefault(currentDay, new boolean[4]);
            if (i >= 0 && i < checks.length) {
                    checkBox.setSelected(checks[i]);
                System.err.println("設置 habit " + i + " 的狀態為: " + checks[i]);
                }
        }
        
        // 設置 CheckBox 的事件監聽器
        checkBox.setOnAction(event -> saveProjectEntry());
           
        TextField tf = new TextField();
        tf.setText(habitText);
        tf.setPromptText("Habit…");
        tf.setEditable(false); // 设为只读，因为habit文本来自Project页面
        HBox.setHgrow(tf, Priority.ALWAYS);

        HBox row = new HBox(6, checkBox, tf);
        return row;
    }

    private void saveProjectEntry() {
        // 首先获取现有的ProjectModel，保持其他数据不变
        UserSession userSession = UserSession.getInstance();
        if (!userSession.isLoggedIn()) {
            return;
        }
        
        ProjectModel entry = database.getProjectEntry(currentYear, currentMonth, userSession.getCurrentUserEmail());
        if (entry == null) {
            entry = new ProjectModel(currentYear, currentMonth);
            UserModel user = database.getUserEntry(userSession.getCurrentUserEmail());
            if (user != null) {
                entry.setUser(user);
            }
        }
        
        // 更新每日勾选状态
        Map<Integer, boolean[]> dailyChecks = collectDailyChecks();
        entry.setDailyChecks(dailyChecks);
    
        // 保存到數據庫
        database.saveProject(entry, userSession.getCurrentUserEmail());
    }

    private Map<Integer, boolean[]> collectDailyChecks() {
        // 獲取現有的每日勾選數據，保留其他天的狀態
        Map<Integer, boolean[]> dailyChecks = new HashMap<>();
        if (projectEntry != null && projectEntry.getDailyChecks() != null) {
            // 複製現有的所有天數據
            for (Map.Entry<Integer, boolean[]> entry : projectEntry.getDailyChecks().entrySet()) {
                dailyChecks.put(entry.getKey(), entry.getValue().clone());
            }
        }
        
        // 只更新當前天的勾選狀態
                 boolean[] checks = new boolean[4];
        for (int i = 0; i < habitTracker.getChildren().size() && i < 4; i++) {
            if (habitTracker.getChildren().get(i) instanceof HBox) {
                HBox row = (HBox) habitTracker.getChildren().get(i);
                if (row.getChildren().size() > 0 && row.getChildren().get(0) instanceof CheckBox) {
                    CheckBox checkBox = (CheckBox) row.getChildren().get(0);
                     checks[i] = checkBox.isSelected();
                }
            }
                 }
                 dailyChecks.put(currentDay, checks);
     
         return dailyChecks;
     }
        
    
}