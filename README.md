# ChitChat - Java WhatsApp-Style Chat Application

A fully functional desktop chat application built with Java Swing for the UI and Java sockets for networking, featuring WhatsApp-like functionality.

## Features

- Real-time messaging between multiple clients
- Beautiful and responsive UI with message bubbles (WhatsApp styling)
- Online users list showing who's connected
- Individual/private chat functionality
- Image sharing capability
- Video call initiation simulation
- Timestamps on messages
- Color-coded messages (sent/received/system/error/private)
- Conversations panel to manage different chats
- Clean and intuitive interface

## Components

1. **WhatsAppServer** - Handles multiple client connections and manages individual/private chats
2. **WhatsAppClient** - Desktop client with WhatsApp-like UI for chatting
3. **ChatApp** - Main application launcher with an elegant UI to start the server or client

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
   - **Start Server** - To start the backend server (run this first on port 9999)
   - **Start Client** - To start the chat client (you can run multiple clients to test chatting)

### Alternative Method

You can also run the server and client separately from the terminal:

1. **Start the server:**
   ```bash
   java WhatsAppServer
   ```

2. **Start clients in separate terminals:**
   ```bash
   java ChatApp
   ```
   (And click "Start Client")

## Usage Instructions

1. Start the **Server** first using the "Start Server" button in the ChatApp launcher window.
2. Launch **Client** applications using the "Start Client" button (you can do this multiple times for multiple users).
3. Enter a username when prompted.
4. Send messages using the input field and "Send" button.
5. Double-click on a user in the "Online Now" list to start a private chat with them.
6. See all your conversations in the left-side panel.
7. Click on a conversation in the left panel to switch between chats.
8. Click the camera button (📷) to share images.
9. Click the video camera button (🎥) in the header to initiate a video call.
10. Click the "✖" (Disconnect) button in the top left to leave the chat.

## Architecture

- **Server**: Uses multithreading to handle multiple clients simultaneously.
- **Client**: Swing-based UI with real-time message display.
- **Communication**: TCP sockets with a custom protocol.
- **Threading**: Proper synchronization for thread-safe operations.

## UI Elements

- Message history panel with color-coded bubbles.
- Online users list and active conversations on the left side.
- Input field with send image and send text buttons at the bottom.
- Timestamps for each message.
- Visual distinction between different message types (group vs private, images, etc).

## Customization

You can customize various aspects of the application by editing the Java files:
- Colors and fonts (e.g., `APP_DARK`, `APP_GREEN`, `APP_SENT_BUBBLE` inside `WhatsAppClient.java`).
- Server port number (default is 9999).