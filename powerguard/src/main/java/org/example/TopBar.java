package org.example;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public final class TopBar {

    private TopBar() {
    }

    public static HBox create(
            Runnable onBack,
            Runnable onLogout,
            ComboBox<String> themeSelector
    ) {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-padding:10 20 10 20;");

        if (onBack != null) {
            Button backBtn = new Button("Back");
            backBtn.setStyle("-fx-background-color:#95a5a6; -fx-text-fill:white; -fx-background-radius:6;");
            backBtn.setOnAction(e -> onBack.run());
            bar.getChildren().add(backBtn);
        }

        if (themeSelector != null) {
            themeSelector.setPrefWidth(180);
            bar.getChildren().add(themeSelector);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        bar.getChildren().add(spacer);

        if (onLogout != null) {
            Button logoutBtn = new Button("Logout");
            logoutBtn.setStyle("-fx-background-color:#e74c3c; -fx-text-fill:white; -fx-background-radius:6;");
            logoutBtn.setOnAction(e -> onLogout.run());
            bar.getChildren().add(logoutBtn);
        }

        return bar;
    }
}
