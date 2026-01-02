package com.marzuque.sms.model;

import javafx.beans.property.*;

public class AccountAdjustment {

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final IntegerProperty studentDbId = new SimpleIntegerProperty();
    private final StringProperty adjDate = new SimpleStringProperty();
    private final DoubleProperty amount = new SimpleDoubleProperty();
    private final StringProperty note = new SimpleStringProperty();

    public AccountAdjustment() {
    }

    public AccountAdjustment(int id, int studentDbId, String adjDate, double amount, String note) {
        setId(id);
        setStudentDbId(studentDbId);
        setAdjDate(adjDate);
        setAmount(amount);
        setNote(note);
    }

    public int getId() {
        return id.get();
    }

    public void setId(int v) {
        id.set(v);
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public int getStudentDbId() {
        return studentDbId.get();
    }

    public void setStudentDbId(int v) {
        studentDbId.set(v);
    }

    public IntegerProperty studentDbIdProperty() {
        return studentDbId;
    }

    public String getAdjDate() {
        return adjDate.get();
    }

    public void setAdjDate(String v) {
        adjDate.set(v);
    }

    public StringProperty adjDateProperty() {
        return adjDate;
    }

    public double getAmount() {
        return amount.get();
    }

    public void setAmount(double v) {
        amount.set(v);
    }

    public DoubleProperty amountProperty() {
        return amount;
    }

    public String getNote() {
        return note.get();
    }

    public void setNote(String v) {
        note.set(v);
    }

    public StringProperty noteProperty() {
        return note;
    }
}
