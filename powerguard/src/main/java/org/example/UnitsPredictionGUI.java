package org.example;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class UnitsPredictionGUI {

    public UnitsPredictionGUI(Stage stage) {
        this(stage, () -> new ModeSelectionGUI(stage), () -> new LoginApp().showLoginPage(stage));
    }

    public UnitsPredictionGUI(Stage stage, Runnable onBack) {
        this(stage, onBack, () -> new LoginApp().showLoginPage(stage));
    }

    public UnitsPredictionGUI(Stage stage, Runnable onBack, Runnable onLogout) {
        String accentBlue = "#0078FF";
        String hoverBlue = "#0056b3";
        String successGreen = "#00E676";
        String errorRed = "#FF5252";

        BorderPane shell = new BorderPane();

        ComboBox<String> themeSelector = new ComboBox<>();
        themeSelector.getItems().addAll(ThemeManager.DARK_THEME, ThemeManager.BLUE_THEME);
        themeSelector.getSelectionModel().select(ThemeManager.getCurrentTheme());
        themeSelector.setOnAction(e -> ThemeManager.setCurrentTheme(themeSelector.getValue()));

        shell.setTop(TopBar.create(onBack, onLogout, themeSelector));

        VBox root = new VBox(15);
        root.setPadding(new Insets(20, 40, 40, 40));
        root.setAlignment(Pos.TOP_CENTER);

        Label lblHeader = new Label("UNIT PREDICTION");
        lblHeader.setFont(Font.font("System", FontWeight.BOLD, 26));

        VBox inputGroup = new VBox(8);
        inputGroup.setAlignment(Pos.CENTER);

        Label lblInputHeader = new Label("Enter Units Consumed:");
        lblInputHeader.setFont(Font.font("System", FontWeight.MEDIUM, 14));

        TextField txtUnits = new TextField();
        txtUnits.setPromptText("e.g. 150");
        txtUnits.setStyle("-fx-padding: 12; -fx-background-radius: 8;");
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
                applyInputStyle(txtUnits, ThemeManager.getCurrentTheme());
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
        resCard.setStyle("-fx-background-radius: 15; -fx-border-color: #333333; -fx-border-radius: 15;");
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

        root.getChildren().addAll(lblHeader, inputGroup, btnPredict, lblStatus, resCard);
        shell.setCenter(root);

        Scene scene = new Scene(shell, 600, 500);
        applyTheme(root, lblHeader, lblInputHeader, lblSub, resCard, txtUnits, ThemeManager.getCurrentTheme());
        Consumer<String> listener = theme -> {
            if (themeSelector.getValue() == null || !themeSelector.getValue().equals(theme)) {
                themeSelector.getSelectionModel().select(theme);
            }
            applyTheme(root, lblHeader, lblInputHeader, lblSub, resCard, txtUnits, theme);
        };
        ThemeManager.addListener(listener);
        stage.setOnHidden(e -> ThemeManager.removeListener(listener));

        stage.setScene(scene);
        stage.setTitle("PowerGuard | Units Prediction");
        stage.show();
    }

    private void applyTheme(
            VBox root,
            Label header,
            Label inputHeader,
            Label sub,
            VBox resCard,
            TextField txtUnits,
            String theme
    ) {
        if (ThemeManager.BLUE_THEME.equals(theme)) {
            root.setStyle("-fx-background-color: linear-gradient(to bottom right, #eff6ff, #dbeafe);");
            header.setTextFill(Color.web("#1f2937"));
            inputHeader.setTextFill(Color.web("#374151"));
            sub.setTextFill(Color.web("#4b5563"));
            resCard.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 15; -fx-border-color: #bfdbfe; -fx-border-radius: 15;");
        } else {
            root.setStyle("-fx-background-color: #0F0F12;");
            header.setTextFill(Color.WHITE);
            inputHeader.setTextFill(Color.web("#BBBBBB"));
            sub.setTextFill(Color.web("#888888"));
            resCard.setStyle("-fx-background-color: #1C1C22; -fx-background-radius: 15; -fx-border-color: #333333; -fx-border-radius: 15;");
        }
        applyInputStyle(txtUnits, theme);
    }

    private void applyInputStyle(TextField field, String theme) {
        if (ThemeManager.BLUE_THEME.equals(theme)) {
            field.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #111827; -fx-padding: 12; -fx-background-radius: 8;");
        } else {
            field.setStyle("-fx-background-color: #2D2D35; -fx-text-fill: white; -fx-padding: 12; -fx-background-radius: 8;");
        }
    }
}
