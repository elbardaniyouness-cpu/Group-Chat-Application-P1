package com.chat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ServerModel {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final int port;
    private final Consumer<String> logListener;
    private final Consumer<List<String>> usersListener;
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private final Set<PrintWriter> outputs = ConcurrentHashMap.newKeySet();

    private volatile boolean running;
    private ServerSocket serverSocket;
    private Thread acceptThread;

    public ServerModel(int port, Consumer<String> logListener, Consumer<List<String>> usersListener) {
        this.port = port;
        this.logListener = logListener;
        this.usersListener = usersListener;
    }

    public void start() {
        if (running) {
            return;
        }

        running = true;
        acceptThread = new Thread(this::acceptClients, "chat-server-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public void stop() {
        running = false;
        closeServerSocket();
        for (ClientHandler handler : new ArrayList<>(clients.values())) {
            handler.close();
        }
        clients.clear();
        outputs.clear();
        publishUsers();
        log("Server stopped");
    }

    private void acceptClients() {
        try (ServerSocket socket = new ServerSocket(port)) {
            serverSocket = socket;
            log("Server Started on port " + port);

            while (running) {
                log("Waiting for Client...");
                Socket clientSocket = socket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                Thread thread = new Thread(handler, "client-handler-" + clientSocket.getPort());
                thread.setDaemon(true);
                thread.start();
            }
        } catch (IOException ex) {
            if (running) {
                log("Server error: " + ex.getMessage());
            }
        } finally {
            running = false;
        }
    }

    private void closeServerSocket() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
                // Closing during shutdown does not need a second UI event.
            }
        }
    }

    private void broadcast(String message) {
        for (PrintWriter output : outputs) {
            output.println(message);
        }
    }

    private void sendUsersTo(ClientHandler handler) {
        List<String> users = sortedUsers();
        handler.send("Active users: " + String.join(", ", users));
        log("Active users sent to " + handler.username);
    }

    private void register(ClientHandler handler, String requestedUsername) {
        String username = normalizeUsername(requestedUsername, handler.socket.getPort());
        while (clients.putIfAbsent(username, handler) != null) {
            username = username + "_" + handler.socket.getPort();
        }

        handler.username = username;
        outputs.add(handler.output);
        publishUsers();
        log("Welcome " + username);
        broadcast("System: " + username + " joined the chat");
    }

    private void unregister(ClientHandler handler) {
        if (handler.username != null) {
            clients.remove(handler.username, handler);
            outputs.remove(handler.output);
            publishUsers();
            log(handler.username + " disconnected");
            broadcast("System: " + handler.username + " left the chat");
        }
        handler.close();
    }

    private String normalizeUsername(String requestedUsername, int port) {
        if (requestedUsername == null || requestedUsername.trim().isEmpty()) {
            return "Guest-" + port;
        }
        return requestedUsername.trim();
    }

    private void publishUsers() {
        usersListener.accept(sortedUsers());
    }

    private List<String> sortedUsers() {
        List<String> users = new ArrayList<>(clients.keySet());
        Collections.sort(users);
        return users;
    }

    private void log(String message) {
        logListener.accept(message);
    }

    private final class ClientHandler implements Runnable {
        private final Socket socket;
        private BufferedReader input;
        private PrintWriter output;
        private String username;

        private ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);

                register(this, input.readLine());

                String line;
                while (running && (line = input.readLine()) != null) {
                    String message = line.trim();
                    if (message.isEmpty()) {
                        continue;
                    }
                    if ("allUsers".equalsIgnoreCase(message)) {
                        sendUsersTo(this);
                    } else if ("end".equalsIgnoreCase(message) || "bye".equalsIgnoreCase(message)) {
                        break;
                    } else {
                        broadcast("[" + LocalTime.now().format(TIME_FORMAT) + "] " + username + ": " + message);
                    }
                }
            } catch (IOException ex) {
                if (running) {
                    log("Client error: " + ex.getMessage());
                }
            } finally {
                unregister(this);
            }
        }

        private void send(String message) {
            if (output != null) {
                output.println(message);
            }
        }

        private void close() {
            try {
                socket.close();
            } catch (IOException ignored) {
                // Best-effort client cleanup.
            }
        }
    }
}
