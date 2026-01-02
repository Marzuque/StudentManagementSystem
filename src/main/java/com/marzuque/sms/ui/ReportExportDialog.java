package com.marzuque.sms.ui;

import com.marzuque.sms.model.MonthlyReportRow;
import com.marzuque.sms.model.Student;
import javafx.scene.control.Alert;
import javafx.scene.control.Control;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class ReportExportDialog {

    public static void exportStudent12Months(Control ownerControl, Student student, List<MonthlyReportRow> rows) {
        String defaultName = "student-report-" + safe(student.getStudentId()) + "-" + YearMonth.now() + ".xlsx";
        File out = choosePath(ownerControl, defaultName);
        if (out == null) {
            return;
        }

        try {
            writeStudentReport(out, student, rows);
            info("Report saved", "Saved to:\n" + out.getAbsolutePath());
        } catch (Exception e) {
            error("Report failed", e.getMessage());
        }
    }

    public static void exportBatch12Months(Control ownerControl, String batch, List<MonthlyReportRow> rows) {
        String defaultName = "batch-report-" + safe(batch) + "-" + YearMonth.now() + ".xlsx";
        File out = choosePath(ownerControl, defaultName);
        if (out == null) {
            return;
        }

        try {
            writeBatchReport(out, batch, rows);
            info("Report saved", "Saved to:\n" + out.getAbsolutePath());
        } catch (Exception e) {
            error("Report failed", e.getMessage());
        }
    }

    private static File choosePath(Control ctrl, String initialName) {
        Window owner = ctrl.getScene() != null ? ctrl.getScene().getWindow() : null;

        FileChooser fc = new FileChooser();
        fc.setTitle("Save 12-Month Report");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));
        fc.setInitialFileName(initialName);

        File file = fc.showSaveDialog(owner);
        if (file == null) {
            return null;
        }

        if (!file.getName().toLowerCase().endsWith(".xlsx")) {
            file = new File(file.getParentFile(), file.getName() + ".xlsx");
        }
        return file;
    }

    private static void writeStudentReport(File file, Student s, List<MonthlyReportRow> rows) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("12-Month Report");

            int r = 0;

            Row t1 = sheet.createRow(r++);
            t1.createCell(0).setCellValue("Student 12-Month Account Report");

            Row meta1 = sheet.createRow(r++);
            meta1.createCell(0).setCellValue("Generated");
            meta1.createCell(1).setCellValue(LocalDate.now().toString());

            Row meta2 = sheet.createRow(r++);
            meta2.createCell(0).setCellValue("Student ID");
            meta2.createCell(1).setCellValue(n(s.getStudentId()));

            Row meta3 = sheet.createRow(r++);
            meta3.createCell(0).setCellValue("Name");
            meta3.createCell(1).setCellValue(n(s.getFullName()));

            Row meta4 = sheet.createRow(r++);
            meta4.createCell(0).setCellValue("Batch");
            meta4.createCell(1).setCellValue(n(s.getBatch()));

            r++; // blank row

            Row h = sheet.createRow(r++);
            h.createCell(0).setCellValue("Month");
            h.createCell(1).setCellValue("Charges");
            h.createCell(2).setCellValue("Adjustments");
            h.createCell(3).setCellValue("Payments");
            h.createCell(4).setCellValue("Net Change");
            h.createCell(5).setCellValue("Ending Balance");

            for (MonthlyReportRow row : rows) {
                Row rr = sheet.createRow(r++);
                rr.createCell(0).setCellValue(row.getYearMonth());
                rr.createCell(1).setCellValue(row.getCharges());
                rr.createCell(2).setCellValue(row.getAdjustments());
                rr.createCell(3).setCellValue(row.getPayments());
                rr.createCell(4).setCellValue(row.getNetChange());
                rr.createCell(5).setCellValue(row.getEndingBalance());
            }

            for (int i = 0; i <= 5; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    private static void writeBatchReport(File file, String batch, List<MonthlyReportRow> rows) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("12-Month Report");

            int r = 0;

            Row t1 = sheet.createRow(r++);
            t1.createCell(0).setCellValue("Batch 12-Month Account Report");

            Row meta1 = sheet.createRow(r++);
            meta1.createCell(0).setCellValue("Generated");
            meta1.createCell(1).setCellValue(LocalDate.now().toString());

            Row meta2 = sheet.createRow(r++);
            meta2.createCell(0).setCellValue("Batch");
            meta2.createCell(1).setCellValue(n(batch));

            r++; // blank row

            Row h = sheet.createRow(r++);
            h.createCell(0).setCellValue("Month");
            h.createCell(1).setCellValue("Charges");
            h.createCell(2).setCellValue("Adjustments");
            h.createCell(3).setCellValue("Payments");
            h.createCell(4).setCellValue("Net Change");
            h.createCell(5).setCellValue("Ending Balance");

            for (MonthlyReportRow row : rows) {
                Row rr = sheet.createRow(r++);
                rr.createCell(0).setCellValue(row.getYearMonth());
                rr.createCell(1).setCellValue(row.getCharges());
                rr.createCell(2).setCellValue(row.getAdjustments());
                rr.createCell(3).setCellValue(row.getPayments());
                rr.createCell(4).setCellValue(row.getNetChange());
                rr.createCell(5).setCellValue(row.getEndingBalance());
            }

            for (int i = 0; i <= 5; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    private static String n(String v) {
        return v == null ? "" : v;
    }

    private static String safe(String v) {
        if (v == null) {
            return "unknown";
        }
        return v.trim().replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    }

    private static void info(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private static void error(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg == null ? "Unknown error" : msg);
        a.showAndWait();
    }
}
