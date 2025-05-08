package com.taskmanager.models;

import javafx.beans.property.*;

public class BudgetItem {
    private final StringProperty details = new SimpleStringProperty("");
    private final DoubleProperty amount = new SimpleDoubleProperty(0.0);
    private final BooleanProperty isIncome = new SimpleBooleanProperty(true);

    public BudgetItem() {
        this("", 0.0, true);
    }

    public BudgetItem(String details, double amount, boolean isIncome) {
        setDetails(details);
        setAmount(amount);
        setIsIncome(isIncome);
    }

    public String getDetails() { return details.get(); }
    public void setDetails(String value) { details.set(value); }
    public StringProperty detailsProperty() { return details; }

    public double getAmount() { return amount.get(); }
    public void setAmount(double value) { amount.set(value); }
    public DoubleProperty amountProperty() { return amount; }

    public boolean getIsIncome() { return isIncome.get(); }
    public void setIsIncome(boolean value) { isIncome.set(value); }
    public BooleanProperty isIncomeProperty() { return isIncome; }

    public String getFormattedAmount() {
        return (isIncome.get() ? "+" : "-") + String.format("%.2f", Math.abs(amount.get()));
    }
} 