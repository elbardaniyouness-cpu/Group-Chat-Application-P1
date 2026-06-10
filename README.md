# Mini Project 1 — Group Chat Application

A Java TCP socket-based group chat application built with **JavaFX**. The system follows a **client-server architecture**, where one central server accepts multiple clients and broadcasts messages between them in real time.

This project was developed for **Mini Project 1: Group Chat Application**.

---

## Table of Contents

- [Project Overview](#project-overview)
- [Main Features](#main-features)
- [System Architecture](#system-architecture)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Configuration](#configuration)
- [How to Run](#how-to-run)
- [Client Commands](#client-commands)
- [Functional Requirements Coverage](#functional-requirements-coverage)
- [Model-View Decoupling](#model-view-decoupling)
- [UML Diagrams](#uml-diagrams)
- [Demo Video Checklist](#demo-video-checklist)
- [Build Executable JAR Files](#build-executable-jar-files)
- [Future Improvements](#future-improvements)
- [Author](#author)

---

## Project Overview

The application allows multiple users to connect to a central server and communicate inside a shared group chat room.

Each client connects to the server using the server IP address and port number. When a client sends a message, the server receives it, formats it with the sender username and timestamp, then distributes it to all connected clients.

The project contains two main applications:

1. **TCPServer**  
   The server application that manages client connections, receives messages, broadcasts messages, tracks active users, and displays activity logs.

2. **TCPClient**  
   The client application that allows users to connect to the server, choose a username, send messages, receive group messages, request active users, and disconnect.

---

## Main Features

### Client Features

- Username entry before joining the chat.
- Read-only mode when the user connects without a username.
- Real-time message sending and receiving.
- Send messages using either:
  - the **SEND** button, or
  - the **Enter** key.
- `allUsers` command to request the list of active users.
- `end` or `bye` command to disconnect from the server.
- Online status label.
- Visual online indicator, such as a green circle.
- JavaFX graphical interface.

### Server Features

- Accepts multiple client connections at the same time.
- Creates a dedicated handler for each connected client.
- Receives messages from clients.
- Broadcasts messages to all active clients.
- Formats messages with:
  - username,
  - timestamp,
  - message content.
- Displays connected users in a live `ListView`.
- Assigns random background colors to users for visual distinction.
- Shows server logs such as:
  - `Server Started`,
  - `Waiting for Client`,
  - `Welcome [username]`,
  - user disconnection messages.

---

## System Architecture

The project uses a **Server-Client architecture**.

```text
+----------------+          TCP Socket          +----------------+
|                | <--------------------------> |                |
|   TCPClient    |                              |                |
|                |                              |                |
+----------------+                              |                |
                                                |                |
+----------------+          TCP Socket          |   TCPServer    |
|                | <--------------------------> |                |
|   TCPClient    |                              |                |
|                |                              |                |
+----------------+                              |                |
                                                |                |
+----------------+          TCP Socket          |                |
|                | <--------------------------> |                |
|   TCPClient    |                              |                |
|                |                              +----------------+
+----------------+
