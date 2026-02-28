# Java WhatsApp-Style Chat Application

A fully functional desktop chat application built with Java Swing for the UI and Java sockets for networking, featuring WhatsApp-like functionality.

## Features

- Real-time messaging between multiple clients
- Beautiful and responsive UI with message bubbles
- Online users list showing who's connected
- Individual/private chat functionality
- Image sharing capability
- Timestamps on messages
- Color-coded messages (sent/received/system/error/private)
- Conversations panel to manage different chats
- Clean and intuitive interface

## Components

1. **WhatsAppServer** - Handles multiple client connections and manages individual/private chats
2. **WhatsAppClient** - Desktop client with WhatsApp-like UI for chatting
3. **ChatServer** - Original group chat server (legacy)
4. **ChatClient** - Original group chat client (legacy)
5. **ChatApp** - Main application launcher with options to start server or client

## How to Run

### Prerequisites
- Java 8 or higher installed

### Steps

1. **Compile all Java files:**
   ```bash
   javac *.java
   ```

2. **Run the application:**
   ```bash
   java ChatApp
   ```
   
3. **In the main window, choose:**
   - "Start Server" - To start the legacy group chat server (run this first)
   - "Start Client" - To start a legacy group chat client (you can run multiple clients)
   - "Start WhatsApp Server" - To start the WhatsApp-style server with individual chats (run this first)
   - "Start WhatsApp Client" - To start a WhatsApp-style client (you can run multiple clients)

### Alternative Method

You can also run the server and client separately:

1. **Start the server:**
   ```bash
   java ChatServer
   ```

2. **Start clients in separate terminals:**
   ```bash
   java ChatClient
   ```

## Usage Instructions

### For WhatsApp-Style Chat:
1. Start the **WhatsApp Server** first using the "Start WhatsApp Server" button
2. Launch **WhatsApp Client** applications in separate windows using the "Start WhatsApp Client" button
3. Enter a username when prompted
4. Send messages using the input field and "Send" button
5. Double-click on a user in the "Online Users" list to start a private chat with them
6. See all your conversations in the "Conversations" panel on the left
7. Double-click on a conversation in the left panel to switch between chats
8. Click the camera button (📷) to share images
9. Click "Disconnect" or close the window to leave the chat

### For Legacy Group Chat:
1. Start the **Chat Server** first using the "Start Server" button
2. Launch **Chat Client** applications in separate windows using the "Start Client" button
3. Enter a username when prompted
4. Send messages using the input field and "Send" button
5. View online users in the right panel
6. Use commands like `/users` to see online users
7. Use `/help` to see available commands
8. Click "Disconnect" or close the window to leave the chat

## Commands

- `/users` - Show list of online users
- `/msg username message` - Send a private message
- `/help` - Show available commands
- `/quit` - Disconnect from the chat

## Architecture

- **Server**: Uses multithreading to handle multiple clients simultaneously
- **Client**: Swing-based UI with real-time message display
- **Communication**: TCP sockets with custom protocol
- **Threading**: Proper synchronization for thread-safe operations

## UI Elements

- Message history panel with color-coded bubbles
- Online users list on the right side
- Input field and send/disconnect buttons at the bottom
- Timestamps for each message
- Visual distinction between different message types

## Customization

You can customize various aspects of the application:
- Colors and fonts in the UI
- Port number (default is 9999)
- Maximum message width
- Connection timeout values