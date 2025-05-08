package com.taskmanager.models;

import com.taskmanager.models.TodoItem;
import com.taskmanager.models.BudgetItem;
import com.taskmanager.models.MealEntry;
import com.taskmanager.models.HabitEntry;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import java.time.LocalDate;

public class DiaryEntry {
    private final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>();
    private final StringProperty dDay = new SimpleStringProperty("");
    private final StringProperty priority = new SimpleStringProperty("");
    private final StringProperty routine = new SimpleStringProperty("");
    private final ListProperty<TodoItem> todoItems = new SimpleListProperty<>();
    private final ListProperty<BudgetItem> budgetItems = new SimpleListProperty<>();
    private final StringProperty photoCollage = new SimpleStringProperty("");
    private final ListProperty<MealEntry> meals = new SimpleListProperty<>();
    private final ListProperty<HabitEntry> habits = new SimpleListProperty<>();
    private final StringProperty anynotes = new SimpleStringProperty("");

    public DiaryEntry() {
        this(LocalDate.now());
    }

    public DiaryEntry(LocalDate date) {
        setDate(date);
    }

    // Getters and setters for all properties
    public LocalDate getDate() { return date.get(); }
    public void setDate(LocalDate value) { date.set(value); }
    public ObjectProperty<LocalDate> dateProperty() { return date; }

    public String getDDay() { return dDay.get(); }
    public void setDDay(String value) { dDay.set(value); }
    public StringProperty dDayProperty() { return dDay; }

    public String getPriority() { return priority.get(); }
    public void setPriority(String value) { priority.set(value); }
    public StringProperty priorityProperty() { return priority; }

    public String getRoutine() { return routine.get(); }
    public void setRoutine(String value) { routine.set(value); }
    public StringProperty routineProperty() { return routine; }

    public ListProperty<TodoItem> todoItemsProperty() { return todoItems; }
    public ListProperty<BudgetItem> budgetItemsProperty() { return budgetItems; }
    public ListProperty<MealEntry> mealsProperty() { return meals; }
    public ListProperty<HabitEntry> habitsProperty() { return habits; }

    public String getPhotoCollage() { return photoCollage.get(); }
    public void setPhotoCollage(String value) { photoCollage.set(value); }
    public StringProperty photoCollageProperty() { return photoCollage; }

    public String getAnynotes() { return anynotes.get(); }
    public void setAnynotes(String value) { anynotes.set(value); }
    public StringProperty anynotesProperty() { return anynotes; }
}