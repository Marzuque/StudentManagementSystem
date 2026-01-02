package com.marzuque.sms.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.Optional;

public class SetBalanceDialog {

    public static class Result {

        public final double desiredBalance;
        public final String note;

        public Result(double desiredBalance, String note) {
            this.desiredBalance = desiredBalance;
            this.note = note;
        }
    }

    public static Optional<Result> show(double currentBalance) {
        Dialog<Result> dialog = new Dialog<>();
        dialog.setTitle("Set Current Balance");
        dialog.setHeaderText("Current balance: " + currentBalance);

        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField desiredField = new TextField();
        desiredField.setPromptText("Enter desired balance (number)");
        desiredField.setText(String.valueOf(currentBalance));

        TextField noteField = new TextField();
        noteField.setPromptText("Optional note (why changed)");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        grid.add(new Label("Desired Balance *"), 0, 0);
        grid.add(desiredField, 1, 0);

        grid.add(new Label("Note"), 0, 1);
        grid.add(noteField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveBtn.setDisable(true);

        desiredField.textProperty().addListener((o, a, b) -> saveBtn.setDisable(!isNumber(desiredField.getText())));

        saveBtn.setDisable(!isNumber(desiredField.getText()));

        dialog.setResultConverter(btn -> {
            if (btn == saveType) {
                double desired = Double.parseDouble(desiredField.getText().trim());
                String note = noteField.getText();
                return new Result(desired, note);
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private static boolean isNumber(String s) {
        try {
            if (s == null) {
                return false;
            }
            String t = s.trim();
            if (t.isEmpty()) {
                return false;
            }
            Double.parseDouble(t);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
