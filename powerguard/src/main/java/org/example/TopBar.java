package org.example;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public final class TopBar {

    private TopBar() {
    }

    public static HBox create(
            Runnable onBack,
            Runnable onLogout
    ) {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("top-bar");

        if (onBack != null) {
            Button backBtn = new Button("Back");
            backBtn.getStyleClass().addAll("top-button", "back-button");
            backBtn.setOnAction(e -> onBack.run());
            bar.getChildren().add(backBtn);
        }

        Button themeBtn = new Button("T");
        themeBtn.getStyleClass().add("theme-toggle-button");
        themeBtn.setOnAction(e -> ThemeManager.toggleTheme());
        bar.getChildren().add(themeBtn);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        bar.getChildren().add(spacer);

        if (onLogout != null) {
            Button logoutBtn = new Button("Logout");
            logoutBtn.getStyleClass().addAll("top-button", "logout-button");
            logoutBtn.setOnAction(e -> onLogout.run());
            bar.getChildren().add(logoutBtn);
        }

        return bar;
    }
}
