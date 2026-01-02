package com.marzuque.sms.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;

public class Db {

    private static final String DB_RELATIVE_PATH = "data/sms.db";

    public static Connection connect() {
        try {
            // Ensure folder exists
            new File("data").mkdirs();

            String absPath = new File(DB_RELATIVE_PATH).getAbsolutePath();
            System.out.println(">>> SQLite DB path: " + absPath);

            String url = "jdbc:sqlite:" + absPath;
            return DriverManager.getConnection(url);

        } catch (Exception e) {
            throw new RuntimeException("Failed to connect DB", e);
        }
    }
}
