package com.taskmanager.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DiaryController {

    @FXML private Label dateLabel;
    @FXML private TextField dDayField, priorityField, routineField;
    @FXML private TextField breakfastField, lunchField, dinnerField, snackField;
    @FXML
    public void initialize() {
        // 設定今天日期格式
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy / MM / dd - EEEE", Locale.ENGLISH); // 設定日期格式
        String formattedDate = today.format(formatter); // 格式化日期
        dateLabel.setText(formattedDate); // 顯示日期於 Label 上
        dateLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: normal;");
         dateLabel.setPadding(Insets.EMPTY);
    }

    @FXML
    private void showPreviousDate() {
        // 你可以在這裡加上切換日期的邏輯
    }

    @FXML
    private void showNextDate() {
        // 你可以在這裡加上切換日期的邏輯
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
        // 格式化並顯示選擇的日期
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy / MM / dd - EEEE", Locale.ENGLISH);
        String formattedDate = date.format(formatter);
        dateLabel.setText(formattedDate);
        dateLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: normal;");
        dateLabel.setPadding(Insets.EMPTY);
        
        // 你可以在這裡加載該日期的日記內容
        loadDiaryContent(date);
    }
    
    private void loadDiaryContent(LocalDate date) {
        // TODO: 從數據存儲中加載選定日期的日記內容
        // 這裡可以添加加載待辦事項、預算、習慣等數據的邏輯
    }
    
}
