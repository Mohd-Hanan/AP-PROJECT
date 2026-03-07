package org.example;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;

public class ModeSelectionGUI {

    public static boolean isDarkMode = false;

    private final Runnable onUnitsSelected;
    private final Runnable onApplianceSelected;
    private final Runnable onLogout;

    public ModeSelectionGUI(Stage stage) {
        this(
                stage,
                () -> new UnitsPredictionGUI(stage),
                () -> new PowerGuardGUI().start(stage),
                () -> new LoginApp().showLoginPage(stage)
        );
    }

    public ModeSelectionGUI(
            Stage stage,
            Runnable onUnitsSelected,
            Runnable onApplianceSelected,
            Runnable onLogout
    ) {
        this.onUnitsSelected = onUnitsSelected;
        this.onApplianceSelected = onApplianceSelected;
        this.onLogout = onLogout;

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: white;");

        Button themeBtn = new Button("Theme");
        themeBtn.setStyle(
                "-fx-background-color:#ecf0f1;" +
                        "-fx-font-size:14px;"
        );

        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle(
                "-fx-background-color:#e74c3c;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-size:14px;" +
                        "-fx-background-radius:6;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(10, themeBtn, spacer, logoutBtn);
        topBar.setAlignment(Pos.CENTER);
        topBar.setStyle("-fx-padding:10 20 10 20;");
        root.setTop(topBar);

        VBox center = new VBox(25);
        center.setAlignment(Pos.CENTER);

        Label title = new Label("POWERGUARD");
        title.setStyle(
                "-fx-font-size:40px;" +
                        "-fx-font-weight:bold;" +
                        "-fx-text-fill:#2c3e50;"
        );

        Label subtitle = new Label("Mode Selection");
        subtitle.setStyle(
                "-fx-font-size:20px;" +
                        "-fx-text-fill:#555;"
        );

        Button unitBtn = buildModeButton("Predict via Electricity Units");
        Button appBtn = buildModeButton("Predict via Appliance Load");

        center.getChildren().addAll(title, subtitle, unitBtn, appBtn);
        root.setCenter(center);

        themeBtn.setOnAction(e -> {
            if (isDarkMode) {
                root.setStyle("-fx-background-color:white;");
                title.setStyle("-fx-font-size:40px;-fx-font-weight:bold;-fx-text-fill:#2c3e50;");
                subtitle.setStyle("-fx-font-size:20px;-fx-text-fill:#555;");
            } else {
                root.setStyle("-fx-background-color:#1e1e1e;");
                title.setStyle("-fx-font-size:40px;-fx-font-weight:bold;-fx-text-fill:white;");
                subtitle.setStyle("-fx-font-size:20px;-fx-text-fill:white;");
            }
            isDarkMode = !isDarkMode;
        });

        logoutBtn.setOnAction(e -> onLogout.run());
        unitBtn.setOnAction(e -> onUnitsSelected.run());
        appBtn.setOnAction(e -> onApplianceSelected.run());

        Scene scene = new Scene(root, 600, 500);
        URL style = getClass().getResource("/style2.css");
        if (style != null) {
            scene.getStylesheets().add(style.toExternalForm());
        }

        stage.setTitle("PowerGuard");
        stage.setScene(scene);
        stage.show();
    }

    private Button buildModeButton(String text) {
        Button button = new Button(text);
        button.setPrefWidth(300);
        button.setPrefHeight(50);
        button.setStyle(
                "-fx-background-color:#3498db;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-size:16px;" +
                        "-fx-background-radius:8;"
        );
        return button;
    }
}
