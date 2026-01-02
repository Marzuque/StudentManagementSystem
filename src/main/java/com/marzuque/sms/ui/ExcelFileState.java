package com.marzuque.sms.ui;

import java.io.File;

public final class ExcelFileState {
    private static File currentExcelFile;

    private ExcelFileState() {}

    public static File getCurrentExcelFile() {
        return currentExcelFile;
    }

    public static void setCurrentExcelFile(File file) {
        currentExcelFile = file;
    }

    public static void clear() {
        currentExcelFile = null;
    }
}
