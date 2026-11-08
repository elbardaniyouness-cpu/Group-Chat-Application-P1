package com.chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

public class ClientModel {
    private final String host;
    private final int port;
    private final String username;
    private final Consumer<String> messageListener;
    private final Consumer<Boolean> connectionListener;

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private Thread readerThread;
    private volatile boolean connected;

    public ClientModel(String host, int port, String username, Consumer<String> messageListener,
                       Consumer<Boolean> connectionListener) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.messageListener = messageListener;
        this.connectionListener = connectionListener;
    }

    public void connect() {
        readerThread = new Thread(this::runReader, "chat-client-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    public void send(String message) {
        if (connected && output != null) {
            output.println(message);
        }
    }

    public void disconnect() {
        connected = false;
        if (output != null) {
            output.println("bye");
        }
        closeSocket();
        connectionListener.accept(false);
    }

    private void runReader() {
        try {
            socket = new Socket(host, port);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            connected = true;
            output.println(username);
            connectionListener.accept(true);

            String line;
            while (connected && (line = input.readLine()) != null) {
                messageListener.accept(line);
            }
        } catch (IOException ex) {
            messageListener.accept("System: Connection error - " + ex.getMessage());
        } finally {
            connected = false;
            closeSocket();
            connectionListener.accept(false);
        }
    }

    private void closeSocket() {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // Best-effort socket cleanup.
            }
        }
    }
}
