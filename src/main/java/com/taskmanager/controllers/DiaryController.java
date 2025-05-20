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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import com.taskmanager.database.DiaryDatabase;
import com.taskmanager.models.DiaryModel;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DiaryController implements TodoChangeListener {

    @FXML private Label dateLabel;
    @FXML private TextField dDayField, priorityField, routineField;
    @FXML private TextField breakfastField, lunchField, dinnerField, snackField;
    @FXML private TextArea anynotesArea;
    @FXML private VBox todoContainer;
    @FXML private TodoController todoContainerController; // This will be automatically injected by JavaFX
    
    private LocalDate currentDate = LocalDate.now();
    private DiaryDatabase database = DiaryDatabase.getInstance();

   
    
    
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
            DiaryModel entry = database.getDiaryEntry(date);
            
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
    }

