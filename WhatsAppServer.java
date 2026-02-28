import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class WhatsAppServer {
    private static final int PORT = 9999;
    public static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static final List<ClientHandler> clientHandlers = new CopyOnWriteArrayList<>();
    
    public static void main(String[] args) {
        System.out.println("WhatsApp Server starting on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is ready to accept connections...");
            
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket.getInetAddress());
                
                ClientHandler clientHandler = new ClientHandler(socket);
                clientHandlers.add(clientHandler);
                clientHandler.start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void sendPrivateMessage(String sender, String recipient, String message) {
        ClientHandler senderHandler = clients.get(sender);
        ClientHandler recipientHandler = clients.get(recipient);
        
        if (recipientHandler != null) {
            // Send to recipient
            recipientHandler.sendMessage("PRIVATE:" + sender + ":" + message);
            
            // Confirm to sender
            if (senderHandler != null) {
                senderHandler.sendMessage("PRIVATE_TO:" + recipient + ":" + message);
            }
        } else {
            // Recipient not found
            if (senderHandler != null) {
                senderHandler.sendMessage("ERROR:User " + recipient + " is not online.");
            }
        }
    }
    
    public static void sendPrivateImage(String sender, String recipient, String imageData) {
        ClientHandler senderHandler = clients.get(sender);
        ClientHandler recipientHandler = clients.get(recipient);
        
        if (recipientHandler != null) {
            // Send image to recipient
            recipientHandler.sendMessage("IMAGE_PRIVATE:" + sender + ":" + imageData);
            
            // Confirm to sender
            if (senderHandler != null) {
                senderHandler.sendMessage("IMAGE_PRIVATE_TO:" + recipient + ":" + imageData);
            }
        } else {
            // Recipient not found
            if (senderHandler != null) {
                senderHandler.sendMessage("ERROR:User " + recipient + " is not online.");
            }
        }
    }
    
    public static void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clientHandlers) {
            if (client != sender && client.isActive()) {
                client.sendMessage(message);
            }
        }
    }
    
    public static void broadcastUserList() {
        StringBuilder userList = new StringBuilder("USERS_LIST:");
        for (String username : clients.keySet()) {
            userList.append(username).append(",");
        }
        
        if (userList.length() > "USERS_LIST:".length()) {
            userList.deleteCharAt(userList.length() - 1); // Remove last comma
        }
        
        for (ClientHandler client : clientHandlers) {
            if (client.isActive()) {
                client.sendMessage(userList.toString());
            }
        }
    }
    
    public static void removeClient(ClientHandler client) {
        if (client.getUsername() != null) {
            clients.remove(client.getUsername());
            clientHandlers.remove(client);
            System.out.println("Client '" + client.getUsername() + "' disconnected");
            
            // Notify others about user leaving
            if (client.getUsername() != null) {
                broadcastMessage("SYSTEM:" + client.getUsername() + " left the chat", null);
            }
            
            broadcastUserList();
        }
    }
    
    public static Map<String, ClientHandler> getClients() {
        return new HashMap<>(clients);
    }
}

class ClientHandler extends Thread {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private volatile boolean active = true;
    
    public ClientHandler(Socket socket) {
        this.socket = socket;
    }
    
    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            
            // First message should be username
            String initialMessage = in.readLine();
            if (initialMessage != null && initialMessage.startsWith("USERNAME:")) {
                username = initialMessage.substring(9); // Remove "USERNAME:" prefix
                
                // Check if username is already taken
                if (WhatsAppServer.getClients().containsKey(username)) {
                    out.println("ERROR:Username already taken. Please choose another one.");
                    closeResources();
                    return;
                }
                
                // Add client to the server's client list
                WhatsAppServer.clients.put(username, this);
                System.out.println("User '" + username + "' joined the chat");
                
                // Send welcome message
                out.println("SYSTEM:Welcome to WhatsApp-style chat, " + username + "!");
                
                // Notify others about new user
                WhatsAppServer.broadcastMessage("SYSTEM:" + username + " joined the chat", this);
                
                // Send updated user list to all clients
                WhatsAppServer.broadcastUserList();
            } else {
                closeResources();
                return;
            }
            
            String message;
            while (active && (message = in.readLine()) != null) {
                if (message.equals("/quit")) {
                    break;
                }
                
                // Handle different types of messages
                if (message.startsWith("/")) {
                    // Command handling
                    handleCommand(message);
                } else if (message.startsWith("MSG_PRIVATE:")) {
                    // Private message: MSG_PRIVATE:recipient:message
                    String[] parts = message.split(":", 3);
                    if (parts.length >= 3) {
                        String recipient = parts[1];
                        String privateMsg = parts[2];
                        WhatsAppServer.sendPrivateMessage(username, recipient, privateMsg);
                    }
                } else if (message.startsWith("IMG_PRIVATE:")) {
                    // Private image: IMG_PRIVATE:recipient:imageData
                    String[] parts = message.split(":", 3);
                    if (parts.length >= 3) {
                        String recipient = parts[1];
                        String imageData = parts[2];
                        WhatsAppServer.sendPrivateImage(username, recipient, imageData);
                    }
                } else {
                    // Regular chat message
                    WhatsAppServer.broadcastMessage(username + ": " + message, this);
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            WhatsAppServer.removeClient(this);
            closeResources();
        }
    }
    
    private void handleCommand(String command) {
        if (command.equals("/users")) {
            // Send user list to this client only
            StringBuilder userList = new StringBuilder("USERLIST:");
            for (String user : WhatsAppServer.getClients().keySet()) {
                userList.append(user).append(", ");
            }
            if (userList.length() > "USERLIST:".length()) {
                userList.delete(userList.length() - 2, userList.length()); // Remove last ", "
            }
            sendMessage(userList.toString());
        } else if (command.startsWith("/msg ")) {
            // Private message: /msg username message
            String[] parts = command.split(" ", 3);
            if (parts.length >= 3) {
                String targetUser = parts[1];
                String privateMsg = parts[2];
                
                WhatsAppServer.sendPrivateMessage(username, targetUser, privateMsg);
            }
        } else if (command.equals("/help")) {
            sendMessage("COMMANDS: /users (show online users), /msg username message (private message), /help (show this)");
        } else {
            sendMessage("ERROR:Unknown command. Type /help for available commands.");
        }
    }
    
    public void sendMessage(String message) {
        if (out != null && active) {
            out.println(message);
        }
    }
    
    public String getUsername() {
        return username;
    }
    
    public boolean isActive() {
        return active && !socket.isClosed();
    }
    
    private void closeResources() {
        active = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing client resources: " + e.getMessage());
        }
    }
}