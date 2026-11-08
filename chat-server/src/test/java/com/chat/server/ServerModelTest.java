package com.chat.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests ServerModel through real TCP sockets on localhost, the same way the JavaFX client talks to it.
 * No UI is involved: the log and user-list callbacks are plain lambdas that record what they receive.
 */
class ServerModelTest {
    private static final int TIMEOUT_MS = 3000;

    private final List<String> logs = new CopyOnWriteArrayList<>();
    private final List<List<String>> userUpdates = new CopyOnWriteArrayList<>();
    private final List<Socket> sockets = new ArrayList<>();
    private ServerModel server;
    private int port;

    @BeforeEach
    void startServer() throws Exception {
        port = freePort();
        CountDownLatch started = new CountDownLatch(1);
        server = new ServerModel(port, message -> {
            logs.add(message);
            if (message.startsWith("Server Started")) {
                started.countDown();
            }
        }, userUpdates::add);
        server.start();
        assertTrue(started.await(TIMEOUT_MS, TimeUnit.MILLISECONDS), "server did not start");
    }

    @AfterEach
    void stopServer() throws IOException {
        for (Socket socket : sockets) {
            socket.close();
        }
        server.stop();
    }

    @Test
    void joiningClientIsAnnouncedToEveryone() throws Exception {
        TestClient alice = connect("alice");
        assertEquals("System: alice joined the chat", alice.readLine());

        TestClient bob = connect("bob");
        assertEquals("System: bob joined the chat", bob.readLine());
        assertEquals("System: bob joined the chat", alice.readLine());
    }

    @Test
    void messageIsBroadcastWithTimestampAndSender() throws Exception {
        TestClient alice = connect("alice");
        TestClient bob = connect("bob");
        alice.skipLines(2); // both join announcements
        bob.skipLines(1);

        alice.send("hello everyone");

        String pattern = "\\[\\d{2}:\\d{2}] alice: hello everyone";
        assertTrue(alice.readLine().matches(pattern), "sender also receives its own message");
        assertTrue(bob.readLine().matches(pattern), "other clients receive the message");
    }

    @Test
    void allUsersCommandRepliesOnlyToRequester() throws Exception {
        TestClient alice = connect("alice");
        TestClient bob = connect("bob");
        alice.skipLines(2);
        bob.skipLines(1);

        bob.send("allUsers");

        assertEquals("Active users: alice, bob", bob.readLine());
        assertNull(alice.readLineOrNull(300), "allUsers reply must not be broadcast");
    }

    @Test
    void commandsAreCaseInsensitive() throws Exception {
        TestClient alice = connect("alice");
        alice.skipLines(1);

        alice.send("ALLUSERS");

        assertEquals("Active users: alice", alice.readLine());
    }

    @Test
    void byeDisconnectsClientAndNotifiesOthers() throws Exception {
        TestClient alice = connect("alice");
        TestClient bob = connect("bob");
        alice.skipLines(2);
        bob.skipLines(1);

        bob.send("bye");

        assertEquals("System: bob left the chat", alice.readLine());
        alice.send("allUsers");
        assertEquals("Active users: alice", alice.readLine());
    }

    @Test
    void endAlsoDisconnects() throws Exception {
        TestClient alice = connect("alice");
        TestClient bob = connect("bob");
        alice.skipLines(2);
        bob.skipLines(1);

        bob.send("end");

        assertEquals("System: bob left the chat", alice.readLine());
    }

    @Test
    void duplicateUsernameGetsUniqueSuffix() throws Exception {
        TestClient first = connect("sam");
        first.skipLines(1);
        TestClient second = connect("sam");

        String joined = second.readLine();
        assertTrue(joined.matches("System: sam_\\d+ joined the chat"), joined);
    }

    @Test
    void blankUsernameBecomesGuest() throws Exception {
        TestClient guest = connect("   ");

        assertTrue(guest.readLine().matches("System: Guest-\\d+ joined the chat"));
    }

    @Test
    void emptyMessagesAreIgnored() throws Exception {
        TestClient alice = connect("alice");
        alice.skipLines(1);

        alice.send("   ");

        assertNull(alice.readLineOrNull(300), "blank lines should not be broadcast");
    }

    @Test
    void activeUserListIsPublishedSorted() throws Exception {
        connect("zoe").skipLines(1);
        connect("adam").skipLines(1);

        List<String> latest = userUpdates.get(userUpdates.size() - 1);
        assertEquals(List.of("adam", "zoe"), latest);
        assertTrue(logs.contains("Welcome adam"));
    }

    private TestClient connect(String username) throws IOException {
        Socket socket = new Socket("127.0.0.1", port);
        socket.setSoTimeout(TIMEOUT_MS);
        sockets.add(socket);
        TestClient client = new TestClient(socket);
        client.send(username); // the first line a client sends is its username
        return client;
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    /** Minimal line-based client, equivalent to what ClientModel does. */
    private static final class TestClient {
        private final Socket socket;
        private final BufferedReader in;
        private final PrintWriter out;

        TestClient(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        }

        void send(String line) {
            out.println(line);
        }

        String readLine() throws IOException {
            return in.readLine();
        }

        void skipLines(int count) throws IOException {
            for (int i = 0; i < count; i++) {
                in.readLine();
            }
        }

        /** Returns the next line, or null if nothing arrives within the given time. */
        String readLineOrNull(int timeoutMs) throws IOException {
            int previous = socket.getSoTimeout();
            socket.setSoTimeout(timeoutMs);
            try {
                return in.readLine();
            } catch (SocketTimeoutException expected) {
                return null;
            } finally {
                socket.setSoTimeout(previous);
            }
        }
    }
}
