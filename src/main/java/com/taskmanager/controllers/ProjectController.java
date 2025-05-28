package com.taskmanager.controllers;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.scene.Node; // 導入 Node 類

import com.taskmanager.database.DiaryDatabase;
import com.taskmanager.models.ProjectModel;
import com.taskmanager.models.UserModel;
import com.taskmanager.services.UserSession;






public class ProjectController {
    
    // HabitController的引用，用於同步
    private static HabitController habitController;

    @FXML
    private VBox taskContainer1;
    @FXML
    private VBox taskContainer2;
    @FXML
    private VBox taskContainer3;
    @FXML
    private VBox taskContainer4;

    @FXML
    private ProgressBar progressBar1;
    @FXML
    private ProgressBar progressBar2;
    @FXML
    private ProgressBar progressBar3;
    @FXML
    private ProgressBar progressBar4;

    @FXML
    private Label monthYearLabel;  // 用來顯示月份的 Label

    private int currentMonth;
    private int currentYear;
    private DiaryDatabase database = DiaryDatabase.getInstance();

    @FXML
    private TextField project1;
    @FXML
    private TextField project2;
    @FXML
    private TextField project3;
    @FXML
    private TextField project4;

    @FXML
    private TextArea about1;

    @FXML
    private TextArea about2;

    @FXML
    private TextArea about3;

    @FXML
    private TextArea about4;

    @FXML
    private TextField habit1;

    @FXML
    private TextField habit2;

    @FXML
    private TextField habit3;

    @FXML
    private TextField habit4;

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


    @FXML
    public void initialize() {
        // Initialize with current date if not set
        if (currentMonth == 0 || currentYear == 0) {
            LocalDate now = LocalDate.now();
            currentMonth = now.getMonthValue();
            currentYear = now.getYear();
        }
        setMonth(currentMonth, currentYear);
        setupProgressBar(taskContainer1, progressBar1);
        setupProgressBar(taskContainer2, progressBar2);
        setupProgressBar(taskContainer3, progressBar3);
        setupProgressBar(taskContainer4, progressBar4);

        VBox[] taskContainers = {taskContainer1, taskContainer2, taskContainer3, taskContainer4};

        for (VBox taskContainer : taskContainers) {
            for (var node : taskContainer.getChildren()) {
                if (node instanceof HBox) {
                    HBox hbox = (HBox) node;
                    hbox.getChildren().forEach(child -> {
                        if (child instanceof TextField) {
                            TextField textField = (TextField) child;
                            textField.setOnAction(event -> moveToNextTextField(taskContainer, textField));
                        }
                    });
                }
            }
        }
        
        // 設定所有輸入欄位的失焦事件處理
        setupBlurEventHandlers();
        
        // 設定CheckBox事件監聽器
        setupCheckBoxListeners();

    }

    private void setupProgressBar(VBox taskContainer, ProgressBar progressBar) {
        for (var node : taskContainer.getChildren()) {
            if (node instanceof HBox) {
                HBox hbox = (HBox) node;
                hbox.getChildren().forEach(child -> {
                    if (child instanceof CheckBox) {
                        ((CheckBox) child).setOnAction(event -> updateProgress(taskContainer, progressBar));
                    } else if (child instanceof TextField) {
                        ((TextField) child).textProperty().addListener((observable, oldValue, newValue) -> updateProgress(taskContainer, progressBar));
                    }
                });
            }
        }
        updateProgress(taskContainer, progressBar);
    }

    private void updateProgress(VBox taskContainer, ProgressBar progressBar) {
        long totalTasks = taskContainer.getChildren().stream()
                .filter(node -> node instanceof HBox)
                .map(node -> (HBox) node)
                .filter(hbox -> hbox.getChildren().stream()
                        .filter(child -> child instanceof TextField)
                        .map(child -> (TextField) child)
                        .anyMatch(textField -> !textField.getText().trim().isEmpty()))
                .count();

        if (totalTasks == 0) {
            progressBar.setProgress(0);
            return;
        }

        long completedTasks = taskContainer.getChildren().stream()
                .filter(node -> node instanceof HBox)
                .map(node -> (HBox) node)
                .filter(hbox -> {
                    boolean isCheckBoxSelected = hbox.getChildren().stream()
                            .filter(child -> child instanceof CheckBox)
                            .map(child -> (CheckBox) child)
                            .anyMatch(CheckBox::isSelected);

                    boolean isTextFieldFilled = hbox.getChildren().stream()
                            .filter(child -> child instanceof TextField)
                            .map(child -> (TextField) child)
                            .anyMatch(textField -> !textField.getText().trim().isEmpty());

                    return isCheckBoxSelected && isTextFieldFilled;
                })
                .count();

        double progress = (double) completedTasks / totalTasks;
        progressBar.setProgress(progress);
    }

