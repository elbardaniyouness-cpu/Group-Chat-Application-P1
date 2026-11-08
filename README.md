# Group Chat Application (Java, JavaFX, TCP sockets)

[![CI](https://github.com/elbardaniyouness-cpu/Group-Chat-Application-P1/actions/workflows/ci.yml/badge.svg)](https://github.com/elbardaniyouness-cpu/Group-Chat-Application-P1/actions/workflows/ci.yml)

A multi-client group chat built on plain Java TCP sockets. One server accepts any number of clients, each on its own thread, and broadcasts every message to everyone in the room. Both the server and the client have a JavaFX desktop UI.

## Features

**Client**
- Login dialog at startup. A blank username opens the client in **read-only mode**: you can read the chat but not post.
- Send with the **Send** button or the **Enter** key.
- Commands (case-insensitive):

  | Command | Effect |
  |---|---|
  | `allUsers` | The server replies to you only with the sorted list of connected users |
  | `end` or `bye` | Disconnects and closes the window |

- Connection indicator: orange = connecting, green = online, red = read-only, grey = disconnected.

**Server**
- Thread-per-client: every connection gets its own `ClientHandler` thread.
- Broadcasts each message to all clients as `[HH:mm] username: message`.
- Announces joins and leaves (`System: alice joined the chat`).
- Duplicate usernames get a unique suffix (`sam_51234`), and an empty username becomes `Guest-<port>`.
- Live list of active users, each with its own background colour, plus a server log (`Server Started on port 3000`, `Welcome alice`, disconnects).

## Architecture

```text
        chat-client (JavaFX)                         chat-server (JavaFX)
 ┌───────────────────────────────┐            ┌─────────────────────────────────┐
 │ ClientMain        (view)      │            │ ServerMain        (view)        │
 │ ClientController  (mediator)  │            │ ServerController  (mediator)    │
 │ ClientModel       (socket I/O)│◀── TCP ──▶│ ServerModel       (socket I/O)  │
 └───────────────────────────────┘  one line  │   └─ ClientHandler per client   │
          × N clients               per msg   └─────────────────────────────────┘
```

The code is split into **model / controller / view** layers:

- **Model** (`ServerModel`, `ClientModel`): sockets, threads and the chat protocol. It has no JavaFX imports and reports events through plain `Consumer<...>` callbacks, which is what makes it unit-testable.
- **Controller** (`ServerController`, `ClientController`): connects the model's callbacks to the UI. Network events arrive on background threads, so the controller hands them to the JavaFX thread with `Platform.runLater()`.
- **View** (`ServerMain`, `ClientMain`): builds the `GridPane` layouts.

**Protocol.** It's a line-based text protocol over TCP. The client's first line is its username, and every line after that is either a command (`allUsers`, `end`, `bye`) or a chat message. The server sends back one line per event.

## Project structure

```text
├── pom.xml                         parent POM (Java 17, JUnit 5)
├── chat-server/
│   ├── src/main/java/com/chat/server/   ServerLauncher, ServerMain, ServerController, ServerModel
│   ├── src/main/resources/config.properties
│   └── src/test/java/...                ServerModelTest
└── chat-client/
    ├── src/main/java/com/chat/client/   ClientLauncher, ClientMain, ClientController, ClientModel
    ├── src/main/resources/config.properties
    └── src/test/java/...                ClientModelTest
```

The `*Launcher` classes exist so the app can start from a plain classpath. A main class that doesn't extend `Application` avoids JavaFX's "runtime components are missing" error.

## Requirements

- JDK 17 or newer
- No Maven install needed: the included Maven Wrapper (`mvnw`) downloads it on first run.

## Build and test

```bash
./mvnw verify          # Windows: .\mvnw.cmd verify
```

This compiles both modules and runs the JUnit tests.

## Run

Start the server first, then one or more clients, each in its own terminal:

```bash
./mvnw -pl chat-server javafx:run
./mvnw -pl chat-client javafx:run
```

To use a different port or a server on another machine, pass arguments:

```bash
./mvnw -pl chat-server javafx:run -Djavafx.args="4000"                 # <port>
./mvnw -pl chat-client javafx:run -Djavafx.args="192.168.1.20 4000"    # <host> <port>
```

Without arguments, both read their defaults from `src/main/resources/config.properties`:

```properties
# chat-server
server.port=3000

# chat-client
server.ip=127.0.0.1
server.port=3000
```

In IntelliJ IDEA you can also run `ServerLauncher` and `ClientLauncher` directly. Allow multiple instances of the client run configuration to test several users.

## Tests

The tests cover the networking logic without starting any UI:

- **`ServerModelTest`** starts a real `ServerModel` on a free local port and connects raw sockets to it. It checks join/leave announcements, the timestamped broadcast format, that `allUsers` replies only to the sender, case-insensitive commands, `end`/`bye`, duplicate and blank usernames, ignoring empty lines, and the sorted user list.
- **`ClientModelTest`** runs `ClientModel` against a fake server socket. It checks that the username is sent first, that server lines reach the listener, that messages are forwarded, that `disconnect()` sends `bye`, and that an unreachable server produces an error message.

## Known limitations

This is a learning project for a trusted local network, not a hardened chat server:

- No authentication or encryption. Messages travel as plain text, and anyone can pick any username.
- Read-only mode is enforced only by the client, not the server.
- Usernames aren't validated, and line length isn't capped.
- Broadcasting writes to each client in turn, so one client that stops reading can slow everyone down. Per-client outbound queues, or NIO, would fix that.

## License

[MIT](LICENSE)
