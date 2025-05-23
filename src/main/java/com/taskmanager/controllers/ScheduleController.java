package com.taskmanager.controllers;

import com.taskmanager.timeline.TimeAxisPane;
import com.taskmanager.timeline.TaskView;
import com.google.api.services.calendar.model.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.util.ResourceBundle;
import java.util.List;

public class ScheduleController implements Initializable {

    @FXML
    private TimeAxisPane timeAxisPane;
    
    @FXML
    private HBox buttonContainer;
    

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
    
    
    
}
