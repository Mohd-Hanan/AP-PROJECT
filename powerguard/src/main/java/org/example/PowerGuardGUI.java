package org.example;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class PowerGuardGUI extends Application {

    private static ApplianceModel predictor;

    private final Map<String, Map<String, Integer>> deviceLibrary = new HashMap<>();
    private final Map<String, XYChart.Data<String, Number>> chartDataByDevice = new HashMap<>();

    private final ObservableList<PredictionResult> predictionRows = FXCollections.observableArrayList();

    private final EnergyUsageService usageService = new EnergyUsageService();

    private BarChart<String, Number> usageChart;
    private XYChart.Series<String, Number> chartSeries;

    private ProgressBar budgetBar;

    private ComboBox<String> comboCompany;
    private ComboBox<String> comboDevice;

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

    @Override
    public void start(Stage stage) {
        initializeData();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));
        root.setLeft(createSidebar());
        root.setCenter(createDashboard());
        root.setRight(createInputPanel());
        BorderPane.setMargin(root.getCenter(), new Insets(0, 12, 0, 12));

        Scene scene = new Scene(root, 1320, 820);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        stage.setTitle("PowerGuard AI Energy Predictor");
        stage.setMinWidth(1150);
        stage.setMinHeight(760);
        stage.setScene(scene);
        stage.show();
    }

    private void initializeData() {
        Map<String, Integer> samsung = new HashMap<>();
        samsung.put("Smart Refrigerator", 400);
        samsung.put("Inverter AC", 1500);

        Map<String, Integer> sony = new HashMap<>();
        sony.put("PlayStation 5", 200);
        sony.put("Bravia TV", 180);

        Map<String, Integer> tesla = new HashMap<>();
        tesla.put("Wall Connector (11.5kW)", 11500);

        deviceLibrary.put("Tesla (EV)", tesla);
        deviceLibrary.put("Samsung", samsung);
        deviceLibrary.put("Sony", sony);
    }

    private VBox createSidebar() {
        Label title = new Label("POWERGUARD");
        title.getStyleClass().add("sidebar-title");

        Label subtitle = new Label("AI Energy Predictor");
        subtitle.getStyleClass().add("sidebar-subtitle");

        Button reset = new Button("RESET ALL");
        Button export = new Button("EXPORT PDF");
        reset.getStyleClass().addAll("primary-button", "button-wide");
        export.getStyleClass().addAll("ghost-button", "button-wide");

        reset.setOnAction(e -> resetAll());
        export.setOnAction(e -> exportChartSnapshot());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox box = new VBox(12, title, subtitle, new Separator(), reset, export, spacer);
        box.getStyleClass().add("sidebar");
        box.setPadding(new Insets(20));
        box.setPrefWidth(215);
        box.setMinWidth(200);
        return box;
    }

    private VBox createDashboard() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Device");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Monthly Usage (kWh)");

        usageChart = new BarChart<>(xAxis, yAxis);
        usageChart.setTitle("Monthly Energy Usage by Device");
        usageChart.setLegendVisible(false);
        usageChart.setAnimated(false);
        usageChart.setCategoryGap(24);
        usageChart.setBarGap(6);

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

        VBox.setVgrow(usageChart, Priority.ALWAYS);
        VBox.setVgrow(historyTable, Priority.ALWAYS);

        VBox box = new VBox(12, usageChart, historyTable);
        box.getStyleClass().add("panel");
        box.setPadding(new Insets(14));
        return box;
    }

    private VBox createInputPanel() {
        String shownModel = (this.modelName == null || this.modelName.isBlank()) ? "N/A" : this.modelName;

        lblModel = new Label("Model: " + shownModel);
        lblR2 = new Label(String.format("R2: %.4f", this.r2));
        lblRMSE = new Label(String.format("RMSE: %.4f", this.rmse));

        comboCompany = new ComboBox<>();
        comboCompany.getItems().addAll(deviceLibrary.keySet());
        comboCompany.setPrefWidth(Double.MAX_VALUE);

        comboDevice = new ComboBox<>();
        comboDevice.setPrefWidth(Double.MAX_VALUE);
        comboCompany.setOnAction(e -> updateDeviceList());

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
        lblCarbon = new Label("CO2: 0.00 kg");

        VBox panel = new VBox(
                10,
                lblModel,
                lblR2,
                lblRMSE,
                new Separator(),
                new Label("Input Parameters"),
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
        panel.setPrefWidth(330);
        panel.setMinWidth(300);
        return panel;
    }

    private void addInputRow(GridPane grid, int row, String labelText, Region inputControl) {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        GridPane.setHgrow(inputControl, Priority.ALWAYS);
        grid.add(label, 0, row);
        grid.add(inputControl, 1, row);
    }

    private void updateDeviceList() {
        comboDevice.getItems().clear();
        String company = comboCompany.getValue();
        if (company == null) {
            return;
        }
        comboDevice.getItems().addAll(deviceLibrary.get(company).keySet());
        if (!comboDevice.getItems().isEmpty()) {
            comboDevice.getSelectionModel().selectFirst();
        }
    }

    private void calculate() {
        try {
            validateSelection();

            String company = comboCompany.getValue();
            String device = comboDevice.getValue();

            int rating = deviceLibrary.get(company).get(device);
            int quantity = parsePositiveInt(txtQuantity.getText(), "Quantity");
            double hours = parseHours(txtHours.getText());
            double budgetLimit = parsePositiveDouble(txtBudget.getText(), "Budget");

            EnergyUsageService.PredictionMetrics metrics = usageService.calculate(
                    device,
                    rating,
                    quantity,
                    hours,
                    predictor
            );

            updateResultPanel(metrics, budgetLimit);
            updateUsageChart(device, metrics.adjustedUnits());
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

    private void updateUsageChart(String device, double unitsToAdd) {
        XYChart.Data<String, Number> data = chartDataByDevice.get(device);
        if (data == null) {
            data = new XYChart.Data<>(device, unitsToAdd);
            chartDataByDevice.put(device, data);
            chartSeries.getData().add(data);
            attachTooltip(data, String.format("%s: %.2f kWh", device, unitsToAdd));
        } else {
            double updated = data.getYValue().doubleValue() + unitsToAdd;
            data.setYValue(updated);
            attachTooltip(data, String.format("%s: %.2f kWh", device, updated));
        }
    }

    private void attachTooltip(XYChart.Data<String, Number> data, String text) {
        Tooltip tooltip = new Tooltip(text);
        if (data.getNode() != null) {
            Tooltip.install(data.getNode(), tooltip);
            return;
        }
        data.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                Tooltip.install(newNode, tooltip);
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
        chartSeries.getData().clear();
        chartDataByDevice.clear();
        predictionRows.clear();

        comboCompany.getSelectionModel().clearSelection();
        comboDevice.getItems().clear();

        txtQuantity.setText("1");
        txtHours.setText("5");
        txtBudget.setText("500");

        lblResult.setText("Rs 0.00");
        lblUnits.setText("Monthly Usage: 0.00 kWh");
        lblCarbon.setText("CO2: 0.00 kg");

        budgetBar.setProgress(0);
        budgetBar.getStyleClass().removeAll("budget-safe", "budget-over");
    }

    private void exportChartSnapshot() {
        try {
            WritableImage image = usageChart.snapshot(new SnapshotParameters(), null);
            String ts = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
            File file = new File("usage_report_" + ts + ".png");
            ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(image, null), "png", file);
            showInfo("Analytics exported to: " + file.getAbsolutePath());
        } catch (IOException ex) {
            showWarning("Export failed: " + ex.getMessage());
        }
    }

    private void validateSelection() {
        if (comboCompany.getValue() == null) {
            throw new IllegalArgumentException("Select a company.");
        }
        if (comboDevice.getValue() == null) {
            throw new IllegalArgumentException("Select a device.");
        }
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
        if (hours > 24) {
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
