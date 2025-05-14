package com.taskmanager.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class ProjectController {

    @FXML
    private ProgressBar progressBar;

    @FXML
    private VBox checklistVBox; // Checklist 項目的容器

    @FXML
    private VBox taskBox;

    @FXML
    private CheckBox task1CheckBox;

    @FXML
    private TextField task1TextField;

    @FXML
    private CheckBox task2CheckBox;

    @FXML
    private TextField task2TextField;

    @FXML
    private CheckBox task3CheckBox;

    @FXML
    private TextField task3TextField;

    @FXML
    private CheckBox task4CheckBox;

    @FXML
    private TextField task4TextField;

    // 用來儲存每個 checklist 項目的 CheckBox 與 TextField
    private List<ChecklistItem> checklistItems = new ArrayList<>();

    @FXML
    public void initialize() {
        // 取得 checklistVBox 裡的每個 HBox 項目
        for (var node : checklistVBox.getChildren()) {
            if (node instanceof HBox) {
                HBox hbox = (HBox) node;
                if (hbox.getChildren().size() >= 2) {
                    var first = hbox.getChildren().get(0);
                    var second = hbox.getChildren().get(1);
                    if (first instanceof CheckBox && second instanceof TextField) {
                        CheckBox cb = (CheckBox) first;
                        TextField tf = (TextField) second;
                        ChecklistItem item = new ChecklistItem(cb, tf);
                        checklistItems.add(item);
                        // 當勾選狀態改變時更新進度
                        cb.selectedProperty().addListener((obs, oldVal, newVal) -> updateProgress());
                        // 當文字改變時更新進度
                        tf.textProperty().addListener((obs, oldVal, newVal) -> updateProgress());
                    }
                }
            }
        }
        updateProgress();
    }

    // 根據有效（有填文字）的任務數及勾選項目數更新進度條
    private void updateProgress() {
        int validTasks = 0;
        int checkedTasks = 0;
        for (ChecklistItem item : checklistItems) {
            if (!item.textField.getText().trim().isEmpty()) {
                validTasks++;
                if (item.checkBox.isSelected()) {
                    checkedTasks++;
                }
            }
        }
        double progress = validTasks == 0 ? 0 : (double) checkedTasks / validTasks;
        progressBar.setProgress(progress);
    }

    // 儲存 CheckBox 與 TextField 的簡單資料結構
    private static class ChecklistItem {
        CheckBox checkBox;
        TextField textField;

        ChecklistItem(CheckBox cb, TextField tf) {
            this.checkBox = cb;
            this.textField = tf;
        }
    }
}
