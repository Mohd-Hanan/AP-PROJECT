package org.example;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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
        root.getStyleClass().add("mode-root");
        root.setTop(TopBar.create(null, this.onLogout));

        VBox center = new VBox(25);
        center.setAlignment(Pos.CENTER);
        center.getStyleClass().add("mode-center");

        Label title = new Label("POWERGUARD");
        title.getStyleClass().add("mode-title");

        Label subtitle = new Label("Mode Selection");
        subtitle.getStyleClass().add("mode-subtitle");

        Button unitBtn = buildModeButton("Predict via Electricity Units");
        Button appBtn = buildModeButton("Predict via Appliance Load");

        unitBtn.setOnAction(e -> onUnitsSelected.run());
        appBtn.setOnAction(e -> onApplianceSelected.run());

        center.getChildren().addAll(title, subtitle, unitBtn, appBtn);
        root.setCenter(center);

        Scene scene = new Scene(root, 1280, 800);
        applyTheme(scene, ThemeManager.getCurrentTheme());

        Consumer<String> listener = theme -> applyTheme(scene, theme);
        ThemeManager.addListener(listener);
        stage.setOnHidden(e -> ThemeManager.removeListener(listener));

        stage.setTitle("PowerGuard");
        stage.setWidth(1280);
        stage.setHeight(800);
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    private Button buildModeButton(String text) {
        Button button = new Button(text);
        button.setPrefWidth(300);
        button.setPrefHeight(50);
        button.getStyleClass().add("mode-button");
        return button;
    }

    private void applyTheme(Scene scene, String theme) {
        String base = getClass().getResource("/style.css").toExternalForm();
        String themed = ThemeManager.BLUE_THEME.equals(theme)
                ? getClass().getResource("/blue-theme.css").toExternalForm()
                : getClass().getResource("/dark-theme.css").toExternalForm();

        scene.getStylesheets().setAll(base, themed);
    }
}