    private void moveToNextTextField(VBox taskContainer, TextField currentTextField) {
        boolean focusNext = false;
        for (var node : taskContainer.getChildren()) {
            if (node instanceof HBox) {
                HBox hbox = (HBox) node;
                for (var child : hbox.getChildren()) {
                    if (child instanceof TextField) {
                        TextField textField = (TextField) child;
                        if (focusNext) {
                            textField.requestFocus();
                            return;
                        }
                        if (textField == currentTextField) {
                            focusNext = true;
                        }
                    }
                }
            }
        }
    }


    //新加的

    public void setMonth(int month, int year) {
        this.currentMonth = month;
        this.currentYear = year;
        // 使用月份和年份設置標籤的文本
        String monthName = LocalDate.of(year, month, 1).getMonth().getDisplayName(TextStyle.FULL, Locale.US);
        monthYearLabel.setText(year +" " + monthName + " " + "project");
        loadProjectContent(year, month);

    }

   @FXML
    private void handleMonthButton(ActionEvent event) throws IOException {
        Parent diaryRoot = FXMLLoader.load(getClass().getResource("/fxml/calendar.fxml"));
        Scene scene = ((Node) event.getSource()).getScene();
        scene.setRoot(diaryRoot);
    }

    @FXML
    private void handleDiaryButton(ActionEvent event) throws IOException {
        Parent diaryRoot = FXMLLoader.load(getClass().getResource("/fxml/DiaryView.fxml"));
        Scene scene = ((Node) event.getSource()).getScene();
        scene.setRoot(diaryRoot);
    }

