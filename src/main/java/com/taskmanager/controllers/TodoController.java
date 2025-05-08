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
} 