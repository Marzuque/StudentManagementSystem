package com.marzuque.sms.util;

import java.time.YearMonth;

public class StudentValidator {

    public static String normalizeBatch(String raw) {
        if (raw == null) {
            return "";
        }
        String b = raw.trim();
        if (b.isEmpty()) {
            return "";
        }

        // If user types "50", normalize to "50th" (simple rule)
        // You can change this to keep it as "50" if you prefer.
        if (b.matches("\\d+")) {
            int n = Integer.parseInt(b);
            return n + ordinalSuffix(n);
        }

        // If user types "50th batch" -> keep just "50th"
        // Very simple cleanup:
        b = b.replaceAll("(?i)\\s*batch\\s*", "").trim();

        return b;
    }

    private static String ordinalSuffix(int n) {
        int mod100 = n % 100;
        if (mod100 >= 11 && mod100 <= 13) {
            return "th";
        }
        switch (n % 10) {
            case 1:
                return "st";
            case 2:
                return "nd";
            case 3:
                return "rd";
            default:
                return "th";
        }
    }

    public static String normalizeBillingStartMonth(String raw) {
        // Must store "YYYY-MM-01"
        if (raw == null || raw.trim().isEmpty()) {
            return YearMonth.now().toString() + "-01";
        }
        String t = raw.trim().replace('/', '-');

        // Accept "YYYY-MM" -> add "-01"
        if (t.matches("\\d{4}-\\d{2}")) {
            YearMonth.parse(t);
            return t + "-01";
        }

        // Accept "YYYY-MM-DD" -> normalize day to 01
        if (t.matches("\\d{4}-\\d{2}-\\d{2}")) {
            YearMonth.parse(t.substring(0, 7));
            return t.substring(0, 7) + "-01";
        }

        // Try best-effort: first 7 chars like YYYY-MM
        if (t.length() >= 7) {
            String ym = t.substring(0, 7);
            YearMonth.parse(ym);
            return ym + "-01";
        }

        // If invalid, default to current month
        return YearMonth.now().toString() + "-01";
    }

    public static Double parseCgpaOrNull(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return null;
        }
        double v = Double.parseDouble(t);
        if (v < 0.0 || v > 4.0) {
            throw new IllegalArgumentException("CGPA must be between 0.0 and 4.0");
        }
        return v;
    }

    public static String validateRequired(String label, String value) {
        if (value == null || value.trim().isEmpty()) {
            return label + " is required.";
        }
        return null;
    }
}
