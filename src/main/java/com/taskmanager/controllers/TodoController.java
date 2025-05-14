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

    @FXML
    private VBox todoList;

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
            
            // Skip empty todos
            if (tf.getText().trim().isEmpty()) continue;
            
            // Format: completed,text|completed,text|...
            sb.append(cb.isSelected() ? "1" : "0")
              .append(",")
              .append(tf.getText().replace(",", "\\,").replace("|", "\\|"))
              .append("|");
        }
        
        return sb.toString();
    }


    private HBox createRow() {
        CheckBox cb = new CheckBox();
        TextField tf = new TextField();
        tf.setPromptText("Todo…");
        HBox.setHgrow(tf, Priority.ALWAYS);

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
            }
        });

        HBox row = new HBox(6, cb, tf);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    public void loadTodoList(String todoData) {
        // Clear existing todos
        todoList.getChildren().clear();
        
        if (todoData == null || todoData.isEmpty()) {
            // Add default empty rows
            for (int i = 0; i < INIT_ROWS; i++) {
                todoList.getChildren().add(createRow());
            }
            return;
        }
        
        String[] todos = todoData.split("\\|");
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
        
        // Ensure we have at least one empty row at the end
        todoList.getChildren().add(createRow());
    }
} 