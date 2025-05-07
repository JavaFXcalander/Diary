package com.gamified.taskmanager.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class HabitController {
    private static final int INIT_ROWS = 5;
    private static final double PADDING = 4;

    @FXML
    private VBox habitTracker;

    @FXML
    public void initialize() {
        habitTracker.setPadding(new Insets(PADDING));
        
        // Create initial rows
        for (int i = 0; i < INIT_ROWS; i++) {
            habitTracker.getChildren().add(createRow());
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
            habitTracker.getChildren().indexOf(tf.getParent()) ==
            habitTracker.getChildren().size() - 1) {
                habitTracker.getChildren().add(createRow());
                habitTracker.layout();
                habitTracker.getChildren()
                            .get(habitTracker.getChildren().size()-1)
                            .requestFocus();
            }
        });

        HBox row = new HBox(6, cb, tf);
        return row;
    }
} 