package org.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PowerGuardGUI extends Application {

    private static final int MAX_QUANTITY = 30;
    private static final double MAX_DAILY_HOURS = 24.0;
    private static final double MAX_BUDGET = 1_000_000;

    private static ApplianceModel predictor;

    private final Map<String, Map<String, Integer>> deviceLibrary = new LinkedHashMap<>();
    private final Map<String, XYChart.Data<String, Number>> chartDataByDevice = new LinkedHashMap<>();

    private final ObservableList<PredictionResult> predictionRows = FXCollections.observableArrayList();
    private final ObservableList<String> companyItems = FXCollections.observableArrayList();
    private final ObservableList<String> deviceItems = FXCollections.observableArrayList();

    private final EnergyUsageService usageService = new EnergyUsageService();

    private Scene mainScene;

    private BarChart<String, Number> usageChart;
    private NumberAxis usageYAxis;
    private XYChart.Series<String, Number> chartSeries;

    private ProgressBar budgetBar;

    private ComboBox<String> comboCompany;
    private ComboBox<String> comboDevice;
    private ComboBox<String> comboTheme;

    private TextField txtHours;
    private TextField txtQuantity;
    private TextField txtBudget;

    private Label lblResult;
    private Label lblCarbon;
    private Label lblUnits;
    private Label lblModel;
    private Label lblR2;
    private Label lblRMSE;

    private TableView<PredictionResult> historyTable;

    private String modelName;
    private double r2;
    private double rmse;
    private Runnable onBack;
    private Runnable onLogout;
    private Consumer<String> themeListener;

    public PowerGuardGUI() {
    }

    public PowerGuardGUI(ApplianceModel predictor) {
        PowerGuardGUI.predictor = predictor;
    }

    public PowerGuardGUI(ApplianceModel predictor, String modelName, double r2, double rmse) {
        PowerGuardGUI.predictor = predictor;
        this.modelName = modelName;
        this.r2 = r2;
        this.rmse = rmse;
    }

    public PowerGuardGUI(
            ApplianceModel predictor,
            String modelName,
            double r2,
            double rmse,
            Runnable onBack,
            Runnable onLogout
    ) {
        this(predictor, modelName, r2, rmse);
        this.onBack = onBack;
        this.onLogout = onLogout;
    }

    @Override
    public void start(Stage stage) {
        initializeData();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));

        if (onBack != null || onLogout != null) {
            HBox topBar = TopBar.create(onBack, onLogout, null);
            root.setTop(topBar);
        }

        VBox sidebar = createSidebar();
        VBox dashboard = createDashboard();
        VBox inputPanel = createInputPanel();

        root.setLeft(sidebar);
        root.setCenter(dashboard);
        root.setRight(inputPanel);

        BorderPane.setMargin(sidebar, new Insets(10, 0, 0, 0));
        BorderPane.setMargin(dashboard, new Insets(10, 12, 0, 12));
        BorderPane.setMargin(inputPanel, new Insets(10, 0, 0, 0));

        mainScene = new Scene(root, 1320, 820);
        String selectedTheme = ThemeManager.getCurrentTheme();
        applyTheme(selectedTheme);
        if (comboTheme != null) {
            comboTheme.getSelectionModel().select(selectedTheme);
        }

        stage.setTitle("PowerGuard AI Energy Predictor");
        stage.setMinWidth(1150);
        stage.setMinHeight(760);
        stage.setScene(mainScene);
        stage.show();

        themeListener = this::applyTheme;
        ThemeManager.addListener(themeListener);
        stage.setOnHidden(e -> {
            if (themeListener != null) {
                ThemeManager.removeListener(themeListener);
            }
        });

        refreshUsageChartFromTable();
    }

    private void initializeData() {
        deviceLibrary.clear();

        addCompany("Samsung", Map.ofEntries(
                Map.entry("Inverter AC (1.5 Ton)", 1500),
                Map.entry("Double Door Refrigerator", 280),
                Map.entry("Front Load Washing Machine", 500),
                Map.entry("LED TV 55\"", 120),
                Map.entry("Microwave Oven", 1200),
                Map.entry("Ceiling Fan", 75)
        ));

        addCompany("LG", Map.ofEntries(
                Map.entry("Dual Inverter AC", 1450),
                Map.entry("Smart Refrigerator", 300),
                Map.entry("Top Load Washing Machine", 450),
                Map.entry("OLED TV", 140),
                Map.entry("Water Purifier", 60),
                Map.entry("Air Purifier", 55)
        ));

        addCompany("Sony", Map.ofEntries(
                Map.entry("Bravia TV", 160),
                Map.entry("PlayStation 5", 200),
                Map.entry("Home Theater", 320),
                Map.entry("Sound Bar", 100)
        ));

        addCompany("Panasonic", Map.ofEntries(
                Map.entry("Split AC", 1550),
                Map.entry("Refrigerator", 260),
                Map.entry("Microwave", 1300),
                Map.entry("Induction Cooktop", 1800),
                Map.entry("Ceiling Fan", 70)
        ));

        addCompany("Whirlpool", Map.ofEntries(
                Map.entry("Refrigerator", 300),
                Map.entry("Washing Machine", 500),
                Map.entry("Air Conditioner", 1500),
                Map.entry("Water Heater", 2000)
        ));

        addCompany("Godrej", Map.ofEntries(
                Map.entry("Refrigerator", 250),
                Map.entry("Chest Freezer", 320),
                Map.entry("Washing Machine", 480),
                Map.entry("Air Conditioner", 1400)
        ));

        addCompany("Haier", Map.ofEntries(
                Map.entry("Inverter AC", 1450),
                Map.entry("Refrigerator", 280),
                Map.entry("Washing Machine", 460),
                Map.entry("LED TV", 110)
        ));

        addCompany("Bosch", Map.ofEntries(
                Map.entry("Dishwasher", 1300),
                Map.entry("Washing Machine", 520),
                Map.entry("Dryer", 1800),
                Map.entry("Built-in Oven", 2400)
        ));

        addCompany("Dell", Map.ofEntries(
                Map.entry("Laptop", 65),
                Map.entry("Desktop", 250),
                Map.entry("24-inch Monitor", 35),
                Map.entry("Wi-Fi Router", 12)
        ));

        addCompany("HP", Map.ofEntries(
                Map.entry("Laptop", 70),
                Map.entry("Desktop", 230),
                Map.entry("Laser Printer", 400),
                Map.entry("Monitor", 32)
        ));

        addCompany("Apple", Map.ofEntries(
                Map.entry("MacBook Pro", 96),
                Map.entry("iMac", 185),
                Map.entry("Apple TV", 6),
                Map.entry("HomePod", 8)
        ));

        addCompany("Philips", Map.ofEntries(
                Map.entry("LED Bulb 9W", 9),
                Map.entry("Tube Light", 20),
                Map.entry("Air Fryer", 1400),
                Map.entry("Room Heater", 2000),
                Map.entry("Induction Cooktop", 1800)
        ));

        addCompany("Havells", Map.ofEntries(
                Map.entry("Ceiling Fan", 70),
                Map.entry("Water Pump", 750),
                Map.entry("Room Heater", 2000),
                Map.entry("LED Batten", 18)
        ));

        addCompany("Bajaj", Map.ofEntries(
                Map.entry("Ceiling Fan", 75),
                Map.entry("Water Heater", 2000),
                Map.entry("Mixer Grinder", 750),
                Map.entry("Microwave", 1100)
        ));

        addCompany("Tesla (EV)", Map.ofEntries(
                Map.entry("Wall Connector (11.5kW)", 11500),
                Map.entry("Portable EV Charger", 3500)
        ));

        companyItems.setAll(deviceLibrary.keySet());
    }

    private void addCompany(String companyName, Map<String, Integer> appliances) {
        deviceLibrary.put(companyName, new LinkedHashMap<>(appliances));
    }

    private VBox createSidebar() {
        Label title = new Label("POWERGUARD");
        title.getStyleClass().add("sidebar-title");

        Label subtitle = new Label("AI Energy Predictor");
        subtitle.getStyleClass().add("sidebar-subtitle");

        Label themeLabel = new Label("Theme");
        themeLabel.getStyleClass().add("field-label");

        comboTheme = new ComboBox<>();
        comboTheme.getItems().addAll(ThemeManager.DARK_THEME, ThemeManager.BLUE_THEME);
        comboTheme.getSelectionModel().select(ThemeManager.getCurrentTheme());
        comboTheme.setMaxWidth(Double.MAX_VALUE);
        comboTheme.setOnAction(e -> ThemeManager.setCurrentTheme(comboTheme.getValue()));

        Button reset = new Button("RESET ALL");
        Button export = new Button("EXPORT PDF");
        reset.getStyleClass().addAll("primary-button", "button-wide");
        export.getStyleClass().addAll("ghost-button", "button-wide");

        reset.setOnAction(e -> resetAll());
        export.setOnAction(e -> exportPdfReport(export.getScene().getWindow()));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox box = new VBox(12, title, subtitle, new Separator(), themeLabel, comboTheme, reset, export, spacer);
        box.getStyleClass().add("sidebar");
        box.setPadding(new Insets(20));
        box.setPrefWidth(235);
        box.setMinWidth(220);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private VBox createDashboard() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Device");

        usageYAxis = new NumberAxis();
        usageYAxis.setLabel("Monthly kWh");
        usageYAxis.setAutoRanging(true);
        usageYAxis.setForceZeroInRange(true);

        usageChart = new BarChart<>(xAxis, usageYAxis);
        usageChart.setTitle("Monthly Energy Usage by Device");
        usageChart.setLegendVisible(false);
        usageChart.setAnimated(true);
        usageChart.setCategoryGap(16);
        usageChart.setBarGap(6);
        usageChart.setMinHeight(320);
        usageChart.setPrefHeight(420);

        chartSeries = new XYChart.Series<>();
        usageChart.getData().add(chartSeries);

        historyTable = new TableView<>();
        historyTable.getStyleClass().add("prediction-table");
        historyTable.setItems(predictionRows);
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<PredictionResult, String> deviceCol = new TableColumn<>("Device");
        deviceCol.setCellValueFactory(data -> data.getValue().deviceProperty());

        TableColumn<PredictionResult, String> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(data -> data.getValue().quantityProperty());

        TableColumn<PredictionResult, String> hoursCol = new TableColumn<>("Hours/Day");
        hoursCol.setCellValueFactory(data -> data.getValue().hoursProperty());

        TableColumn<PredictionResult, String> unitsCol = new TableColumn<>("Monthly kWh");
        unitsCol.setCellValueFactory(data -> data.getValue().unitsProperty());

        TableColumn<PredictionResult, String> costCol = new TableColumn<>("Predicted Bill");
        costCol.setCellValueFactory(data -> data.getValue().costProperty());

        TableColumn<PredictionResult, String> carbonCol = new TableColumn<>("CO2");
        carbonCol.setCellValueFactory(data -> data.getValue().carbonProperty());

        TableColumn<PredictionResult, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> data.getValue().statusProperty());

        historyTable.getColumns().addAll(deviceCol, qtyCol, hoursCol, unitsCol, costCol, carbonCol, statusCol);

        predictionRows.addListener((ListChangeListener<PredictionResult>) change -> refreshUsageChartFromTable());

        VBox.setVgrow(usageChart, Priority.ALWAYS);
        VBox.setVgrow(historyTable, Priority.ALWAYS);

        VBox box = new VBox(12, usageChart, historyTable);
        box.getStyleClass().add("panel");
        box.setPadding(new Insets(14));
        box.setFillWidth(true);
        return box;
    }

    private VBox createInputPanel() {
        String shownModel = (this.modelName == null || this.modelName.isBlank()) ? "N/A" : this.modelName;

        lblModel = new Label("Model: " + shownModel);
        lblModel.getStyleClass().add("meta-label");
        lblR2 = new Label(String.format("R2: %.4f", this.r2));
        lblR2.getStyleClass().add("meta-label");
        lblRMSE = new Label(String.format("RMSE: %.4f", this.rmse));
        lblRMSE.getStyleClass().add("meta-label");

        comboCompany = new ComboBox<>();
        comboCompany.setPrefWidth(Double.MAX_VALUE);
        setupSearchableComboBox(comboCompany, companyItems);

        comboDevice = new ComboBox<>();
        comboDevice.setPrefWidth(Double.MAX_VALUE);
        setupSearchableComboBox(comboDevice, deviceItems);

        comboCompany.setOnAction(e -> {
            updateDeviceList();
            comboCompany.getEditor().setText(comboCompany.getValue() == null ? "" : comboCompany.getValue());
        });

        txtQuantity = new TextField("1");
        txtHours = new TextField("5");
        txtBudget = new TextField("500");

        GridPane inputGrid = new GridPane();
        inputGrid.setHgap(10);
        inputGrid.setVgap(10);

        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setPercentWidth(40);
        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setPercentWidth(60);
        inputGrid.getColumnConstraints().addAll(labelCol, fieldCol);

        addInputRow(inputGrid, 0, "Company", comboCompany);
        addInputRow(inputGrid, 1, "Device", comboDevice);
        addInputRow(inputGrid, 2, "Quantity", txtQuantity);
        addInputRow(inputGrid, 3, "Daily Hours", txtHours);
        addInputRow(inputGrid, 4, "Budget (Rs)", txtBudget);

        budgetBar = new ProgressBar(0);
        budgetBar.setPrefWidth(Double.MAX_VALUE);
        budgetBar.getStyleClass().add("budget-bar");

        Button predict = new Button("PREDICT BILL");
        predict.getStyleClass().addAll("primary-button", "button-wide");
        predict.setOnAction(e -> calculate());

        lblResult = new Label("Rs 0.00");
        lblResult.getStyleClass().add("result-value");

        lblUnits = new Label("Monthly Usage: 0.00 kWh");
        lblUnits.getStyleClass().add("result-meta");
        lblCarbon = new Label("CO2: 0.00 kg");
        lblCarbon.getStyleClass().add("result-meta");

        Label inputHeader = new Label("Input Parameters");
        inputHeader.getStyleClass().add("section-title");

        VBox panel = new VBox(
                10,
                lblModel,
                lblR2,
                lblRMSE,
                new Separator(),
                inputHeader,
                inputGrid,
                budgetBar,
                predict,
                new Separator(),
                lblResult,
                lblUnits,
                lblCarbon
        );

        panel.getStyleClass().add("panel");
        panel.getStyleClass().add("input-panel");
        panel.setPadding(new Insets(14));
        panel.setPrefWidth(340);
        panel.setMinWidth(310);
        panel.setMaxWidth(420);
        return panel;
    }

    private void addInputRow(GridPane grid, int row, String labelText, Region inputControl) {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        GridPane.setHgrow(inputControl, Priority.ALWAYS);
        grid.add(label, 0, row);
        grid.add(inputControl, 1, row);
    }

    private void setupSearchableComboBox(ComboBox<String> comboBox, ObservableList<String> sourceItems) {
        comboBox.setEditable(true);
        FilteredList<String> filteredItems = new FilteredList<>(sourceItems, item -> true);
        comboBox.setItems(filteredItems);

        comboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (!comboBox.isFocused()) {
                return;
            }
            String normalized = newText == null ? "" : newText.trim().toLowerCase();
            filteredItems.setPredicate(item -> item.toLowerCase().contains(normalized));
            if (!comboBox.isShowing()) {
                comboBox.show();
            }
        });

        comboBox.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                return;
            }
            String typed = comboBox.getEditor().getText() == null ? "" : comboBox.getEditor().getText().trim();
            if (typed.isEmpty()) {
                comboBox.getSelectionModel().clearSelection();
                filteredItems.setPredicate(item -> true);
                return;
            }

            String match = sourceItems.stream()
                    .filter(item -> item.equalsIgnoreCase(typed))
                    .findFirst()
                    .orElse(null);

            if (match != null) {
                comboBox.getSelectionModel().select(match);
                comboBox.getEditor().setText(match);
            } else if (comboBox.getValue() != null) {
                comboBox.getEditor().setText(comboBox.getValue());
            } else {
                comboBox.getEditor().clear();
            }
            filteredItems.setPredicate(item -> true);
        });
    }

    private void applyTheme(String selectedTheme) {
        if (mainScene == null || selectedTheme == null) {
            return;
        }

        String stylesheet;
        if (ThemeManager.BLUE_THEME.equals(selectedTheme)) {
            stylesheet = getClass().getResource("/blue-theme.css").toExternalForm();
        } else {
            stylesheet = getClass().getResource("/dark-theme.css").toExternalForm();
        }
        String base = getClass().getResource("/style.css").toExternalForm();

        mainScene.getStylesheets().clear();
        mainScene.getStylesheets().add(base);
        mainScene.getStylesheets().add(stylesheet);
        if (comboTheme != null && comboTheme.getValue() != null && !comboTheme.getValue().equals(selectedTheme)) {
            comboTheme.getSelectionModel().select(selectedTheme);
        }
        refreshUsageChartFromTable();
    }

    private void updateDeviceList() {
        comboDevice.getSelectionModel().clearSelection();
        comboDevice.getEditor().clear();

        String company = comboCompany.getValue();
        if (company == null || !deviceLibrary.containsKey(company)) {
            deviceItems.clear();
            return;
        }

        deviceItems.setAll(deviceLibrary.get(company).keySet());
        if (!deviceItems.isEmpty()) {
            comboDevice.getSelectionModel().selectFirst();
            comboDevice.getEditor().setText(comboDevice.getValue());
        }
    }

    private void calculate() {
        try {
            validateSelection();

            String company = comboCompany.getValue();
            String device = comboDevice.getValue();

            int rating = deviceLibrary.get(company).get(device);
            int quantity = parseQuantity(txtQuantity.getText());
            double hours = parseHours(txtHours.getText());
            double budgetLimit = parseBudget(txtBudget.getText());

            EnergyUsageService.PredictionMetrics metrics = usageService.calculate(
                    device,
                    rating,
                    quantity,
                    hours,
                    predictor
            );

            updateResultPanel(metrics, budgetLimit);
            updateHistoryTable(device, quantity, hours, budgetLimit, metrics);

        } catch (IllegalArgumentException ex) {
            showWarning(ex.getMessage());
        } catch (Exception ex) {
            showWarning("Prediction failed: " + ex.getMessage());
        }
    }

    private void updateResultPanel(EnergyUsageService.PredictionMetrics metrics, double budgetLimit) {
        double cost = metrics.billAmount();
        double progress = budgetLimit <= 0 ? 0 : cost / budgetLimit;

        lblResult.setText(String.format("Rs %.2f", cost));
        lblUnits.setText(String.format("Monthly Usage: %.2f kWh (base %.2f)", metrics.adjustedUnits(), metrics.physicalUnits()));
        lblCarbon.setText(String.format("CO2: %.2f kg", metrics.co2Kg()));

        budgetBar.setProgress(Math.min(progress, 1.0));
        budgetBar.getStyleClass().removeAll("budget-safe", "budget-over");
        budgetBar.getStyleClass().add(cost > budgetLimit ? "budget-over" : "budget-safe");
    }

    private void refreshUsageChartFromTable() {
        Map<String, Double> aggregatedUsage = new LinkedHashMap<>();
        for (PredictionResult row : predictionRows) {
            String device = row.deviceProperty().get();
            double units = parseUnits(row.unitsProperty().get());
            aggregatedUsage.merge(device, units, Double::sum);
        }

        chartSeries.getData().clear();
        chartDataByDevice.clear();

        for (Map.Entry<String, Double> entry : aggregatedUsage.entrySet()) {
            String device = entry.getKey();
            double value = entry.getValue();

            XYChart.Data<String, Number> point = new XYChart.Data<>(device, value);
            chartDataByDevice.put(device, point);
            chartSeries.getData().add(point);
            attachTooltip(point, String.format("%s\nMonthly kWh: %.2f", device, value));
        }

        if (!usageChart.getData().contains(chartSeries)) {
            usageChart.getData().clear();
            usageChart.getData().add(chartSeries);
        }
        usageYAxis.setAutoRanging(true);
        usageChart.requestLayout();
    }

    private double parseUnits(String unitsText) {
        if (unitsText == null || unitsText.isBlank()) {
            return 0;
        }
        String normalized = unitsText.replaceAll("[^0-9.]", "");
        if (normalized.isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void attachTooltip(XYChart.Data<String, Number> data, String text) {
        Tooltip tooltip = new Tooltip(text);
        if (data.getNode() != null) {
            Tooltip.install(data.getNode(), tooltip);
            return;
        }
        Platform.runLater(() -> {
            if (data.getNode() != null) {
                Tooltip.install(data.getNode(), tooltip);
            } else {
                data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        Tooltip.install(newNode, tooltip);
                    }
                });
            }
        });
    }

    private void updateHistoryTable(
            String device,
            int quantity,
            double hours,
            double budgetLimit,
            EnergyUsageService.PredictionMetrics metrics
    ) {
        String status = metrics.billAmount() > budgetLimit ? "OVER BUDGET" : "WITHIN BUDGET";
        predictionRows.add(new PredictionResult(
                device,
                String.valueOf(quantity),
                String.format("%.2f", hours),
                String.format("%.2f", metrics.adjustedUnits()),
                String.format("Rs %.2f", metrics.billAmount()),
                String.format("%.2f kg", metrics.co2Kg()),
                status
        ));
        historyTable.refresh();
    }

    private void resetAll() {
        predictionRows.clear();
        chartDataByDevice.clear();
        chartSeries.getData().clear();

        comboCompany.getSelectionModel().clearSelection();
        comboCompany.getEditor().clear();
        comboDevice.getSelectionModel().clearSelection();
        comboDevice.getEditor().clear();
        deviceItems.clear();

        txtQuantity.setText("1");
        txtHours.setText("5");
        txtBudget.setText("500");

        lblResult.setText("Rs 0.00");
        lblUnits.setText("Monthly Usage: 0.00 kWh");
        lblCarbon.setText("CO2: 0.00 kg");

        budgetBar.setProgress(0);
        budgetBar.getStyleClass().removeAll("budget-safe", "budget-over");

        refreshUsageChartFromTable();
    }

    private void exportPdfReport(Window ownerWindow) {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Export Usage Report as PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            chooser.setInitialFileName("powerguard_usage_report.pdf");

            File selectedFile = chooser.showSaveDialog(ownerWindow);
            if (selectedFile == null) {
                return;
            }

            if (!selectedFile.getName().toLowerCase().endsWith(".pdf")) {
                selectedFile = new File(selectedFile.getAbsolutePath() + ".pdf");
            }

            WritableImage snapshot = usageChart.snapshot(new SnapshotParameters(), null);
            BufferedImage chartImage = SwingFXUtils.fromFXImage(snapshot, null);
            writePdfReport(selectedFile, chartImage);

            showInfo("Analytics exported to: " + selectedFile.getAbsolutePath());
        } catch (Exception ex) {
            showWarning("Export failed: " + ex.getMessage());
        }
    }

    private void writePdfReport(File file, BufferedImage chartImage) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float margin = 36;
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float availableWidth = pageWidth - (2 * margin);

            PDImageXObject pdfImage = LosslessFactory.createFromImage(document, chartImage);

            float imageWidth = availableWidth;
            float imageHeight = (chartImage.getHeight() * imageWidth) / chartImage.getWidth();
            imageHeight = Math.min(imageHeight, pageHeight - 180);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 16);
                content.newLineAtOffset(margin, pageHeight - 50);
                content.showText("PowerGuard AI Energy Predictor - Usage Report");
                content.endText();

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 10);
                content.newLineAtOffset(margin, pageHeight - 68);
                content.showText("Generated: " + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()));
                content.endText();

                float imageY = pageHeight - 90 - imageHeight;
                content.drawImage(pdfImage, margin, imageY, imageWidth, imageHeight);
            }

            document.save(file);
        }
    }

    private void validateSelection() {
        if (comboCompany.getValue() == null || !deviceLibrary.containsKey(comboCompany.getValue())) {
            throw new IllegalArgumentException("Select a valid company.");
        }
        if (comboDevice.getValue() == null || !deviceLibrary.get(comboCompany.getValue()).containsKey(comboDevice.getValue())) {
            throw new IllegalArgumentException("Select a valid device.");
        }
    }

    private int parseQuantity(String value) {
        int quantity = parsePositiveInt(value, "Quantity");
        if (quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("Quantity is too high. Allowed range: 1 to " + MAX_QUANTITY + ".");
        }
        return quantity;
    }

    private double parseBudget(String value) {
        double budget = parsePositiveDouble(value, "Budget");
        if (budget > MAX_BUDGET) {
            throw new IllegalArgumentException("Budget is too high. Enter a value up to " + (int) MAX_BUDGET + ".");
        }
        return budget;
    }

    private int parsePositiveInt(String value, String field) {
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed <= 0) {
                throw new IllegalArgumentException(field + " must be greater than zero.");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(field + " must be a whole number.");
        }
    }

    private double parsePositiveDouble(String value, String field) {
        try {
            double parsed = Double.parseDouble(value.trim());
            if (parsed <= 0) {
                throw new IllegalArgumentException(field + " must be greater than zero.");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(field + " must be numeric.");
        }
    }

    private double parseHours(String value) {
        double hours = parsePositiveDouble(value, "Daily Hours");
        if (hours > MAX_DAILY_HOURS) {
            throw new IllegalArgumentException("Daily Hours must be between 0 and 24.");
        }
        return hours;
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText("Validation Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText("Success");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
