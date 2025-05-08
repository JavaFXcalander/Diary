package com.taskmanager.models;

import javafx.beans.property.*;

public class TodoItem {
    private final StringProperty task = new SimpleStringProperty("");
    private final BooleanProperty completed = new SimpleBooleanProperty(false);

    public TodoItem() {
        this("");
    }

    public TodoItem(String task) {
        setTask(task);
    }

    public String getTask() { return task.get(); }
    public void setTask(String value) { task.set(value); }
    public StringProperty taskProperty() { return task; }

    public boolean isCompleted() { return completed.get(); }
    public void setCompleted(boolean value) { completed.set(value); }
    public BooleanProperty completedProperty() { return completed; }
} 