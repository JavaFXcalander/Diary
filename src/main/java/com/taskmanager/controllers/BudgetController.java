package com.taskmanager.controllers;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;

public class BudgetController{

    @FXML private VBox listBox;    // 細項容器
    @FXML private VBox ieColumn;   // 金額容器
    @FXML private TextField incomeField, expensesField, balanceField;

    private final DoubleProperty incomeProp  = new SimpleDoubleProperty(0);
    private final DoubleProperty expenseProp = new SimpleDoubleProperty(0);

    @FXML
    public void initialize(URL url, ResourceBundle rb) {
        addRow();                       // 啟動先給一行
        bindSummary();                  // 繫結統計欄
    }

    /** 建立細項 + 金額的「一行」 */
    private void addRow() {
        TextField detailTf = new TextField();
        TextField amountTf = new TextField();

        listBox.getChildren().add(detailTf);
        ieColumn.getChildren().add(amountTf);

        // 1. 監聽金額變動
        amountTf.textProperty().addListener((o, ov, nv) -> recalc());

        // 2. 在最後一行按 Enter 就加新行
        amountTf.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER &&
                ieColumn.getChildren().indexOf(amountTf) == ieColumn.getChildren().size()-1) {
                addRow();
                ((TextField) listBox.getChildren().get(listBox.getChildren().size()-1)).requestFocus();
            }
        });
    }

    /** 重新計算收入、支出、餘額 */
    private void recalc() {
        double inc = 0, exp = 0;
        for (Node n : ieColumn.getChildren()) {
            String txt = ((TextField) n).getText().trim();
            if (txt.isEmpty()) continue;
            try {
                double v = Double.parseDouble(txt);
                if (v < 0) exp += v; else inc += v;
            } catch (NumberFormatException ignore) {}
        }
        incomeProp.set(inc);
        expenseProp.set(exp);
    }

    /** 將屬性繫結到 TextField，顯示格式化字串 */
    private void bindSummary() {
        // Using NumberFormat for Taiwan locale formatting
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.TAIWAN);
        
        // Format the income, expenses, and balance fields with String.format inside asString
        incomeField.textProperty().bind(incomeProp.asString("%,.0f"));  // Format as number with commas
        expensesField.textProperty().bind(expenseProp.asString("%,.0f"));  // Format as number with commas
        balanceField.textProperty().bind(incomeProp.add(expenseProp)
                                                .asString("%,.0f"));  // Format as number with commas
    }
    
}
