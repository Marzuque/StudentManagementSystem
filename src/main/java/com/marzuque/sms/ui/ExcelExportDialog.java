package com.marzuque.sms.ui;

import com.marzuque.sms.model.Student;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class ExcelExportDialog {

    /**
     * Save to the same file that was last imported/saved. If none, falls back
     * to Save As.
     */
    public static void saveToCurrentOrSaveAs(Control anyControlInScene, List<Student> students) {
        File current = ExcelFileState.getCurrentExcelFile();
        if (current == null) {
            saveAs(anyControlInScene, students);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Save Excel");
        confirm.setHeaderText("Overwrite the current Excel file?");
        confirm.setContentText(current.getAbsolutePath());

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    writeStudentsXlsx(current, students);
                    info("Saved", "Saved to:\n" + current.getAbsolutePath());
                } catch (Exception e) {
                    error("Save failed", e.getMessage());
                }
            }
        });
    }

    /**
     * Always asks for a path.
     */
    public static void saveAs(Control anyControlInScene, List<Student> students) {
        Window owner = anyControlInScene.getScene() != null ? anyControlInScene.getScene().getWindow() : null;

        FileChooser fc = new FileChooser();
        fc.setTitle("Save Students to Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));

        File current = ExcelFileState.getCurrentExcelFile();
        if (current != null && current.getParentFile() != null) {
            fc.setInitialDirectory(current.getParentFile());
            fc.setInitialFileName(current.getName());
        } else {
            fc.setInitialFileName("students.xlsx");
        }

        File file = fc.showSaveDialog(owner);
        if (file == null) {
            return;
        }

        if (!file.getName().toLowerCase().endsWith(".xlsx")) {
            file = new File(file.getParentFile(), file.getName() + ".xlsx");
        }

        try {
            writeStudentsXlsx(file, students);
            ExcelFileState.setCurrentExcelFile(file); // becomes current for Save Excel
            info("Saved", "Saved to:\n" + file.getAbsolutePath());
        } catch (Exception e) {
            error("Save failed", e.getMessage());
        }
    }

    /**
     * Export format (round-trip compatible with import): A: Student ID B: Full
     * Name C: Batch D: CGPA E: Semester CGPA F: Billing Start Month
     * (YYYY-MM-01) G: Current Balance (optional, helpful for admins)
     */
    private static void writeStudentsXlsx(File file, List<Student> students) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Students");

            // Header
            Row h = sheet.createRow(0);
            h.createCell(0).setCellValue("Student ID");
            h.createCell(1).setCellValue("Full Name");
            h.createCell(2).setCellValue("Batch");
            h.createCell(3).setCellValue("CGPA");
            h.createCell(4).setCellValue("Semester CGPA");
            h.createCell(5).setCellValue("Billing Start Month");
            h.createCell(6).setCellValue("Current Balance");

            // Rows
            int r = 1;
            for (Student s : students) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(n(s.getStudentId()));
                row.createCell(1).setCellValue(n(s.getFullName()));
                row.createCell(2).setCellValue(n(s.getBatch()));
                row.createCell(3).setCellValue(s.getCgpa());
                row.createCell(4).setCellValue(s.getSemesterCgpa());
                row.createCell(5).setCellValue(n(s.getBillingStartMonth()));
                row.createCell(6).setCellValue(s.getCurrentBalance());
            }

            for (int i = 0; i < 7; i++) {
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