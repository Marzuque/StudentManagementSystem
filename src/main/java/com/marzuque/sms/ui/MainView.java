package com.marzuque.sms.ui;

import com.marzuque.sms.dao.StudentDao;
import com.marzuque.sms.model.AccountAdjustment;
import com.marzuque.sms.model.Student;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class MainView {

    // Flat fee for now (BDT). Later we’ll move this to a settings table.
    private static final double MONTHLY_TUITION_FEE = 5000.0;

    private final StudentDao dao = new StudentDao();

    // Students list
    private final ObservableList<Student> master = FXCollections.observableArrayList();
    private final FilteredList<Student> filtered = new FilteredList<>(master, s -> true);

    private final TableView<Student> table = new TableView<>();
    private final TextField search = new TextField();

    // Batch tabs
    private final TabPane batchTabs = new TabPane();
    private String activeBatch = null; // null = All

    // Adjustments panel
    private final Label selectedStudentLabel = new Label("No student selected");
    private final Label balanceLabel = new Label("Balance: 0");
    private final TableView<AccountAdjustment> adjTable = new TableView<>();
    private final ObservableList<AccountAdjustment> adjMaster = FXCollections.observableArrayList();

    // Buttons we need to enable/disable
    private final Button editBtn = new Button("Edit");
    private final Button delBtn = new Button("Delete");
    private final Button saveExcelBtn = new Button("Save Excel");
    private final Button setBalanceBtn = new Button("Set Balance");

    private final Button studentReportBtn = new Button("Export Student Report");
    private final Button batchReportBtn = new Button("Export Batch Report");

    public Parent create() {
        // --- Top toolbar buttons
        Button addBtn = new Button("Add Student");
        Button importBtn = new Button("Import Excel");
        Button saveAsExcelBtn = new Button("Save As Excel...");
        Button refreshBtn = new Button("Refresh");
        Button genChargesBtn = new Button("Generate Charges (Up to This Month)");

        editBtn.setDisable(true);
        delBtn.setDisable(true);
        setBalanceBtn.setDisable(true);

        studentReportBtn.setDisable(true); // enable only when student selected

        addBtn.setOnAction(e -> onAdd());
        editBtn.setOnAction(e -> onEditSelected());
        delBtn.setOnAction(e -> onDeleteSelected());

        importBtn.setOnAction(e -> onImportExcel());
        saveExcelBtn.setOnAction(e -> onSaveExcel());
        saveAsExcelBtn.setOnAction(e -> onSaveAsExcel());
        refreshBtn.setOnAction(e -> load());

        genChargesBtn.setOnAction(e -> {
            try {
                dao.generateChargesUpToMonthForAllStudents(MONTHLY_TUITION_FEE, YearMonth.now());
                load();            // refresh balances
                loadAdjustments(); // keep right panel consistent
            } catch (RuntimeException ex) {
                showError("Charge generation failed", ex.getMessage());
            }
        });

        setBalanceBtn.setOnAction(e -> onSetBalance());

        studentReportBtn.setOnAction(e -> {
            Student sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) {
                showError("Student report", "Select a student first.");
                return;
            }

            MonthsBackDialog.ask("Student Report Range", 11).ifPresent(monthsBack -> {
                try {
                    var rows = dao.buildStudentReportPreviousMonths(sel.getId(), YearMonth.now(), monthsBack);
                    ReportExportDialog.exportStudent12Months(table, sel, rows);
                } catch (RuntimeException ex) {
                    showError("Student report failed", ex.getMessage());
                }
            });
        });

        batchReportBtn.setOnAction(e -> {
            String batch = activeBatch; // tab-driven
            if (batch == null || batch.isBlank()) {
                showError("Batch report", "Switch to a batch tab first (not 'All').");
                return;
            }

            MonthsBackDialog.ask("Batch Report Range", 11).ifPresent(monthsBack -> {
                try {
                    var rows = dao.buildBatchReportPreviousMonths(batch, YearMonth.now(), monthsBack);
                    ReportExportDialog.exportBatch12Months(table, batch, rows);
                } catch (RuntimeException ex) {
                    showError("Batch report failed", ex.getMessage());
                }
            });
        });

        updateSaveButtons();

        // --- Selection behavior
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            boolean has = sel != null;
            editBtn.setDisable(!has);
            delBtn.setDisable(!has);
            setBalanceBtn.setDisable(!has);
            studentReportBtn.setDisable(!has);

            if (!has) {
                selectedStudentLabel.setText("No student selected");
                balanceLabel.setText("Balance: 0");
                adjMaster.clear();
            } else {
                selectedStudentLabel.setText(sel.getStudentId() + " — " + sel.getFullName());
                balanceLabel.setText("Balance: " + sel.getCurrentBalance());
                loadAdjustments();
            }
        });

        // --- Search
        search.setPromptText("Search ID, name...");
        search.textProperty().addListener((obs, o, n) -> applyFilters());

        ToolBar bar = new ToolBar(
                addBtn, editBtn, delBtn,
                new Separator(),
                new Label("Search:"), search,
                new Separator(),
                genChargesBtn,
                new Separator(),
                studentReportBtn, batchReportBtn,
                new Separator(),
                importBtn, saveExcelBtn, saveAsExcelBtn,
                new Separator(),
                refreshBtn
        );

        // --- Batch tabs setup
        batchTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        batchTabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            activeBatch = (newTab == null) ? null : (String) newTab.getUserData();
            applyFilters();

            // clear selection when switching tabs to avoid “selected student not visible” confusion
            table.getSelectionModel().clearSelection();
            selectedStudentLabel.setText("No student selected");
            balanceLabel.setText("Balance: 0");
            adjMaster.clear();
        });

        // --- Students table columns
        TableColumn<Student, String> sid = new TableColumn<>("Student ID");
        sid.setCellValueFactory(d -> d.getValue().studentIdProperty());

        TableColumn<Student, String> name = new TableColumn<>("Full Name");
        name.setCellValueFactory(d -> d.getValue().fullNameProperty());

        TableColumn<Student, String> batch = new TableColumn<>("Batch");
        batch.setCellValueFactory(d -> d.getValue().batchProperty());

        TableColumn<Student, Number> cgpa = new TableColumn<>("CGPA");
        cgpa.setCellValueFactory(d -> d.getValue().cgpaProperty());

        TableColumn<Student, Number> semCgpa = new TableColumn<>("Sem CGPA");
        semCgpa.setCellValueFactory(d -> d.getValue().semesterCgpaProperty());

        TableColumn<Student, Number> balance = new TableColumn<>("Current Balance");
        balance.setCellValueFactory(d -> d.getValue().currentBalanceProperty());

        table.getColumns().setAll(sid, name, batch, cgpa, semCgpa, balance);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No students yet. Click “Add Student”."));

        table.setItems(filtered);

        // Double-click row to edit
        table.setRowFactory(tv -> {
            TableRow<Student> row = new TableRow<>();
            row.setOnMouseClicked(evt -> {
                if (!row.isEmpty()
                        && evt.getButton() == MouseButton.PRIMARY
                        && evt.getClickCount() == 2) {
                    onEdit(row.getItem());
                }
            });
            return row;
        });

        // --- Adjustments table
        TableColumn<AccountAdjustment, String> cDate = new TableColumn<>("Date");
        cDate.setCellValueFactory(d -> d.getValue().adjDateProperty());

        TableColumn<AccountAdjustment, Number> cAmt = new TableColumn<>("Amount");
        cAmt.setCellValueFactory(d -> d.getValue().amountProperty());

        TableColumn<AccountAdjustment, String> cNote = new TableColumn<>("Note");
        cNote.setCellValueFactory(d -> d.getValue().noteProperty());

        adjTable.getColumns().setAll(cDate, cAmt, cNote);
        adjTable.setItems(adjMaster);
        adjTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        adjTable.setPlaceholder(new Label("No adjustments yet."));

        VBox right = new VBox(
                10,
                selectedStudentLabel,
                balanceLabel,
                setBalanceBtn,
                new Separator(),
                new Label("Adjustments"),
                adjTable
        );
        right.setPadding(new Insets(10));
        right.setPrefWidth(380);

        SplitPane split = new SplitPane();
        split.getItems().addAll(table, right);
        split.setDividerPositions(0.68);

        BorderPane root = new BorderPane();
        VBox top = new VBox(bar, batchTabs);
        root.setTop(top);
        root.setCenter(split);
        BorderPane.setMargin(split, new Insets(10));

        load();
        return root;
    }

    private void applyFilters() {
        String q = (search.getText() == null) ? "" : search.getText().trim().toLowerCase();
        String batch = activeBatch; // null = All

        filtered.setPredicate(s -> {
            if (batch != null && !batch.isBlank()) {
                if (s.getBatch() == null) {
                    return false;
                }
                if (!batch.equalsIgnoreCase(s.getBatch())) {
                    return false;
                }
            }

            if (q.isEmpty()) {
                return true;
            }

            return safe(s.getStudentId()).contains(q)
                    || safe(s.getFullName()).contains(q)
                    || safe(s.getBatch()).contains(q);
        });
    }

    private void load() {
        List<Student> all = dao.findAll();

        // Compute balances as-of current month
        Map<Integer, Double> balances = dao.getBalancesAsOf(YearMonth.now());
        for (Student s : all) {
            s.setCurrentBalance(balances.getOrDefault(s.getId(), 0.0));
        }

        master.setAll(all);

        rebuildBatchTabs(all);
        applyFilters();

        Student sel = table.getSelectionModel().getSelectedItem();
        if (sel != null) {
            balanceLabel.setText("Balance: " + sel.getCurrentBalance());
        }
    }

    private void rebuildBatchTabs(List<Student> all) {
        String previouslyActive = activeBatch; // preserve by batch key, not by tab label

        Map<String, Long> counts = all.stream()
                .map(Student::getBatch)
                .filter(b -> b != null && !b.isBlank())
                .collect(Collectors.groupingBy(b -> b, Collectors.counting()));

        List<String> batches = new ArrayList<>(counts.keySet());
        batches.sort(String.CASE_INSENSITIVE_ORDER);

        batchTabs.getTabs().clear();

        Tab allTab = new Tab("All (" + all.size() + ")");
        allTab.setUserData(null); // null = All
        batchTabs.getTabs().add(allTab);

        for (String b : batches) {
            long n = counts.getOrDefault(b, 0L);
            Tab t = new Tab(b + " (" + n + ")");
            t.setUserData(b); // IMPORTANT: actual batch key
            batchTabs.getTabs().add(t);
        }

        // Restore previous selection if possible
        Tab toSelect = allTab;
        if (previouslyActive != null && !previouslyActive.isBlank()) {
            for (Tab t : batchTabs.getTabs()) {
                if (previouslyActive.equalsIgnoreCase(String.valueOf(t.getUserData()))) {
                    toSelect = t;
                    break;
                }
            }
        }

        batchTabs.getSelectionModel().select(toSelect);
        activeBatch = (String) toSelect.getUserData();
    }

    private void loadAdjustments() {
        Student sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            adjMaster.clear();
            return;
        }
        try {
            adjMaster.setAll(dao.listAdjustments(sel.getId()));
        } catch (RuntimeException ex) {
            showError("Could not load adjustments", ex.getMessage());
            adjMaster.clear();
        }
    }

    private void onSetBalance() {
        Student sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            return;
        }

        SetBalanceDialog.show(sel.getCurrentBalance()).ifPresent(res -> {
            try {
                dao.setBalanceAsOfCurrentMonth(sel.getId(), res.desiredBalance, res.note);
                load();
                loadAdjustments();
            } catch (RuntimeException ex) {
                showError("Could not set balance", ex.getMessage());
            }
        });
    }

    private void onAdd() {
        StudentFormDialog.showAddDialog().ifPresent(s -> {
            try {
                dao.insert(s);
                load();
            } catch (RuntimeException ex) {
                showError("Could not save student", friendlyDbMessage(ex));
            }
        });
    }

    private void onEditSelected() {
        Student sel = table.getSelectionModel().getSelectedItem();
        if (sel != null) {
            onEdit(sel);
        }
    }

    private void onEdit(Student sel) {
        StudentFormDialog.showEditDialog(sel).ifPresent(updated -> {
            try {
                dao.update(updated);
                load();
                loadAdjustments();
            } catch (RuntimeException ex) {
                showError("Could not update student", friendlyDbMessage(ex));
            }
        });
    }

    private void onDeleteSelected() {
        Student sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Student");
        confirm.setHeaderText("Are you sure you want to delete this student?");
        confirm.setContentText(sel.getStudentId() + " — " + sel.getFullName());

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    dao.deleteById(sel.getId());
                    load();
                    loadAdjustments();
                } catch (RuntimeException ex) {
                    showError("Could not delete student", friendlyDbMessage(ex));
                }
            }
        });
    }

    private void onImportExcel() {
        ExcelImportDialog.importStudents(table, dao, this::load);
        updateSaveButtons();
    }

    private void onSaveExcel() {
        List<Student> all = dao.findAll();
        ExcelExportDialog.saveToCurrentOrSaveAs(table, all);
        updateSaveButtons();
    }

    private void onSaveAsExcel() {
        List<Student> all = dao.findAll();
        ExcelExportDialog.saveAs(table, all);
        updateSaveButtons();
    }

    private void updateSaveButtons() {
        saveExcelBtn.setDisable(ExcelFileState.getCurrentExcelFile() == null);
    }

    private static String safe(String v) {
        return v == null ? "" : v.toLowerCase();
    }

    private static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message == null ? "Unknown error" : message);
        alert.showAndWait();
    }

    private static String friendlyDbMessage(RuntimeException ex) {
        Throwable t = ex.getCause();
        String msg = (t == null || t.getMessage() == null) ? ex.getMessage() : t.getMessage();
        if (msg != null && msg.toLowerCase().contains("unique")) {
            return "That Student ID already exists. Please use a different Student ID.";
        }
        return "Something went wrong.\n\nDetails: " + msg;
    }
}
