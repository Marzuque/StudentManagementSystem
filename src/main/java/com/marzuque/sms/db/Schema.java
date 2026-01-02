package com.marzuque.sms.db;

import java.sql.Connection;
import java.sql.Statement;

public class Schema {

    public static void init() {
        try (Connection c = Db.connect(); Statement st = c.createStatement()) {

            System.out.println(">>> Running Schema.init()");

            st.execute("""
                CREATE TABLE IF NOT EXISTS students (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    student_id TEXT UNIQUE NOT NULL,
                    full_name TEXT NOT NULL,
                    batch TEXT NOT NULL,
                    cgpa REAL,
                    semester_cgpa REAL,
                    billing_start_month TEXT NOT NULL
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS tuition_charges (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    student_id INTEGER NOT NULL,
                    charge_month TEXT NOT NULL,
                    amount REAL NOT NULL,
                    charge_type TEXT NOT NULL DEFAULT 'TUITION',
                    note TEXT,
                    UNIQUE(student_id, charge_month, charge_type),
                    FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS payments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    student_id INTEGER NOT NULL,
                    payment_date TEXT NOT NULL,
                    amount REAL NOT NULL,
                    method TEXT,
                    reference_no TEXT,
                    note TEXT,
                    FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS account_adjustments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    student_id INTEGER NOT NULL,
                    adj_date TEXT NOT NULL,
                    amount REAL NOT NULL,
                    note TEXT,
                    FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE
                );
            """);

            System.out.println(">>> Schema.init() done");

        } catch (Exception e) {
            throw new RuntimeException("DB init failed", e);
        }
    }
}