    public void loadProjectContent(int year, int month) {
        // 從數據庫中加載選定日期的日記內容
        ProjectModel entry;
        UserSession userSession = UserSession.getInstance();

        if (userSession.isLoggedIn()) {
            entry = database.getProjectEntry(year, month, userSession.getCurrentUserEmail());
        } else {
            return;
        }
        
        if (entry != null) {
            // 填充UI元素
                project1.setText(entry.getProject1());
                System.out.println("get = "+ entry.getProject2());
                project2.setText(entry.getProject2());
                project3.setText(entry.getProject3());
                project4.setText(entry.getProject4());
                about1.setText(entry.getAbout1());
                about2.setText(entry.getAbout2());
                about3.setText(entry.getAbout3());
                about4.setText(entry.getAbout4());
                habit1.setText(entry.getHabit1());
                habit2.setText(entry.getHabit2());
                habit3.setText(entry.getHabit3());
                habit4.setText(entry.getHabit4());

                Map<Integer, boolean[]> dailyChecks = entry.getDailyChecks();
                for (int day = 1; day <= 31; day++) {
                    try {
                        VBox dayContainer = (VBox) getClass().getDeclaredField("dayContainer" + day).get(this);
                        boolean[] checks = dailyChecks.getOrDefault(day, new boolean[4]);
                        for (int i = 0; i < 4; i++) {
                            CheckBox checkBox = (CheckBox) dayContainer.getChildren().get(i + 1);
                            checkBox.setSelected(checks[i]);
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }
    
            
        } else {
            project1.clear();
            System.out.println("沒");
        }
    }

    private void setupBlurEventHandlers() {
        // 為每個輸入欄位添加失焦事件處理器
        project1.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                saveProjectEntry();
            }
        });
        project2.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                saveProjectEntry();
            }
        });
        project3.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                saveProjectEntry();
            }
        });
        project4.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                saveProjectEntry();
            }
        });
        about1.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                saveProjectEntry();
            }
        });
        about2.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                saveProjectEntry();
            }
        });
        about3.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                saveProjectEntry();
            }
        });
        about4.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                saveProjectEntry();
            }
        });
        habit1.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                saveProjectEntry();
            }
        });
        habit2.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                saveProjectEntry();
            }
        });
        habit3.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                saveProjectEntry();
            }
        });
        habit4.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                saveProjectEntry();
            }
        });

          
    }

    private void saveProjectEntry() {
        ProjectModel entry = new ProjectModel(currentYear, currentMonth);
        //System.out.println(currentYear + " " + currentMonth);
        entry.setProject1(project1.getText());
        entry.setProject2(project2.getText());
        //System.out.println("string = "+ entry.getProject2());
        entry.setProject3(project3.getText());
        entry.setProject4(project4.getText());
        entry.setAbout1(about1.getText());
        entry.setAbout2(about2.getText());
        entry.setAbout3(about3.getText());
        entry.setAbout4(about4.getText());
        entry.setHabit1(habit1.getText());
        entry.setHabit2(habit2.getText());
        entry.setHabit3(habit3.getText());
        entry.setHabit4(habit4.getText());
        Map<Integer, boolean[]> dailyChecks = collectDailyChecks();
        entry.setDailyChecks(dailyChecks);

        UserSession userSession = UserSession.getInstance();
        String email = userSession.getCurrentUserEmail();;
        if (userSession.isLoggedIn()) {
            UserModel user = database.getUserEntry(email);
            if (user != null) {
                entry.setUser(user);
            }
        }
    
        // 保存到數據庫
        database.saveProject(entry ,email);
    }

    private Map<Integer, boolean[]> collectDailyChecks() {
        Map<Integer, boolean[]> dailyChecks = new HashMap<>();
        
        // 假設您的UI中有31天的VBox容器，命名為dayContainer1~dayContainer31
        for (int day = 1; day <= 31; day++) {
            try {
                // 動態獲取每天的VBox（例如：dayContainer1）
                VBox dayContainer = (VBox) getClass().getDeclaredField("dayContainer" + day).get(this);
                
                boolean[] checks = new boolean[4];
                for (int i = 0; i < 4; i++) {
                    CheckBox checkBox = (CheckBox) dayContainer.getChildren().get(i + 1); // 跳過Label
                    checks[i] = checkBox.isSelected();
                }
                dailyChecks.put(day, checks);
            } catch (Exception e) {
                // 如果某天的容器不存在，跳過
                continue;
            }
        } 
        return dailyChecks;
    }

    private void setupCheckBoxListeners() {
        // 設定CheckBox事件監聽器
        VBox[] taskContainers = {taskContainer1, taskContainer2, taskContainer3, taskContainer4};
        ProgressBar[] progressBars = {progressBar1, progressBar2, progressBar3, progressBar4};
        
        for (int i = 0; i < taskContainers.length; i++) {
            VBox taskContainer = taskContainers[i];
            ProgressBar progressBar = progressBars[i];
            
            for (var node : taskContainer.getChildren()) {
                if (node instanceof HBox) {
                    HBox hbox = (HBox) node;
                    hbox.getChildren().forEach(child -> {
                        if (child instanceof CheckBox) {
                            CheckBox checkBox = (CheckBox) child;
                            checkBox.setOnAction(event -> {
                                updateProgress(taskContainer, progressBar);
                                saveProjectEntry(); // 自動保存
                            });
                        }
                    });
                }
            }
        }
        
        // 為每個dayContainer中的CheckBox添加事件監聽器
        for (int day = 1; day <= 31; day++) {
            try {
                VBox dayContainer = (VBox) getClass().getDeclaredField("dayContainer" + day).get(this);
                if (dayContainer != null) {
                    for (int i = 0; i < dayContainer.getChildren().size(); i++) {
                        if (dayContainer.getChildren().get(i) instanceof CheckBox) {
                            CheckBox checkBox = (CheckBox) dayContainer.getChildren().get(i);
                            checkBox.setOnAction(event -> saveProjectEntry());
                        }
                    }
                }
            } catch (Exception e) {
                // 如果某天的容器不存在，跳過
                continue;
            }
        }
    }

    
    
}