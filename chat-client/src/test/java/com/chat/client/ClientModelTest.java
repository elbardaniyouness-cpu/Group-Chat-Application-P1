package com.chat.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests ClientModel against a fake server socket owned by the test, so the client module
 * can be tested on its own. The callbacks feed queues the test reads from.
 */
class ClientModelTest {
    private static final int TIMEOUT_MS = 3000;

    private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
    private final BlockingQueue<Boolean> connectionStates = new LinkedBlockingQueue<>();
    private ServerSocket fakeServer;

    @BeforeEach
    void openFakeServer() throws IOException {
        fakeServer = new ServerSocket(0);
        fakeServer.setSoTimeout(TIMEOUT_MS);
    }

    @AfterEach
    void closeFakeServer() throws IOException {
        fakeServer.close();
    }

    @Test
    void sendsUsernameFirstAndReportsConnected() throws Exception {
        ClientModel client = newClient("alice");
        client.connect();

        try (Socket server = fakeServer.accept()) {
            BufferedReader fromClient = reader(server);
            assertEquals("alice", fromClient.readLine());
            assertEquals(Boolean.TRUE, connectionStates.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    void deliversEachServerLineToListener() throws Exception {
        ClientModel client = newClient("alice");
        client.connect();

        try (Socket server = fakeServer.accept()) {
            reader(server).readLine(); // username
            PrintWriter toClient = new PrintWriter(server.getOutputStream(), true);
            toClient.println("System: alice joined the chat");
            toClient.println("[10:15] bob: hi");

            assertEquals("System: alice joined the chat", messages.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertEquals("[10:15] bob: hi", messages.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    void sendForwardsMessageToServer() throws Exception {
        ClientModel client = newClient("alice");
        client.connect();

        try (Socket server = fakeServer.accept()) {
            BufferedReader fromClient = reader(server);
            fromClient.readLine(); // username
            connectionStates.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS); // wait until connected

            client.send("allUsers");

            assertEquals("allUsers", fromClient.readLine());
        }
    }

    @Test
    void disconnectSendsByeAndReportsDisconnected() throws Exception {
        ClientModel client = newClient("alice");
        client.connect();

        try (Socket server = fakeServer.accept()) {
            BufferedReader fromClient = reader(server);
            fromClient.readLine(); // username
            assertEquals(Boolean.TRUE, connectionStates.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS));

            client.disconnect();

            assertEquals("bye", fromClient.readLine());
            assertEquals(Boolean.FALSE, connectionStates.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    void reportsErrorWhenServerIsUnreachable() throws Exception {
        int closedPort = fakeServer.getLocalPort();
        fakeServer.close(); // nothing is listening on this port any more

        ClientModel client = new ClientModel("127.0.0.1", closedPort, "alice", messages::add, connectionStates::add);
        client.connect();

        String error = messages.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull(error);
        assertTrue(error.startsWith("System: Connection error"), error);
        assertEquals(Boolean.FALSE, connectionStates.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private ClientModel newClient(String username) {
        return new ClientModel("127.0.0.1", fakeServer.getLocalPort(), username, messages::add, connectionStates::add);
    }

    private static BufferedReader reader(Socket socket) throws IOException {
        socket.setSoTimeout(TIMEOUT_MS);
        return new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }
}
