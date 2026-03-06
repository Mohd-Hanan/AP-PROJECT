package org.example;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
public class PowerGuardGUI extends Application {

    private static LinearRegressionModel predictor;
    private BarChart<String, Number> chart;
    private Map<String, Map<String, Integer>> deviceLibrary = new HashMap<>();
    private ProgressBar budgetBar;
    private ComboBox<String> comboCompany;
    private ComboBox<String> comboDevice;
    private TextField txtHours;
    private TextField txtQuantity;
    private TextField txtBudget;
    private Label lblResult;
    private Label lblCarbon;
    private Label lblModel;
    private Label lblR2;
    private Label lblRMSE;
    private TableView<PredictionResult> historyTable;
    private XYChart.Series<String, Number> chartSeries;
    public PowerGuardGUI() {}

    public PowerGuardGUI(LinearRegressionModel predictor) {
        this.predictor = predictor;
    }

    @Override
    public void start(Stage stage) {

        initializeData();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        root.setLeft(createSidebar());
        root.setCenter(createDashboard());
        root.setRight(createInputPanel());

        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(
                getClass().getResource("/style.css").toExternalForm()
        );

        stage.setTitle("PowerGuard AI Energy Predictor");
        stage.setScene(scene);
        stage.show();
    }
    private String modelName;
    private double r2;
    private double rmse;

    public PowerGuardGUI(LinearRegressionModel predictor,
                         String modelName,
                         double r2,
                         double rmse) {

        this.predictor = predictor;
        this.modelName = modelName;
        this.r2 = r2;
        this.rmse = rmse;
    }
    private void initializeData() {

        Map<String,Integer> samsung = new HashMap<>();
        samsung.put("Smart Refrigerator",400);
        samsung.put("Inverter AC",1500);

        Map<String,Integer> sony = new HashMap<>();
        sony.put("PlayStation 5",200);
        sony.put("Bravia TV",180);

        Map<String,Integer> tesla = new HashMap<>();
        tesla.put("Wall Connector (11.5kW)",11500);

        deviceLibrary.put("Tesla (EV)",tesla);
        deviceLibrary.put("Samsung",samsung);
        deviceLibrary.put("Sony",sony);
    }

    private VBox createSidebar(){

        Label title = new Label("POWERGUARD");
        title.setStyle("-fx-font-size:20px;-fx-text-fill:#2f81f7;");

        Button reset = new Button("RESET ALL");
        Button export = new Button("EXPORT PDF");

        VBox box = new VBox(20,title,reset,export);
        box.getStyleClass().add("sidebar");
        box.setPadding(new Insets(20));
        box.setPrefWidth(200);
        reset.setOnAction(e -> {
            if(chartSeries != null) chartSeries.getData().clear();
            if(historyTable != null) historyTable.getItems().clear();

        });
        // Inside createSidebar()
        export.setOnAction(e -> {
            try {
                // Capture the chart as an image
                WritableImage image = chart.snapshot(new SnapshotParameters(), null);
                File file = new File("usage_report.png");

                // Convert and save
                javax.imageio.ImageIO.write(
                        javafx.embed.swing.SwingFXUtils.fromFXImage(image, null),
                        "png",
                        file
                );

                // Show Success Alert
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText(null);
                alert.setContentText("Analytics exported to: " + file.getAbsolutePath());
                alert.showAndWait();

            } catch (java.io.IOException ex) {
                ex.printStackTrace();
            }
        });
        return box;
    }

