package com.taskmanager.models;

import javafx.beans.property.*;

public class MealEntry {
    private final StringProperty mealType = new SimpleStringProperty("");
    private final StringProperty description = new SimpleStringProperty("");
    private final IntegerProperty satisfaction = new SimpleIntegerProperty(0);

    public MealEntry() {
        this("", "");
    }

    public MealEntry(String mealType, String description) {
        setMealType(mealType);
        setDescription(description);
    }

    public String getMealType() { return mealType.get(); }
    public void setMealType(String value) { mealType.set(value); }
    public StringProperty mealTypeProperty() { return mealType; }

    public String getDescription() { return description.get(); }
    public void setDescription(String value) { description.set(value); }
    public StringProperty descriptionProperty() { return description; }

    public int getSatisfaction() { return satisfaction.get(); }
    public void setSatisfaction(int value) { satisfaction.set(value); }
    public IntegerProperty satisfactionProperty() { return satisfaction; }
} 