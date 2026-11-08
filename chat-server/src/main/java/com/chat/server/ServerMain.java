package com.chat.server;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public class ServerMain extends Application {
    private ServerController controller;

    @Override
    public void start(Stage stage) {
        int port = loadPort(getParameters().getRaw().toArray(new String[0]));

        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);

        ObservableList<String> users = ServerController.createUserList();
        ListView<String> userList = new ListView<>(users);
        userList.setCellFactory(list -> new UserCell());

        GridPane root = new GridPane();
        root.setPadding(new Insets(16));
        root.setHgap(12);
        root.setVgap(8);
        root.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-background-color: #f5f7fa;");

        ColumnConstraints logColumn = new ColumnConstraints();
        logColumn.setHgrow(Priority.ALWAYS);
        ColumnConstraints userColumn = new ColumnConstraints();
        userColumn.setPrefWidth(220);
        root.getColumnConstraints().addAll(logColumn, userColumn);

        Label logLabel = new Label("Server Log");
        Label usersLabel = new Label("Active Users");

        root.add(logLabel, 0, 0);
        root.add(usersLabel, 1, 0);
        root.add(logArea, 0, 1);
        root.add(userList, 1, 1);
        GridPane.setHgrow(logArea, Priority.ALWAYS);
        GridPane.setVgrow(logArea, Priority.ALWAYS);
        GridPane.setVgrow(userList, Priority.ALWAYS);

        controller = new ServerController(port, logArea, users);
        controller.startServer();

        stage.setTitle("TCP Chat Server");
        stage.setScene(new Scene(root, 760, 460));
        stage.setMinWidth(620);
        stage.setMinHeight(360);
        stage.setOnCloseRequest(event -> controller.stopServer());
        stage.show();
    }

    private int loadPort(String[] args) {
        Properties properties = new Properties();
        try (InputStream input = ServerMain.class.getResourceAsStream("/config.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException ignored) {
            // Defaults below keep the server runnable when the config is missing.
        }
        return Integer.parseInt(args.length > 0 ? args[0] : properties.getProperty("server.port", "3000"));
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static final class UserCell extends ListCell<String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setStyle("");
                return;
            }

            setText(item);
            int hash = Math.abs(Objects.hash(item));
            int red = 210 + hash % 35;
            int green = 220 + hash / 7 % 30;
            int blue = 230 + hash / 13 % 25;
            Color color = Color.rgb(red, green, blue);
            setStyle("-fx-background-color: " + toRgb(color) + "; -fx-padding: 8; -fx-background-radius: 6;");
        }

        private String toRgb(Color color) {
            return "rgb(" + Math.round(color.getRed() * 255) + ","
                    + Math.round(color.getGreen() * 255) + ","
                    + Math.round(color.getBlue() * 255) + ")";
        }
    }
}
