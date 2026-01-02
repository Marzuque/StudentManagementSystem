package com.marzuque.sms.ui;

import com.marzuque.sms.dao.StudentDao;
import com.marzuque.sms.model.Student;
import javafx.scene.control.Alert;
import javafx.scene.control.Control;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class ExcelImportDialog {

    /**
     * Expected .xlsx format (First sheet): Column A: Student ID (required)
     * Column B: Full Name (required) Column C: Batch (required) e.g., "50th"
     * Column D: CGPA (optional) e.g., 3.75 Column E: Semester CGPA (optional)
     * e.g., 3.90 Column F: Billing Start Month (required-ish) as 'YYYY-MM-01'
     * If blank/invalid, we default to current month 'YYYY-MM-01'
     *
     * First row can be headers. We try to detect and skip it.
     */
    public static void importStudents(Control anyControlInScene, StudentDao dao, Runnable onDoneReload) {
        Window owner = anyControlInScene.getScene() != null ? anyControlInScene.getScene().getWindow() : null;

        FileChooser fc = new FileChooser();
        fc.setTitle("Import Students from Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));

        File file = fc.showOpenDialog(owner);
        if (file == null) {
            return;
        }

        try {
            List<Student> students = readStudentsFromXlsx(file);

            if (students.isEmpty()) {
                info("Import", "No valid rows found.\n\nRequired columns: Student ID, Full Name, Batch.");
                return;
            }

            // Remember this file as the current Excel file for Save Excel behavior
            ExcelFileState.setCurrentExcelFile(file);

            StudentDao.ImportResult result = dao.upsertMany(students);
            onDoneReload.run();

            info("Import complete",
                    "Rows prepared: " + result.processed + "\n"
                    + "Failed while preparing rows: " + result.failed + "\n\n"
                    + "Existing Student IDs are updated (upsert).");

        } catch (Exception e) {
            error("Import failed", e.getMessage());
        }
    }

    private static List<Student> readStudentsFromXlsx(File file) throws Exception {
        List<Student> out = new ArrayList<>();
        DataFormatter fmt = new DataFormatter();

        try (FileInputStream fis = new FileInputStream(file); Workbook wb = new XSSFWorkbook(fis)) {

            Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            if (sheet == null) {
                return out;
            }

            int firstRow = sheet.getFirstRowNum();
            int lastRow = sheet.getLastRowNum();
            if (lastRow < firstRow) {
                return out;
            }

            int start = firstRow;

            // Header detection heuristic: if first row includes "student" / "name" / "batch"
            Row r0 = sheet.getRow(firstRow);
            if (r0 != null) {
                String a = cell(fmt, r0, 0).toLowerCase();
                String b = cell(fmt, r0, 1).toLowerCase();
                String c = cell(fmt, r0, 2).toLowerCase();
                String f = cell(fmt, r0, 5).toLowerCase();
                if (a.contains("student") || a.contains("id")
                        || b.contains("name")
                        || c.contains("batch")
                        || f.contains("billing")) {
                    start = firstRow + 1;
                }
            }

            for (int i = start; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String studentId = cell(fmt, row, 0).trim();
                String fullName = cell(fmt, row, 1).trim();
                String batch = cell(fmt, row, 2).trim();

                String cgpaText = cell(fmt, row, 3).trim();
                String semCgpaText = cell(fmt, row, 4).trim();
                String billingText = cell(fmt, row, 5).trim();

                // required: studentId + fullName + batch
                if (studentId.isEmpty() || fullName.isEmpty() || batch.isEmpty()) {
                    continue;
                }

                Student s = new Student();
                s.setStudentId(studentId);
                s.setFullName(fullName);
                s.setBatch(batch);

                s.setCgpa(parseDoubleOrZero(cgpaText));
                s.setSemesterCgpa(parseDoubleOrZero(semCgpaText));

                String billingStart = normalizeBillingStartMonth(billingText);
                s.setBillingStartMonth(billingStart);

                out.add(s);
            }
        }

        return out;
    }

    private static String normalizeBillingStartMonth(String billingText) {
        // desired: 'YYYY-MM-01'
        // if blank/invalid -> current month
        String fallback = YearMonth.now().toString() + "-01";
        if (billingText == null) {
            return fallback;
        }

        String t = billingText.trim();
        if (t.isEmpty()) {
            return fallback;
        }

        // Accept:
        // 1) 'YYYY-MM-01'
        // 2) 'YYYY-MM'   -> append '-01'
        // 3) 'YYYY/MM/01' or 'YYYY/MM' -> normalize to '-'
        t = t.replace('/', '-');

        try {
            if (t.length() == 7) {
                // 'YYYY-MM'
                YearMonth.parse(t);
                return t + "-01";
            }
            if (t.length() == 10) {
                // 'YYYY-MM-01' expected
                YearMonth.parse(t.substring(0, 7));
                if (t.endsWith("-01")) {
                    return t;
                }
                // if they put another day, still normalize to first day
                return t.substring(0, 7) + "-01";
            }

            // Sometimes Excel numeric date gets formatted weirdly; attempt best-effort:
            // If it starts with YYYY-MM, take that
            if (t.length() >= 7) {
                String ym = t.substring(0, 7);
                YearMonth.parse(ym);
                return ym + "-01";
            }

            return fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String cell(DataFormatter fmt, Row row, int col) {
        org.apache.poi.ss.usermodel.Cell c
                = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (c == null) {
            return "";
        }
        return fmt.formatCellValue(c);
    }

    private static double parseDoubleOrZero(String t) {
        try {
            if (t == null) {
                return 0.0;
            }
            String s = t.trim();
            if (s.isEmpty()) {
                return 0.0;
            }
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0.0;
        }
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
