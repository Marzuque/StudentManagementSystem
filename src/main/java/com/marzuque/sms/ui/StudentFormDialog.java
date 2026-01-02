package com.marzuque.sms.ui;

import com.marzuque.sms.model.Student;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.time.YearMonth;
import java.util.Optional;

public class StudentFormDialog {

    public static Optional<Student> showAddDialog() {
        // default billing start month = current month
        String defaultMonth = YearMonth.now().toString() + "-01";
        return showDialog("Add Student", null, defaultMonth);
    }

    public static Optional<Student> showEditDialog(Student existing) {
        return showDialog("Edit Student", existing, existing.getBillingStartMonth());
    }

    private static Optional<Student> showDialog(String title, Student existing, String defaultBillingStartMonth) {
        Dialog<Student> dialog = new Dialog<>();
        dialog.setTitle(title);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField studentIdField = new TextField();
        TextField fullNameField = new TextField();
        TextField batchField = new TextField();

        TextField cgpaField = new TextField();
        TextField semCgpaField = new TextField();
        TextField billingStartMonthField = new TextField();

        studentIdField.setPromptText("e.g., 2025-1234");
        fullNameField.setPromptText("e.g., Ayesha Rahman");
        batchField.setPromptText("e.g., 50th");

        cgpaField.setPromptText("e.g., 3.75");
        semCgpaField.setPromptText("e.g., 3.90");
        billingStartMonthField.setPromptText("YYYY-MM-01");

        if (existing != null) {
            studentIdField.setText(n(existing.getStudentId()));
            fullNameField.setText(n(existing.getFullName()));
            batchField.setText(n(existing.getBatch()));
            cgpaField.setText(String.valueOf(existing.getCgpa()));
            semCgpaField.setText(String.valueOf(existing.getSemesterCgpa()));
            billingStartMonthField.setText(n(existing.getBillingStartMonth()));
        } else {
            billingStartMonthField.setText(defaultBillingStartMonth);
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        grid.add(new Label("Student ID *"), 0, 0);
        grid.add(studentIdField, 1, 0);
        grid.add(new Label("Full Name *"), 0, 1);
        grid.add(fullNameField, 1, 1);
        grid.add(new Label("Batch *"), 0, 2);
        grid.add(batchField, 1, 2);

        grid.add(new Label("CGPA"), 0, 3);
        grid.add(cgpaField, 1, 3);
        grid.add(new Label("Semester CGPA"), 0, 4);
        grid.add(semCgpaField, 1, 4);

        grid.add(new Label("Billing Start Month *"), 0, 5);
        grid.add(billingStartMonthField, 1, 5);

        dialog.getDialogPane().setContent(grid);

        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveBtn.setDisable(true);

        Runnable validate = () -> {
            boolean requiredOk = !studentIdField.getText().trim().isEmpty()
                    && !fullNameField.getText().trim().isEmpty()
                    && !batchField.getText().trim().isEmpty()
                    && isValidMonth(billingStartMonthField.getText().trim());
            saveBtn.setDisable(!requiredOk);
        };

        studentIdField.textProperty().addListener((o, a, b) -> validate.run());
        fullNameField.textProperty().addListener((o, a, b) -> validate.run());
        batchField.textProperty().addListener((o, a, b) -> validate.run());
        billingStartMonthField.textProperty().addListener((o, a, b) -> validate.run());
        validate.run();

        dialog.setResultConverter(button -> {
            if (button == saveButtonType) {
                Student s = (existing == null) ? new Student() : cloneStudent(existing);

                s.setStudentId(studentIdField.getText().trim());
                s.setFullName(fullNameField.getText().trim());
                s.setBatch(batchField.getText().trim());

                s.setCgpa(parseDoubleOrZero(cgpaField.getText()));
                s.setSemesterCgpa(parseDoubleOrZero(semCgpaField.getText()));

                s.setBillingStartMonth(billingStartMonthField.getText().trim());
                return s;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private static Student cloneStudent(Student e) {
        return new Student(e.getId(), e.getStudentId(), e.getFullName(),
                e.getBatch(), e.getCgpa(), e.getSemesterCgpa(), e.getBillingStartMonth());
    }

    private static boolean isValidMonth(String text) {
        // expects 'YYYY-MM-01'
        try {
            if (text.length() != 10) {
                return false;
            }
            YearMonth.parse(text.substring(0, 7));
            return text.endsWith("-01");
        } catch (Exception e) {
            return false;
        }
    }

    private static double parseDoubleOrZero(String t) {
        try {
            String s = t == null ? "" : t.trim();
            if (s.isEmpty()) {
                return 0.0;
            }
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static String n(String v) {
        return v == null ? "" : v;
    }
}
