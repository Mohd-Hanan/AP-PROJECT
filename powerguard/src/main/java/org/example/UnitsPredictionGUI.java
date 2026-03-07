package org.example;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

public class UnitsPredictionGUI {

    public UnitsPredictionGUI(Stage stage) {
        this(stage, () -> new ModeSelectionGUI(stage));
    }

    public UnitsPredictionGUI(Stage stage, Runnable onBack) {
        String bgColor = "#0F0F12";
        String cardColor = "#1C1C22";
        String accentBlue = "#0078FF";
        String hoverBlue = "#0056b3";
        String successGreen = "#00E676";
        String errorRed = "#FF5252";

        VBox root = new VBox(15);
        root.setPadding(new Insets(20, 40, 40, 40));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: " + bgColor + ";");

        Button btnBack = new Button("Back");
        btnBack.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;");
        btnBack.setOnAction(e -> onBack.run());

        HBox navBar = new HBox(btnBack);
        navBar.setAlignment(Pos.CENTER_LEFT);

        Label lblHeader = new Label("UNIT PREDICTION");
        lblHeader.setFont(Font.font("System", FontWeight.BOLD, 26));
        lblHeader.setTextFill(Color.WHITE);

        VBox inputGroup = new VBox(8);
        inputGroup.setAlignment(Pos.CENTER);

        Label lblInputHeader = new Label("Enter Units Consumed:");
        lblInputHeader.setTextFill(Color.web("#BBBBBB"));
        lblInputHeader.setFont(Font.font("System", FontWeight.MEDIUM, 14));

        TextField txtUnits = new TextField();
        txtUnits.setPromptText("e.g. 150");
        txtUnits.setStyle("-fx-background-color: #2D2D35; -fx-text-fill: white; -fx-padding: 12; -fx-background-radius: 8;");
        txtUnits.setMaxWidth(220);

        Label lblError = new Label("Please enter numbers only");
        lblError.setTextFill(Color.web(errorRed));
        lblError.setFont(Font.font("System", 11));
        lblError.setVisible(false);

        txtUnits.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                lblError.setVisible(true);
                txtUnits.setStyle("-fx-background-color: #2D2D35; -fx-text-fill: " + errorRed + "; -fx-border-color: " + errorRed + "; -fx-border-radius: 8; -fx-background-radius: 8;");
            } else {
                lblError.setVisible(false);
                txtUnits.setStyle("-fx-background-color: #2D2D35; -fx-text-fill: white; -fx-background-radius: 8;");
            }
        });

        inputGroup.getChildren().addAll(lblInputHeader, txtUnits, lblError);

        Button btnPredict = new Button("PREDICT BILL");
        btnPredict.setPrefSize(220, 45);
        btnPredict.setStyle("-fx-background-color: " + accentBlue + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");

        btnPredict.setOnMouseEntered(e -> btnPredict.setStyle("-fx-background-color: " + hoverBlue + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;"));
        btnPredict.setOnMouseExited(e -> btnPredict.setStyle("-fx-background-color: " + accentBlue + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;"));

        Label lblStatus = new Label("");
        lblStatus.setFont(Font.font("System", FontWeight.MEDIUM, 13));
        lblStatus.setVisible(false);

        VBox resCard = new VBox(5);
        resCard.setAlignment(Pos.CENTER);
        resCard.setPadding(new Insets(25));
        resCard.setStyle("-fx-background-color: " + cardColor + "; -fx-background-radius: 15; -fx-border-color: #333333; -fx-border-radius: 15;");
        resCard.setMaxWidth(300);

        Text symbol = new Text("Rs ");
        symbol.setFont(Font.font("System", FontWeight.BOLD, 18));
        symbol.setFill(Color.web(successGreen));

        Text value = new Text("0.00");
        value.setFont(Font.font("System", FontWeight.BOLD, 32));
        value.setFill(Color.web(successGreen));

        TextFlow flow = new TextFlow(symbol, value);
        flow.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Label lblSub = new Label("ESTIMATED KSEB BILL");
        lblSub.setTextFill(Color.web("#888888"));
        lblSub.setFont(Font.font("System", 11));
        resCard.getChildren().addAll(flow, lblSub);

        btnPredict.setOnAction(e -> {
            try {
                double units = Double.parseDouble(txtUnits.getText());
                double bill = KSEBBillCalculator.calculate(units);
                value.setText(String.format("%.2f", bill));

                lblStatus.setText("Units Confirmed: " + units + " kWh");
                lblStatus.setTextFill(Color.web(successGreen));
                lblStatus.setVisible(true);
            } catch (Exception ex) {
                lblStatus.setText("Calculation Failed");
                lblStatus.setTextFill(Color.web(errorRed));
                lblStatus.setVisible(true);
            }
        });

        root.getChildren().addAll(navBar, lblHeader, inputGroup, btnPredict, lblStatus, resCard);

        Scene scene = new Scene(root, 600, 500);
        stage.setScene(scene);
        stage.setTitle("PowerGuard | Units Prediction");
        stage.show();
    }
}