    private VBox createDashboard(){

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();

        chart = new BarChart<>(xAxis,yAxis);
        chart.setTitle("Usage Analytics Engine");

        chartSeries = new XYChart.Series<>();
        chart.getData().add(chartSeries);
        chart.setAnimated(true);
        chart.setCategoryGap(30);
        historyTable = new TableView<>();

        TableColumn<PredictionResult,String> deviceCol =
                new TableColumn<>("DEVICE");
        deviceCol.setCellValueFactory(data -> data.getValue().deviceProperty());

        TableColumn<PredictionResult,String> costCol =
                new TableColumn<>("COST");
        costCol.setCellValueFactory(data -> data.getValue().costProperty());

        TableColumn<PredictionResult,String> carbonCol =
                new TableColumn<>("CARBON");
        carbonCol.setCellValueFactory(data -> data.getValue().carbonProperty());

        TableColumn<PredictionResult,String> statusCol =
                new TableColumn<>("STATUS");
        statusCol.setCellValueFactory(data -> data.getValue().statusProperty());

        historyTable.getColumns().addAll(
                deviceCol, costCol, carbonCol, statusCol
        );

        historyTable.setPrefHeight(300);

        VBox box = new VBox(20, chart, historyTable);
        box.getStyleClass().add("panel");

        return box;
    }

    private VBox createInputPanel(){

        lblModel = new Label("Model: " + this.modelName);
        lblR2 = new Label(String.format("R²: %.4f", this.r2));
        lblRMSE = new Label(String.format("RMSE: %.4f", this.rmse));

        comboCompany = new ComboBox<>();
        comboCompany.getItems().addAll(deviceLibrary.keySet());

        comboDevice = new ComboBox<>();
        comboCompany.setOnAction(e -> updateDeviceList());

        txtQuantity = new TextField("1");
        txtHours = new TextField("5");
        txtBudget = new TextField("500");

        budgetBar = new ProgressBar(0);   // ✅ moved here

        Button predict = new Button("PREDICT BILL");
        predict.setPrefWidth(200);
        predict.setOnAction(e -> calculate());

        lblResult = new Label("₹0");
        lblResult.setStyle("-fx-font-size:22px;-fx-text-fill:#ff4d4f;");

        lblCarbon = new Label("0 kg CO2");

        VBox panel = new VBox(12,
                lblModel,
                lblR2,
                lblRMSE,
                new Separator(),
                new Label("Company"),comboCompany,
                new Label("Device"),comboDevice,
                new Label("Quantity"),txtQuantity,
                new Label("Daily Hours"),txtHours,
                new Label("Limit (₹)"),txtBudget,
                budgetBar,
                predict,
                lblResult,
                lblCarbon
        );

        panel.getStyleClass().add("panel");
        panel.setPrefWidth(260);

        return panel;
    }

    private void updateDeviceList() {

        comboDevice.getItems().clear();

        String company = comboCompany.getValue();

        if(company!=null)
            comboDevice.getItems().addAll(deviceLibrary.get(company).keySet());
    }

    private void calculate() {
        if(comboCompany.getValue()==null || comboDevice.getValue()==null){
            return;
        }
        try{

            String company = comboCompany.getValue();
            String device = comboDevice.getValue();

            int rating = deviceLibrary.get(company).get(device);

            int quantity = Integer.parseInt(txtQuantity.getText());
            double hours = Double.parseDouble(txtHours.getText());

            double hourlyKW = (rating/1000.0)*quantity;

            double predictedUnits = predictor.predictUnits(hourlyKW);
            double totalUnits = predictedUnits * hours;

            double cost = KSEBBillCalculator.calculate(totalUnits);

            double carbon = totalUnits * 0.85;

            lblResult.setText("₹"+String.format("%.2f",cost));
            lblCarbon.setText(String.format("%.2f kg CO2",carbon));

            int index = chartSeries.getData().size()+1;

            chartSeries.getData().add(
                    new XYChart.Data<>(device + " ("+index+")",cost)
            );

            double budget = Double.parseDouble(txtBudget.getText());

            budgetBar.setProgress(cost / budget);

            if(cost > budget)
                budgetBar.setStyle("-fx-accent:red;");
            else
                budgetBar.setStyle("-fx-accent:green;");

            historyTable.getItems().add(
                    new PredictionResult(
                            device,
                            String.format("₹%.2f",cost),
                            String.format("%.2f kg",carbon),
                            cost > budget ? "OVER" : "SAFE"
                    )
            );

        }catch(Exception e){
            System.out.println(e.getMessage());
        }
    }

}