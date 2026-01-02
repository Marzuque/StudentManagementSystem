package com.marzuque.sms.model;

public class MonthlyReportRow {

    private final String yearMonth; // "YYYY-MM"
    private final double charges;
    private final double adjustments;
    private final double payments;
    private final double endingBalance;

    public MonthlyReportRow(String yearMonth, double charges, double adjustments, double payments, double endingBalance) {
        this.yearMonth = yearMonth;
        this.charges = charges;
        this.adjustments = adjustments;
        this.payments = payments;
        this.endingBalance = endingBalance;
    }

    public String getYearMonth() {
        return yearMonth;
    }

    public double getCharges() {
        return charges;
    }

    public double getAdjustments() {
        return adjustments;
    }

    public double getPayments() {
        return payments;
    }

    public double getEndingBalance() {
        return endingBalance;
    }

    public double getNetChange() {
        return charges + adjustments - payments;
    }
}