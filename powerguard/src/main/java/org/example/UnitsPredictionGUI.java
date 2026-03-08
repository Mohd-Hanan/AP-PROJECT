package org.example;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
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
        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("units-root");
        shell.setTop(TopBar.create(onBack, onLogout));

        VBox center = new VBox(15);
        center.setPadding(new Insets(20, 40, 40, 40));
        center.setAlignment(Pos.TOP_CENTER);
        center.getStyleClass().add("units-center");

        Label header = new Label("UNIT PREDICTION");
        header.getStyleClass().add("units-header");

        Label inputHeader = new Label("Enter Units Consumed:");
        inputHeader.getStyleClass().add("units-input-header");

        TextField txtUnits = new TextField();
        txtUnits.setPromptText("e.g. 150");
        txtUnits.setMaxWidth(220);
        txtUnits.getStyleClass().add("units-input");

        Label error = new Label("Please enter numbers only");
        error.getStyleClass().add("units-error");
        error.setVisible(false);

        VBox inputGroup = new VBox(8, inputHeader, txtUnits, error);
        inputGroup.setAlignment(Pos.CENTER);

        Button predict = new Button("PREDICT BILL");
        predict.setPrefSize(220, 45);
        predict.getStyleClass().addAll("primary-button", "units-predict-button");

        Label status = new Label("");
        status.getStyleClass().add("units-status");
        status.setVisible(false);

        Label amount = new Label("Rs 0.00");
        amount.getStyleClass().add("units-amount");

        Label sub = new Label("ESTIMATED KSEB BILL");
        sub.getStyleClass().add("units-sub");

        VBox resultCard = new VBox(5, amount, sub);
        resultCard.setAlignment(Pos.CENTER);
        resultCard.setPadding(new Insets(25));
        resultCard.setMaxWidth(300);
        resultCard.getStyleClass().add("units-result-card");

        txtUnits.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean valid = newVal != null && newVal.matches("\\d*(\\.\\d*)?");
            error.setVisible(!valid);
            txtUnits.getStyleClass().remove("units-input-error");
            if (!valid) {
                txtUnits.getStyleClass().add("units-input-error");
            }
        });

        predict.setOnAction(e -> {
            try {
                double units = Double.parseDouble(txtUnits.getText());
                double bill = KSEBBillCalculator.calculate(units);
                amount.setText(String.format("Rs %.2f", bill));
                status.setText("Units Confirmed: " + units + " kWh");
                status.getStyleClass().removeAll("status-error", "status-success");
                status.getStyleClass().add("status-success");
                status.setVisible(true);
            } catch (Exception ex) {
                status.setText("Calculation Failed");
                status.getStyleClass().removeAll("status-error", "status-success");
                status.getStyleClass().add("status-error");
                status.setVisible(true);
            }
        });

        center.getChildren().addAll(header, inputGroup, predict, status, resultCard);
        shell.setCenter(center);

        Scene scene = new Scene(shell, 1280, 800);
        applyTheme(scene, ThemeManager.getCurrentTheme());
        Consumer<String> listener = theme -> applyTheme(scene, theme);
        ThemeManager.addListener(listener);
        stage.setOnHidden(e -> ThemeManager.removeListener(listener));

        stage.setScene(scene);
        stage.setTitle("PowerGuard | Units Prediction");
        stage.setWidth(1280);
        stage.setHeight(800);
        stage.setResizable(false);
        stage.show();
    }

    private void applyTheme(Scene scene, String theme) {
        String base = getClass().getResource("/style.css").toExternalForm();
        String themed = ThemeManager.BLUE_THEME.equals(theme)
                ? getClass().getResource("/blue-theme.css").toExternalForm()
                : getClass().getResource("/dark-theme.css").toExternalForm();
        scene.getStylesheets().setAll(base, themed);
    }
}
