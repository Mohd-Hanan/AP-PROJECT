package org.example;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class LoginApp
{
    private static final Pattern USER_PATTERN = Pattern.compile("^[a-zA-Z0-9]{5,}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^[a-zA-Z0-9#_]{8,}$");
    private static final String HASH_PREFIX = "sha256:";
    private static final Path USERS_FILE = Path.of(
            System.getProperty("user.home"),
            ".powerguard",
            "users.txt"
    );

    TextField usernameField, regUserField;
    PasswordField passwordField, regPassField;
    Label messageLabel;
    private Runnable loginSuccessCallback;

    public void showLoginPage(Stage stage, Runnable onSuccess) {
        this.loginSuccessCallback = onSuccess;
        showLoginPage(stage);
    }

    public void showLoginPage(Stage stage)
    {

        // LEFT SIDE: SIGN IN
        VBox leftSide = new VBox(20);
        leftSide.setAlignment(Pos.CENTER);
        leftSide.setPadding(new Insets(50));
        leftSide.setPrefWidth(450);
        leftSide.setStyle("-fx-background-color: white;");

        Text loginTitle = new Text("Login to Your Account");
        loginTitle.setFont(Font.font("Arial", FontWeight.BOLD, 28));

        usernameField = new TextField();
        usernameField.setPromptText("Email or Username");
        styleInput(usernameField);

        passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        styleInput(passwordField);

        Button signInBtn = new Button("Sign In");
        signInBtn.setPrefWidth(250);
        signInBtn.setPrefHeight(45);
        signInBtn.setStyle("-fx-background-color: #2ebf91; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 25;");

        messageLabel = new Label();

        leftSide.getChildren().addAll(loginTitle, usernameField, passwordField, signInBtn, messageLabel);

        // RIGHT SIDE: SIGN UP PROMPT
        VBox rightSide = new VBox(20);
        rightSide.setAlignment(Pos.CENTER);
        rightSide.setPadding(new Insets(50));
        rightSide.setPrefWidth(350);
        rightSide.setStyle("-fx-background-color: linear-gradient(to bottom right, #2ebf91, #8360c3);");

        Text welcomeTitle = new Text("New Here?");
        welcomeTitle.setFill(Color.WHITE);
        welcomeTitle.setFont(Font.font("Arial", FontWeight.BOLD, 32));

        Label desc = new Label("Sign up and discover a great\namount of new opportunities!");
        desc.setTextFill(Color.WHITE);
        desc.setStyle("-fx-text-alignment: center;");

        Button signUpBtn = new Button("Sign Up");
        signUpBtn.setPrefWidth(180);
        signUpBtn.setStyle("-fx-background-color: transparent; -fx-border-color: white; -fx-border-radius: 20; -fx-text-fill: white;");

        rightSide.getChildren().addAll(welcomeTitle, desc, signUpBtn);

        HBox mainRoot = new HBox(leftSide, rightSide);
        stage.setScene(new Scene(mainRoot, 800, 500));
        stage.setTitle("EnergyPredict - Login");
        stage.show();

        // Actions
        signInBtn.setOnAction(e -> loginUser());
        signUpBtn.setOnAction(e -> showRegisterPage(stage));
    }

    public void showRegisterPage(Stage stage)
    {
        // --- LEFT SIDE: WELCOME BACK PANEL (The colorful part now on the left) ---
        VBox leftSide = new VBox(20);
        leftSide.setAlignment(Pos.CENTER);
        leftSide.setPadding(new Insets(50));
        leftSide.setPrefWidth(350);
        leftSide.setStyle("-fx-background-color: linear-gradient(to bottom left, #8360c3, #2ebf91);");

        Text backTitle = new Text("One of us?");
        backTitle.setFill(Color.WHITE);
        backTitle.setFont(Font.font("Arial", FontWeight.BOLD, 32));

        Label backDesc = new Label("If you already have an account,\njust sign in. We've missed you!");
        backDesc.setTextFill(Color.WHITE);
        backDesc.setStyle("-fx-text-alignment: center;");

        Button backToLoginBtn = new Button("Sign In");
        backToLoginBtn.setPrefWidth(180);
        backToLoginBtn.setStyle("-fx-background-color: transparent; -fx-border-color: white; -fx-border-radius: 20; -fx-text-fill: white;");

        leftSide.getChildren().addAll(backTitle, backDesc, backToLoginBtn);

        // --- RIGHT SIDE: REGISTER FORM ---
        VBox rightSide = new VBox(20);
        rightSide.setAlignment(Pos.CENTER);
        rightSide.setPadding(new Insets(50));
        rightSide.setPrefWidth(450);
        rightSide.setStyle("-fx-background-color: white;");

        Text regTitle = new Text("Create Account");
        regTitle.setFont(Font.font("Arial", FontWeight.BOLD, 28));

        regUserField = new TextField();
        regUserField.setPromptText("Username");
        styleInput(regUserField);

        regPassField = new PasswordField();
        regPassField.setPromptText("Password");
        styleInput(regPassField);

        Button registerBtn = new Button("Sign Up");
        registerBtn.setPrefWidth(250);
        registerBtn.setPrefHeight(45);
        registerBtn.setStyle("-fx-background-color: #8360c3; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 25;");

        messageLabel = new Label(); // Reusing the message label for errors

        rightSide.getChildren().addAll(regTitle, regUserField, regPassField, registerBtn, messageLabel);

        // --- MAIN LAYOUT (800x500 to match Login) ---
        HBox mainRoot = new HBox(leftSide, rightSide);
        stage.setScene(new Scene(mainRoot, 800, 500));

        // Actions
        backToLoginBtn.setOnAction(e -> showLoginPage(stage));
        registerBtn.setOnAction(e -> handleRegistration(stage));
    }

    private void handleRegistration(Stage stage)
    {
        String user = regUserField.getText().trim();
        String pass = regPassField.getText().trim();

        if (user.isEmpty() || pass.isEmpty())
        {
            setErrorMessage("Error: All fields are required!");
            return;
        }

        if (!USER_PATTERN.matcher(user).matches())
        {
            setErrorMessage("Username must be at least 5 characters and only use letters/numbers.");
            return;
        }

        if (!PASSWORD_PATTERN.matcher(pass).matches())
        {
            setErrorMessage("Password must be at least 8 chars and may include letters, numbers, # and _.");
            return;
        }

        try
        {
            ensureUserStoreExists();
            Map<String, String> users = loadUsers();
            if (users.containsKey(user))
            {
                setErrorMessage("This username already exists. Choose a different one.");
                return;
            }

            String payload = user + "," + HASH_PREFIX + hashPassword(pass) + System.lineSeparator();
            Files.writeString(USERS_FILE, payload, StandardCharsets.UTF_8, StandardOpenOption.APPEND);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(null);
            alert.setContentText("Account Created Successfully!");
            alert.showAndWait();
            showLoginPage(stage);
        } catch (IOException e)
        {
            setErrorMessage("System Error: Could not save account.");
        }
    }

    private void loginUser()
    {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText().trim();

        if (user.isEmpty() || pass.isEmpty())
        {
            setErrorMessage("Error: Both ID and Password are required!");
            return;
        }
        if (user.length() < 5 || pass.length() < 8)
        {
            setErrorMessage("Invalid credentials. Check length requirements.");
            return;
        }

        try
        {
            ensureUserStoreExists();
            Map<String, String> users = loadUsers();
            if (verifyPassword(pass, users.get(user)))
            {
                setSuccessMessage("Success!");
                Stage stage = (Stage) usernameField.getScene().getWindow();
                if (loginSuccessCallback != null) {
                    loginSuccessCallback.run();
                } else {
                    new ModeSelectionGUI(stage);
                }
                return;
            }
            setErrorMessage("Invalid Username or Password.");
        } catch (Exception e)
        {
            setErrorMessage("User database error.");
        }
    }

    private void styleInput(TextField field)
    {
        field.setPrefHeight(40);
        field.setMaxWidth(300);
        field.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 10; -fx-padding: 10;");
    }

    private void setErrorMessage(String text) {
        messageLabel.setText(text);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(350);
        messageLabel.setAlignment(Pos.CENTER);
        messageLabel.setStyle("-fx-text-fill: #e74c3c; -fx-text-alignment: center; -fx-font-weight: bold;");
    }

    private void setSuccessMessage(String text) {
        messageLabel.setText(text);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(350);
        messageLabel.setAlignment(Pos.CENTER);
        messageLabel.setStyle("-fx-text-fill: #27ae60; -fx-text-alignment: center; -fx-font-weight: bold;");
    }

    private void ensureUserStoreExists() throws IOException {
        Files.createDirectories(USERS_FILE.getParent());
        if (Files.exists(USERS_FILE)) {
            return;
        }

        try (InputStream in = getClass().getResourceAsStream("/users.txt")) {
            if (in != null) {
                Files.copy(in, USERS_FILE, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.createFile(USERS_FILE);
            }
        }
    }

    private Map<String, String> loadUsers() throws IOException {
        Map<String, String> users = new LinkedHashMap<>();
        List<String> lines = Files.readAllLines(USERS_FILE, StandardCharsets.UTF_8);
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            String[] data = line.split(",", 2);
            if (data.length == 2) {
                users.put(data[0].trim(), data[1].trim());
            }
        }
        return users;
    }

    private boolean verifyPassword(String rawPassword, String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            return false;
        }
        if (storedValue.startsWith(HASH_PREFIX)) {
            return storedValue.equals(HASH_PREFIX + hashPassword(rawPassword));
        }
        return storedValue.equals(rawPassword);
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
