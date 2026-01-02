package com.marzuque.sms.model;

import javafx.beans.property.*;

public class Student {

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty studentId = new SimpleStringProperty();
    private final StringProperty fullName = new SimpleStringProperty();

    private final StringProperty batch = new SimpleStringProperty();
    private final DoubleProperty cgpa = new SimpleDoubleProperty();
    private final DoubleProperty semesterCgpa = new SimpleDoubleProperty();

    // store as 'YYYY-MM-01' text for simplicity
    private final StringProperty billingStartMonth = new SimpleStringProperty();

    // calculated (not stored)
    private final DoubleProperty currentBalance = new SimpleDoubleProperty();

    public Student() {
    }

    public Student(int id, String studentId, String fullName,
            String batch, Double cgpa, Double semesterCgpa, String billingStartMonth) {
        setId(id);
        setStudentId(studentId);
        setFullName(fullName);
        setBatch(batch);
        setCgpa(cgpa == null ? 0.0 : cgpa);
        setSemesterCgpa(semesterCgpa == null ? 0.0 : semesterCgpa);
        setBillingStartMonth(billingStartMonth);
    }

    public int getId() {
        return id.get();
    }

    public void setId(int value) {
        id.set(value);
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public String getStudentId() {
        return studentId.get();
    }

    public void setStudentId(String value) {
        studentId.set(value);
    }

    public StringProperty studentIdProperty() {
        return studentId;
    }

    public String getFullName() {
        return fullName.get();
    }

    public void setFullName(String value) {
        fullName.set(value);
    }

    public StringProperty fullNameProperty() {
        return fullName;
    }

    public String getBatch() {
        return batch.get();
    }

    public void setBatch(String value) {
        batch.set(value);
    }

    public StringProperty batchProperty() {
        return batch;
    }

    public double getCgpa() {
        return cgpa.get();
    }

    public void setCgpa(double value) {
        cgpa.set(value);
    }

    public DoubleProperty cgpaProperty() {
        return cgpa;
    }

    public double getSemesterCgpa() {
        return semesterCgpa.get();
    }

    public void setSemesterCgpa(double value) {
        semesterCgpa.set(value);
    }

    public DoubleProperty semesterCgpaProperty() {
        return semesterCgpa;
    }

    public String getBillingStartMonth() {
        return billingStartMonth.get();
    }

    public void setBillingStartMonth(String value) {
        billingStartMonth.set(value);
    }

    public StringProperty billingStartMonthProperty() {
        return billingStartMonth;
    }

    public double getCurrentBalance() {
        return currentBalance.get();
    }

    public void setCurrentBalance(double value) {
        currentBalance.set(value);
    }

    public DoubleProperty currentBalanceProperty() {
        return currentBalance;
    }
}