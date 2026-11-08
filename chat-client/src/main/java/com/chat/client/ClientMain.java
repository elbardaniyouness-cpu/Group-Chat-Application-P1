package com.chat.client;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

public class ClientMain extends Application {
    private ClientController controller;

    @Override
    public void start(Stage stage) {
        ConnectionSettings settings = loadSettings(getParameters().getRaw().toArray(new String[0]));
        LoginResult login = requestUsername();

        TextArea chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);

        TextField inputField = new TextField();
        inputField.setPromptText("Type message, allUsers, end, or bye");

        Button sendButton = new Button("Send");
        Circle statusCircle = new Circle(7, Color.ORANGE);
        Label statusText = new Label("Connecting");
        HBox status = new HBox(8, new Label("Status"), statusCircle, statusText);
        status.setAlignment(Pos.CENTER_LEFT);

        GridPane root = new GridPane();
        root.setPadding(new Insets(16));
        root.setHgap(8);
        root.setVgap(10);
        root.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-background-color: #f7f8fb;");

        ColumnConstraints messageColumn = new ColumnConstraints();
        messageColumn.setHgrow(Priority.ALWAYS);
        ColumnConstraints buttonColumn = new ColumnConstraints();
        buttonColumn.setPrefWidth(90);
        root.getColumnConstraints().addAll(messageColumn, buttonColumn);

        root.add(status, 0, 0, 2, 1);
        root.add(chatArea, 0, 1, 2, 1);
        root.add(inputField, 0, 2);
        root.add(sendButton, 1, 2);
        GridPane.setHgrow(chatArea, Priority.ALWAYS);
        GridPane.setVgrow(chatArea, Priority.ALWAYS);
        GridPane.setHgrow(inputField, Priority.ALWAYS);

        controller = new ClientController(settings.host, settings.port, login.username, login.readOnly,
                chatArea, inputField, statusCircle, statusText);
        controller.setStage(stage);

        inputField.setOnAction(event -> controller.sendCurrentMessage());
        sendButton.setOnAction(event -> controller.sendCurrentMessage());

        stage.setTitle("TCP Chat Client - " + login.username);
        stage.setScene(new Scene(root, 620, 420));
        stage.setMinWidth(480);
        stage.setMinHeight(320);
        stage.setOnCloseRequest(event -> controller.shutdown());
        stage.show();

        controller.connect();
    }

    private LoginResult requestUsername() {
        TextField usernameField = new TextField();
        usernameField.setPromptText("Leave blank for read-only");

        GridPane pane = new GridPane();
        pane.setPadding(new Insets(12));
        pane.setHgap(8);
        pane.setVgap(8);
        pane.add(new Label("Username:"), 0, 0);
        pane.add(usernameField, 1, 0);

        javafx.scene.control.Dialog<String> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Chat Login");
        dialog.getDialogPane().setContent(pane);
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.OK);
        dialog.setResultConverter(button -> usernameField.getText());

        Optional<String> result = dialog.showAndWait();
        String rawUsername = result.orElse("").trim();
        boolean readOnly = rawUsername.isEmpty();
        String username = readOnly ? "Reader-" + System.currentTimeMillis() % 10000 : rawUsername;
        return new LoginResult(username, readOnly);
    }

    private ConnectionSettings loadSettings(String[] args) {
        Properties properties = new Properties();
        try (InputStream input = ClientMain.class.getResourceAsStream("/config.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException ignored) {
            // Defaults below keep the client runnable when the config is missing.
        }

        String host = args.length > 0 ? args[0] : properties.getProperty("server.ip", "127.0.0.1");
        int port = Integer.parseInt(args.length > 1 ? args[1] : properties.getProperty("server.port", "3000"));
        return new ConnectionSettings(host, port);
    }

    public static void main(String[] args) {
        launch(args);
    }

    private record ConnectionSettings(String host, int port) {
    }

    private record LoginResult(String username, boolean readOnly) {
    }
}
