package com.marzuque.sms.dao;

import com.marzuque.sms.db.Db;
import com.marzuque.sms.model.Student;
import com.marzuque.sms.model.AccountAdjustment;
import com.marzuque.sms.model.MonthlyReportRow;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class StudentDao {

    // ---------- Students CRUD ----------
    public List<Student> findAll() {
        String sql = """
            SELECT id, student_id, full_name, batch, cgpa, semester_cgpa, billing_start_month
            FROM students
            ORDER BY batch ASC, student_id ASC
            """;

        List<Student> list = new ArrayList<>();

        try (Connection c = Db.connect(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Student s = new Student(
                        rs.getInt("id"),
                        rs.getString("student_id"),
                        rs.getString("full_name"),
                        rs.getString("batch"),
                        (Double) rs.getObject("cgpa"),
                        (Double) rs.getObject("semester_cgpa"),
                        rs.getString("billing_start_month")
                );
                list.add(s);
            }
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load students", e);
        }
    }

    public Student insert(Student s) {
        String sql = """
            INSERT INTO students (student_id, full_name, batch, cgpa, semester_cgpa, billing_start_month)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection c = Db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, s.getStudentId());
            ps.setString(2, s.getFullName());
            ps.setString(3, s.getBatch());
            ps.setObject(4, s.getCgpa());
            ps.setObject(5, s.getSemesterCgpa());
            ps.setString(6, s.getBillingStartMonth());

            ps.executeUpdate();

            try (PreparedStatement idStmt = c.prepareStatement("SELECT last_insert_rowid() AS id"); ResultSet rs = idStmt.executeQuery()) {
                if (rs.next()) {
                    s.setId(rs.getInt("id"));
                }
            }

            return s;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert student", e);
        }
    }

    public void update(Student s) {
        String sql = """
            UPDATE students
            SET student_id = ?, full_name = ?, batch = ?, cgpa = ?, semester_cgpa = ?, billing_start_month = ?
            WHERE id = ?
            """;

        try (Connection c = Db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, s.getStudentId());
            ps.setString(2, s.getFullName());
            ps.setString(3, s.getBatch());
            ps.setObject(4, s.getCgpa());
            ps.setObject(5, s.getSemesterCgpa());
            ps.setString(6, s.getBillingStartMonth());
            ps.setInt(7, s.getId());

            int changed = ps.executeUpdate();
            if (changed == 0) {
                throw new RuntimeException("Student not found.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update student", e);
        }
    }

    public void deleteById(int id) {
        String sql = "DELETE FROM students WHERE id = ?";

        try (Connection c = Db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete student", e);
        }
    }

    // ---------- Tuition: generate monthly charges (flat fee) ----------
    public void generateChargesUpToMonthForAllStudents(double monthlyFee, YearMonth upTo) {
        List<Student> students = findAll();
        for (Student s : students) {
            generateChargesUpToMonthForStudent(s.getId(), s.getBillingStartMonth(), monthlyFee, upTo);
        }
    }

    private void generateChargesUpToMonthForStudent(int studentDbId, String billingStartMonthText,
            double monthlyFee, YearMonth upTo) {
        YearMonth start = YearMonth.parse(billingStartMonthText.substring(0, 7)); // "YYYY-MM"
        YearMonth cur = start;

        while (!cur.isAfter(upTo)) {
            String chargeMonth = cur.toString() + "-01"; // 'YYYY-MM-01'
            insertChargeIfMissing(studentDbId, chargeMonth, monthlyFee);
            cur = cur.plusMonths(1);
        }
    }

    private void insertChargeIfMissing(int studentDbId, String chargeMonth, double amount) {
        String sql = """
            INSERT OR IGNORE INTO tuition_charges (student_id, charge_month, amount, charge_type, note)
            VALUES (?, ?, ?, 'TUITION', NULL)
            """;

        try (Connection c = Db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, studentDbId);
            ps.setString(2, chargeMonth);
            ps.setDouble(3, amount);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to generate charge", e);
        }
    }

    // ---------- Balance: as-of this month ----------
    /**
     * Returns balance for each student db-id as of end of month 'asOf'.
     */
    public Map<Integer, Double> getBalancesAsOf(YearMonth asOf) {
        String asOfMonth = asOf.toString() + "-01";
        LocalDate endOfMonth = asOf.atEndOfMonth();
        String endDate = endOfMonth.toString(); // 'YYYY-MM-DD'

        String sql = """
    SELECT s.id AS student_id,
           COALESCE(ch.total_charges, 0)
         + COALESCE(adj.total_adjustments, 0)
         - COALESCE(p.total_payments, 0) AS balance
    FROM students s
    LEFT JOIN (
        SELECT student_id, SUM(amount) AS total_charges
        FROM tuition_charges
        WHERE charge_month <= ?
        GROUP BY student_id
    ) ch ON ch.student_id = s.id
    LEFT JOIN (
        SELECT student_id, SUM(amount) AS total_adjustments
        FROM account_adjustments
        WHERE adj_date <= ?
        GROUP BY student_id
    ) adj ON adj.student_id = s.id
    LEFT JOIN (
        SELECT student_id, SUM(amount) AS total_payments
        FROM payments
        WHERE payment_date <= ?
        GROUP BY student_id
    ) p ON p.student_id = s.id
    """;


        Map<Integer, Double> out = new HashMap<>();

        try (Connection c = Db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, asOfMonth);
            ps.setString(2, endDate);
            ps.setString(3, endDate);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.put(rs.getInt("student_id"), rs.getDouble("balance"));
                }
            }

            return out;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to compute balances", e);
        }
    }

    // ---------- Excel import helper (upsert by student_id) ----------
    public ImportResult upsertMany(List<Student> students) {
        String sql = """
            INSERT INTO students (student_id, full_name, batch, cgpa, semester_cgpa, billing_start_month)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(student_id) DO UPDATE SET
                full_name = excluded.full_name,
                batch = excluded.batch,
                cgpa = excluded.cgpa,
                semester_cgpa = excluded.semester_cgpa,
                billing_start_month = excluded.billing_start_month
            """;

        int processed = 0;
        int failed = 0;

        try (Connection c = Db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {

            c.setAutoCommit(false);
            try {
                for (Student s : students) {
                    try {
                        ps.setString(1, s.getStudentId());
                        ps.setString(2, s.getFullName());
                        ps.setString(3, s.getBatch());
                        ps.setObject(4, s.getCgpa());
                        ps.setObject(5, s.getSemesterCgpa());
                        ps.setString(6, s.getBillingStartMonth());
                        ps.addBatch();
                        processed++;
                    } catch (Exception ex) {
                        failed++;
                    }
                }
                ps.executeBatch();
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }

            return new ImportResult(processed, failed);

        } catch (Exception e) {
            throw new RuntimeException("Failed to import students", e);
        }
    }

    public static class ImportResult {

        public final int processed;
        public final int failed;

        public ImportResult(int processed, int failed) {
            this.processed = processed;
            this.failed = failed;
        }
    }
    // List adjustments for a student

    public java.util.List<AccountAdjustment> listAdjustments(int studentDbId) {
        String sql = """
        SELECT id, student_id, adj_date, amount, note
        FROM account_adjustments
        WHERE student_id = ?
        ORDER BY adj_date DESC, id DESC
        """;

        java.util.List<AccountAdjustment> list = new java.util.ArrayList<>();

        try (java.sql.Connection c = Db.connect(); java.sql.PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, studentDbId);

            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new AccountAdjustment(
                            rs.getInt("id"),
                            rs.getInt("student_id"),
                            rs.getString("adj_date"),
                            rs.getDouble("amount"),
                            rs.getString("note")
                    ));
                }
            }

            return list;

        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to load adjustments", e);
        }
    }

// Add a raw adjustment (+ or -)
    public void addAdjustment(int studentDbId, LocalDate date, double amount, String note) {
        String sql = """
        INSERT INTO account_adjustments (student_id, adj_date, amount, note)
        VALUES (?, ?, ?, ?)
        """;

        try (java.sql.Connection c = Db.connect(); java.sql.PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, studentDbId);
            ps.setString(2, date.toString()); // YYYY-MM-DD
            ps.setDouble(3, amount);
            ps.setString(4, (note == null || note.trim().isEmpty()) ? null : note.trim());
            ps.executeUpdate();

        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to add adjustment", e);
        }
    }

    /**
     * Sets balance to a desired value (as-of end of current month) by inserting
     * one adjustment = (desired - current). This is your “manual edit current
     * balance” feature.
     */
    public void setBalanceAsOfCurrentMonth(int studentDbId, double desiredBalance, String note) {
        YearMonth asOf = YearMonth.now();
        double current = getBalancesAsOf(asOf).getOrDefault(studentDbId, 0.0);
        double delta = desiredBalance - current;

        // if already equal (within a tiny tolerance), do nothing
        if (Math.abs(delta) < 0.000001) {
            return;
        }

        String finalNote = (note == null ? "" : note.trim());
        if (finalNote.isEmpty()) {
            finalNote = "Set balance to " + desiredBalance;
        }

        addAdjustment(studentDbId, LocalDate.now(), delta, finalNote);
    }
    
    // -------------------- REPORTS (LAST 12 MONTHS) --------------------
    public java.util.List<com.marzuque.sms.model.MonthlyReportRow> buildStudentReportPreviousMonths(
            int studentDbId, YearMonth endMonthInclusive, int monthsBack) {

        if (monthsBack < 0) {
            monthsBack = 0;
        }

        YearMonth startMonth = endMonthInclusive.minusMonths(monthsBack);

        // Starting balance = end of month BEFORE startMonth
        double running = getStudentBalanceAsOf(studentDbId, startMonth.minusMonths(1));

        java.util.Map<String, Double> charges = sumChargesByYmForStudent(studentDbId, startMonth, endMonthInclusive);
        java.util.Map<String, Double> adjs = sumAdjustmentsByYmForStudent(studentDbId, startMonth, endMonthInclusive);
        java.util.Map<String, Double> pays = sumPaymentsByYmForStudent(studentDbId, startMonth, endMonthInclusive);

        java.util.List<com.marzuque.sms.model.MonthlyReportRow> rows = new java.util.ArrayList<>();
        YearMonth cur = startMonth;

        while (!cur.isAfter(endMonthInclusive)) {
            String ym = cur.toString(); // "YYYY-MM"
            double c = charges.getOrDefault(ym, 0.0);
            double a = adjs.getOrDefault(ym, 0.0);
            double p = pays.getOrDefault(ym, 0.0);

            running = running + c + a - p;
            rows.add(new com.marzuque.sms.model.MonthlyReportRow(ym, c, a, p, running));

            cur = cur.plusMonths(1);
        }

        return rows;
    }

    public java.util.List<com.marzuque.sms.model.MonthlyReportRow> buildBatchReportPreviousMonths(
            String batch, YearMonth endMonthInclusive, int monthsBack) {

        if (monthsBack < 0) {
            monthsBack = 0;
        }

        YearMonth startMonth = endMonthInclusive.minusMonths(monthsBack);

        // Starting balance = end of month BEFORE startMonth
        double running = getBatchBalanceAsOf(batch, startMonth.minusMonths(1));

        java.util.Map<String, Double> charges = sumChargesByYmForBatch(batch, startMonth, endMonthInclusive);
        java.util.Map<String, Double> adjs = sumAdjustmentsByYmForBatch(batch, startMonth, endMonthInclusive);
        java.util.Map<String, Double> pays = sumPaymentsByYmForBatch(batch, startMonth, endMonthInclusive);

        java.util.List<com.marzuque.sms.model.MonthlyReportRow> rows = new java.util.ArrayList<>();
        YearMonth cur = startMonth;

        while (!cur.isAfter(endMonthInclusive)) {
            String ym = cur.toString();
            double c = charges.getOrDefault(ym, 0.0);
            double a = adjs.getOrDefault(ym, 0.0);
            double p = pays.getOrDefault(ym, 0.0);

            running = running + c + a - p;
            rows.add(new com.marzuque.sms.model.MonthlyReportRow(ym, c, a, p, running));

            cur = cur.plusMonths(1);
        }

        return rows;
    }

// ---- balances as-of (single student / batch) ----
    public double getStudentBalanceAsOf(int studentDbId, YearMonth asOf) {
        String asOfMonth = asOf.toString() + "-01";
        String endDate = asOf.atEndOfMonth().toString();

        String sql = """
        SELECT
           COALESCE(ch.total_charges, 0)
         + COALESCE(adj.total_adjustments, 0)
         - COALESCE(p.total_payments, 0) AS balance
        FROM students s
        LEFT JOIN (
            SELECT student_id, SUM(amount) AS total_charges
            FROM tuition_charges
            WHERE charge_month <= ?
            GROUP BY student_id
        ) ch ON ch.student_id = s.id
        LEFT JOIN (
            SELECT student_id, SUM(amount) AS total_adjustments
            FROM account_adjustments
            WHERE adj_date <= ?
            GROUP BY student_id
        ) adj ON adj.student_id = s.id
        LEFT JOIN (
            SELECT student_id, SUM(amount) AS total_payments
            FROM payments
            WHERE payment_date <= ?
            GROUP BY student_id
        ) p ON p.student_id = s.id
        WHERE s.id = ?
        """;

        try (java.sql.Connection c = Db.connect(); java.sql.PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, asOfMonth);
            ps.setString(2, endDate);
            ps.setString(3, endDate);
            ps.setInt(4, studentDbId);

            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("balance");
                }
                return 0.0;
            }

        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to compute student balance", e);
        }
    }

    public double getBatchBalanceAsOf(String batch, YearMonth asOf) {
        String asOfMonth = asOf.toString() + "-01";
        String endDate = asOf.atEndOfMonth().toString();

        String sql = """
        SELECT
           COALESCE(ch.total_charges, 0)
         + COALESCE(adj.total_adjustments, 0)
         - COALESCE(p.total_payments, 0) AS balance
        FROM (
            SELECT id
            FROM students
            WHERE batch = ?
        ) s
        LEFT JOIN (
            SELECT s2.id AS student_id, SUM(tc.amount) AS total_charges
            FROM students s2
            JOIN tuition_charges tc ON tc.student_id = s2.id
            WHERE s2.batch = ? AND tc.charge_month <= ?
            GROUP BY s2.id
        ) ch ON ch.student_id = s.id
        LEFT JOIN (
            SELECT s2.id AS student_id, SUM(a.amount) AS total_adjustments
            FROM students s2
            JOIN account_adjustments a ON a.student_id = s2.id
            WHERE s2.batch = ? AND a.adj_date <= ?
            GROUP BY s2.id
        ) adj ON adj.student_id = s.id
        LEFT JOIN (
            SELECT s2.id AS student_id, SUM(p.amount) AS total_payments
            FROM students s2
            JOIN payments p ON p.student_id = s2.id
            WHERE s2.batch = ? AND p.payment_date <= ?
            GROUP BY s2.id
        ) p ON p.student_id = s.id
        """;

        // That query returns one row per student; we need SUM across all of them.
        // Easiest: compute totals directly with three separate SUM queries, then combine.
        double totalCharges = sumChargesUpToForBatch(batch, asOfMonth);
        double totalAdj = sumAdjustmentsUpToForBatch(batch, endDate);
        double totalPay = sumPaymentsUpToForBatch(batch, endDate);
        return totalCharges + totalAdj - totalPay;
    }

// ---- grouped sums (by YYYY-MM) ----
    private java.util.Map<String, Double> sumChargesByYmForStudent(int studentDbId, YearMonth start, YearMonth end) {
        String startMonth = start.toString() + "-01";
        String endMonth = end.toString() + "-01";

        String sql = """
        SELECT substr(charge_month, 1, 7) AS ym, SUM(amount) AS total
        FROM tuition_charges
        WHERE student_id = ?
          AND charge_month >= ?
          AND charge_month <= ?
        GROUP BY ym
        """;

        return queryYmSum(sql, ps -> {
            ps.setInt(1, studentDbId);
            ps.setString(2, startMonth);
            ps.setString(3, endMonth);
        });
    }

    private java.util.Map<String, Double> sumAdjustmentsByYmForStudent(int studentDbId, YearMonth start, YearMonth end) {
        LocalDate startDate = start.atDay(1);
        LocalDate endDate = end.atEndOfMonth();

        String sql = """
        SELECT substr(adj_date, 1, 7) AS ym, SUM(amount) AS total
        FROM account_adjustments
        WHERE student_id = ?
          AND adj_date >= ?
          AND adj_date <= ?
        GROUP BY ym
        """;

        return queryYmSum(sql, ps -> {
            ps.setInt(1, studentDbId);
            ps.setString(2, startDate.toString());
            ps.setString(3, endDate.toString());
        });
    }

    private java.util.Map<String, Double> sumPaymentsByYmForStudent(int studentDbId, YearMonth start, YearMonth end) {
        LocalDate startDate = start.atDay(1);
        LocalDate endDate = end.atEndOfMonth();

        String sql = """
        SELECT substr(payment_date, 1, 7) AS ym, SUM(amount) AS total
        FROM payments
        WHERE student_id = ?
          AND payment_date >= ?
          AND payment_date <= ?
        GROUP BY ym
        """;

        return queryYmSum(sql, ps -> {
            ps.setInt(1, studentDbId);
            ps.setString(2, startDate.toString());
            ps.setString(3, endDate.toString());
        });
    }

    private java.util.Map<String, Double> sumChargesByYmForBatch(String batch, YearMonth start, YearMonth end) {
        String startMonth = start.toString() + "-01";
        String endMonth = end.toString() + "-01";

        String sql = """
        SELECT substr(tc.charge_month, 1, 7) AS ym, SUM(tc.amount) AS total
        FROM students s
        JOIN tuition_charges tc ON tc.student_id = s.id
        WHERE s.batch = ?
          AND tc.charge_month >= ?
          AND tc.charge_month <= ?
        GROUP BY ym
        """;

        return queryYmSum(sql, ps -> {
            ps.setString(1, batch);
            ps.setString(2, startMonth);
            ps.setString(3, endMonth);
        });
    }

    private java.util.Map<String, Double> sumAdjustmentsByYmForBatch(String batch, YearMonth start, YearMonth end) {
        LocalDate startDate = start.atDay(1);
        LocalDate endDate = end.atEndOfMonth();

        String sql = """
        SELECT substr(a.adj_date, 1, 7) AS ym, SUM(a.amount) AS total
        FROM students s
        JOIN account_adjustments a ON a.student_id = s.id
        WHERE s.batch = ?
          AND a.adj_date >= ?
          AND a.adj_date <= ?
        GROUP BY ym
        """;

        return queryYmSum(sql, ps -> {
            ps.setString(1, batch);
            ps.setString(2, startDate.toString());
            ps.setString(3, endDate.toString());
        });
    }

    private java.util.Map<String, Double> sumPaymentsByYmForBatch(String batch, YearMonth start, YearMonth end) {
        LocalDate startDate = start.atDay(1);
        LocalDate endDate = end.atEndOfMonth();

        String sql = """
        SELECT substr(p.payment_date, 1, 7) AS ym, SUM(p.amount) AS total
        FROM students s
        JOIN payments p ON p.student_id = s.id
        WHERE s.batch = ?
          AND p.payment_date >= ?
          AND p.payment_date <= ?
        GROUP BY ym
        """;

        return queryYmSum(sql, ps -> {
            ps.setString(1, batch);
            ps.setString(2, startDate.toString());
            ps.setString(3, endDate.toString());
        });
    }

// ---- totals up to a cutoff (for batch starting balance) ----
    private double sumChargesUpToForBatch(String batch, String asOfMonthInclusiveYYYYMM01) {
        String sql = """
        SELECT COALESCE(SUM(tc.amount), 0) AS total
        FROM students s
        JOIN tuition_charges tc ON tc.student_id = s.id
        WHERE s.batch = ? AND tc.charge_month <= ?
        """;
        try (java.sql.Connection c = Db.connect(); java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, batch);
            ps.setString(2, asOfMonthInclusiveYYYYMM01);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble("total") : 0.0;
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to sum batch charges", e);
        }
    }

    private double sumAdjustmentsUpToForBatch(String batch, String asOfDateInclusiveYYYYMMDD) {
        String sql = """
        SELECT COALESCE(SUM(a.amount), 0) AS total
        FROM students s
        JOIN account_adjustments a ON a.student_id = s.id
        WHERE s.batch = ? AND a.adj_date <= ?
        """;
        try (java.sql.Connection c = Db.connect(); java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, batch);
            ps.setString(2, asOfDateInclusiveYYYYMMDD);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble("total") : 0.0;
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to sum batch adjustments", e);
        }
    }

    private double sumPaymentsUpToForBatch(String batch, String asOfDateInclusiveYYYYMMDD) {
        String sql = """
        SELECT COALESCE(SUM(p.amount), 0) AS total
        FROM students s
        JOIN payments p ON p.student_id = s.id
        WHERE s.batch = ? AND p.payment_date <= ?
        """;
        try (java.sql.Connection c = Db.connect(); java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, batch);
            ps.setString(2, asOfDateInclusiveYYYYMMDD);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble("total") : 0.0;
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to sum batch payments", e);
        }
    }

// ---- tiny helper for "SELECT ym, SUM(...) GROUP BY ym" ----
    @FunctionalInterface
    private interface Binder {

        void bind(java.sql.PreparedStatement ps) throws java.sql.SQLException;
    }

    private java.util.Map<String, Double> queryYmSum(String sql, Binder binder) {
        java.util.Map<String, Double> map = new java.util.HashMap<>();
        try (java.sql.Connection c = Db.connect(); java.sql.PreparedStatement ps = c.prepareStatement(sql)) {

            binder.bind(ps);

            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("ym"), rs.getDouble("total"));
                }
            }
            return map;

        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to query monthly sums", e);
        }
    }

}