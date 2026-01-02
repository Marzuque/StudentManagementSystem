package com.marzuque.sms;

import com.marzuque.sms.db.Schema;
import com.marzuque.sms.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        Schema.init(); // <-- MUST be before MainView loads students

        Scene scene = new Scene(new MainView().create(), 1100, 650);
        stage.setTitle("Student Management System");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
