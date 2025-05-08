package com.taskmanager.models;

import javafx.beans.property.*;

public class HabitEntry {
    private final StringProperty habitName = new SimpleStringProperty("");
    private final BooleanProperty completed = new SimpleBooleanProperty(false);
    private final IntegerProperty streak = new SimpleIntegerProperty(0);

    public HabitEntry() {
        this("");
    }

    public HabitEntry(String habitName) {
        setHabitName(habitName);
    }

    public String getHabitName() { return habitName.get(); }
    public void setHabitName(String value) { habitName.set(value); }
    public StringProperty habitNameProperty() { return habitName; }

    public boolean isCompleted() { return completed.get(); }
    public void setCompleted(boolean value) { completed.set(value); }
    public BooleanProperty completedProperty() { return completed; }

    public int getStreak() { return streak.get(); }
    public void setStreak(int value) { streak.set(value); }
    public IntegerProperty streakProperty() { return streak; }
} 