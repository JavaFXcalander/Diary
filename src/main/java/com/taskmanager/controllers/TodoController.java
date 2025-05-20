package com.taskmanager.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class TodoController {
    private static final int INIT_ROWS = 5;
    private static final double PADDING = 4;
    
    private TodoChangeListener changeListener;

    @FXML
    private VBox todoList;
    
    // 設置變更監聽器
    public void setChangeListener(TodoChangeListener listener) {
        this.changeListener = listener;
    }

    @FXML
    public void initialize() {
        todoList.setPadding(new Insets(PADDING));
        
        // Create initial rows
        for (int i = 0; i < INIT_ROWS; i++) {
            todoList.getChildren().add(createRow());
        }
    }

    public String serializeTodoList() {
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < todoList.getChildren().size(); i++) {
            HBox row = (HBox) todoList.getChildren().get(i);
            CheckBox cb = (CheckBox) row.getChildren().get(0);
            TextField tf = (TextField) row.getChildren().get(1);
            
            // 保存所有行，包括空行
            String text = tf.getText().trim();
            
            // Format: completed,text|completed,text|...
            sb.append(cb.isSelected() ? "1" : "0")
              .append(",")
              .append(text.replace(",", "\\,").replace("|", "\\|"))
              .append("|");
        }
        
        return sb.toString();
    }


    private HBox createRow() {
        CheckBox cb = new CheckBox();
        TextField tf = new TextField();
        tf.setPromptText("Todo…");
        HBox.setHgrow(tf, Priority.ALWAYS);

        // 添加失焦事件監聽器
        tf.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // 當失去焦點時
                notifyChange();
            }
        });
        
        // 添加CheckBox選擇變更監聽器
        cb.selectedProperty().addListener((obs, oldVal, newVal) -> {
            notifyChange();
        });

        // Add new row when Enter is pressed on the last row
        tf.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER &&
                todoList.getChildren().indexOf(tf.getParent()) ==
                todoList.getChildren().size() - 1) {
                    todoList.getChildren().add(createRow());
                    todoList.layout();
                    todoList.getChildren()
                            .get(todoList.getChildren().size()-1)
                            .requestFocus();
                    notifyChange();
            }
        });

        HBox row = new HBox(6, cb, tf);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    public void loadTodoList(String todoData) {
        // 完全清空现有todos
        todoList.getChildren().clear();
        
        if (todoData == null || todoData.isEmpty()) {
            // Add default empty rows
            for (int i = 0; i < INIT_ROWS; i++) {
                todoList.getChildren().add(createRow());
            }
            return;
        }
        
        String[] todos = todoData.split("\\|");
                
        // 加载保存的行数据
        for (String todo : todos) {
            if (todo.isEmpty()) continue;
            
            String[] parts = todo.split(",", 2);
            if (parts.length < 2) continue;
            
            boolean completed = "1".equals(parts[0]);
            String text = parts[1].replace("\\,", ",").replace("\\|", "|");
            
            HBox row = createRow();
            CheckBox cb = (CheckBox) row.getChildren().get(0);
            TextField tf = (TextField) row.getChildren().get(1);
            
            cb.setSelected(completed);
            tf.setText(text);
            
            todoList.getChildren().add(row);
            
        }
        
        // 如果加载的行数少于INIT_ROWS，则添加空行直到达到INIT_ROWS
        while (todoList.getChildren().size() < INIT_ROWS) {
            todoList.getChildren().add(createRow());
        }
    }
    
    // 通知數據變更
    private void notifyChange() {
        if (changeListener != null) {
            changeListener.onTodoChanged();
        }
    }
} 