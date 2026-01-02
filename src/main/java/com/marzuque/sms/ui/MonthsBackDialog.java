package com.marzuque.sms.ui;

import javafx.scene.control.TextInputDialog;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.util.Optional;

public class MonthsBackDialog {

    public static Optional<Integer> ask(String title, int defaultMonthsBack) {
        TextInputDialog d = new TextInputDialog(String.valueOf(defaultMonthsBack));
        d.setTitle(title);
        d.setHeaderText("Enter X (months before current month)");
        d.setContentText("Example: If it is December and X=5, report will include July–December.");

        // Make sure it doesn't hide behind the app
        Window owner = d.getDialogPane().getScene() != null ? d.getDialogPane().getScene().getWindow() : null;
        if (owner != null) {
            d.initModality(Modality.APPLICATION_MODAL);
        } else {
            d.initModality(Modality.APPLICATION_MODAL);
        }

        return d.showAndWait().map(s -> {
            try {
                int v = Integer.parseInt(s.trim());
                return Math.max(0, v);
            } catch (Exception e) {
                return defaultMonthsBack;
            }
        });
    }
}
