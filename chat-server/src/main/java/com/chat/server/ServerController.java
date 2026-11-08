package com.chat.server;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TextArea;

import java.util.List;

public class ServerController {
    private final TextArea logArea;
    private final ObservableList<String> activeUsers;
    private final ServerModel model;

    public ServerController(int port, TextArea logArea, ObservableList<String> activeUsers) {
        this.logArea = logArea;
        this.activeUsers = activeUsers;
        this.model = new ServerModel(port, this::appendLog, this::replaceUsers);
    }

    public static ObservableList<String> createUserList() {
        return FXCollections.observableArrayList();
    }

    public void startServer() {
        model.start();
    }

    public void stopServer() {
        model.stop();
    }

    private void appendLog(String message) {
        Platform.runLater(() -> logArea.appendText(message + System.lineSeparator()));
    }

    private void replaceUsers(List<String> users) {
        Platform.runLater(() -> activeUsers.setAll(users));
    }
}
