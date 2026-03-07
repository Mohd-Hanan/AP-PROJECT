package org.example;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class ModeSelectionGUI {

    private final Runnable onUnitsSelected;
    private final Runnable onApplianceSelected;
    private final Runnable onLogout;

    public ModeSelectionGUI(Stage stage) {
        this(
                stage,
                () -> new UnitsPredictionGUI(stage, () -> new ModeSelectionGUI(stage), () -> new LoginApp().showLoginPage(stage)),
                () -> new PowerGuardGUI(null, "N/A", 0, 0, () -> new ModeSelectionGUI(stage), () -> new LoginApp().showLoginPage(stage)).start(stage),
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

        ComboBox<String> themeSelector = new ComboBox<>();
        themeSelector.getItems().addAll(ThemeManager.DARK_THEME, ThemeManager.BLUE_THEME);
        themeSelector.getSelectionModel().select(ThemeManager.getCurrentTheme());
        themeSelector.setOnAction(e -> ThemeManager.setCurrentTheme(themeSelector.getValue()));

        root.setTop(TopBar.create(null, this.onLogout, themeSelector));

        VBox center = new VBox(25);
        center.setAlignment(Pos.CENTER);

        Label title = new Label("POWERGUARD");
        title.setStyle("-fx-font-size:40px; -fx-font-weight:bold;");

        Label subtitle = new Label("Mode Selection");
        subtitle.setStyle("-fx-font-size:20px;");

        Button unitBtn = buildModeButton("Predict via Electricity Units");
        Button appBtn = buildModeButton("Predict via Appliance Load");

        unitBtn.setOnAction(e -> onUnitsSelected.run());
        appBtn.setOnAction(e -> onApplianceSelected.run());

        center.getChildren().addAll(title, subtitle, unitBtn, appBtn);
        root.setCenter(center);

        Scene scene = new Scene(root, 600, 500);
        applyTheme(root, title, subtitle, ThemeManager.getCurrentTheme());

        Consumer<String> listener = theme -> {
            if (themeSelector.getValue() == null || !themeSelector.getValue().equals(theme)) {
                themeSelector.getSelectionModel().select(theme);
            }
            applyTheme(root, title, subtitle, theme);
        };
        ThemeManager.addListener(listener);
        stage.setOnHidden(e -> ThemeManager.removeListener(listener));

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

    private void applyTheme(BorderPane root, Label title, Label subtitle, String theme) {
        if (ThemeManager.BLUE_THEME.equals(theme)) {
            root.setStyle("-fx-background-color: linear-gradient(to bottom right, #eaf4ff, #dbeafe);");
            title.setStyle("-fx-font-size:40px; -fx-font-weight:bold; -fx-text-fill:#1f2937;");
            subtitle.setStyle("-fx-font-size:20px; -fx-text-fill:#374151;");
        } else {
            root.setStyle("-fx-background-color:#1e1e1e;");
            title.setStyle("-fx-font-size:40px; -fx-font-weight:bold; -fx-text-fill:white;");
            subtitle.setStyle("-fx-font-size:20px; -fx-text-fill:#d1d5db;");
        }
    }
}
