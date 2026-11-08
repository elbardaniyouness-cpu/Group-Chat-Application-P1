package com.chat.client;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class ClientController {
    private final TextArea chatArea;
    private final TextField inputField;
    private final Circle statusCircle;
    private final Label statusText;
    private final boolean readOnly;
    private final ClientModel model;
    private Stage stage;
    private boolean closing;

    public ClientController(String host, int port, String username, boolean readOnly, TextArea chatArea,
                            TextField inputField, Circle statusCircle, Label statusText) {
        this.chatArea = chatArea;
        this.inputField = inputField;
        this.statusCircle = statusCircle;
        this.statusText = statusText;
        this.readOnly = readOnly;
        this.model = new ClientModel(host, port, username, this::appendMessage, this::setConnected);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void connect() {
        statusCircle.setFill(Color.ORANGE);
        model.connect();
    }

    public void sendCurrentMessage() {
        String message = inputField.getText().trim();
        if (message.isEmpty()) {
            return;
        }

        inputField.clear();
        if (readOnly && !"allUsers".equalsIgnoreCase(message)
                && !"end".equalsIgnoreCase(message) && !"bye".equalsIgnoreCase(message)) {
            appendMessage("System: READ-ONLY MODE prevents sending messages.");
            return;
        }

        model.send(message);
        if ("end".equalsIgnoreCase(message) || "bye".equalsIgnoreCase(message)) {
            disconnectAndClose();
        }
    }

    public void disconnectAndClose() {
        shutdown();
        if (stage != null) {
            Platform.runLater(stage::close);
        }
    }

    public void shutdown() {
        if (closing) {
            return;
        }
        closing = true;
        model.disconnect();
    }

    private void appendMessage(String message) {
        Platform.runLater(() -> chatArea.appendText(message + System.lineSeparator()));
    }

    private void setConnected(Boolean connected) {
        Platform.runLater(() -> {
            if (connected) {
                statusCircle.setFill(readOnly ? Color.RED : Color.GREEN);
                statusText.setText(readOnly ? "Read-Only" : "Online");
            } else {
                statusCircle.setFill(Color.GREY);
                statusText.setText("Disconnected");
            }
        });
    }
}
