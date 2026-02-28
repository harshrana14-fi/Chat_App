import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class WhatsAppClient extends JFrame {

    // ── App colour palette ────────────────────────────────────────────────────
    private static final Color APP_DARK = new Color(7, 94, 84);
    private static final Color APP_GREEN = new Color(18, 140, 126);
    private static final Color APP_SENT_BUBBLE = new Color(217, 253, 211);
    private static final Color APP_RECV_BUBBLE = Color.WHITE;
    private static final Color APP_BG = new Color(236, 229, 221);
    private static final Color APP_SIDEBAR_BG = new Color(249, 249, 249);
    private static final Color APP_HEADER_BG = new Color(32, 44, 51);
    private static final Color APP_DIVIDER = new Color(230, 230, 230);
    private static final Color APP_TIME_COLOR = new Color(142, 142, 142);
    private static final Color APP_SEND_BTN = new Color(18, 140, 126);

    // ── UI components ─────────────────────────────────────────────────────────
    private JPanel messagesPanel;
    private JTextField inputField;
    private JButton sendButton;
    private JButton sendImageButton;
    private JButton disconnectButton;
    private JButton videoCallButton;
    private JLabel headerStatusLabel;
    private JLabel headerNameLabel;

    // ── Data ──────────────────────────────────────────────────────────────────
    private PrintWriter out;
    private volatile boolean connected = true;
    private String userName;
    private DefaultListModel<String> usersListModel = new DefaultListModel<>();
    private DefaultListModel<String> conversationsListModel = new DefaultListModel<>();
    private JList<String> usersList;
    private JList<String> conversationsList;
    private String currentConversation = "All";
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

    // ─────────────────────────────────────────────────────────────────────────
    public WhatsAppClient() {
        setTitle("ChitChat");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(920, 650);
        setMinimumSize(new Dimension(700, 500));
        setLocationRelativeTo(null);
        buildUI();
    }

    // ─── Build full UI ───────────────────────────────────────────────────────
    private void buildUI() {
        setLayout(new BorderLayout());
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildSidebar(), buildChatArea());
        mainSplit.setDividerSize(1);
        mainSplit.setDividerLocation(270);
        mainSplit.setBorder(null);
        add(mainSplit, BorderLayout.CENTER);
    }

    // ─── LEFT SIDEBAR ────────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(APP_SIDEBAR_BG);

        // Sidebar header bar
        JPanel sidebarHeader = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(APP_HEADER_BG);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        sidebarHeader.setPreferredSize(new Dimension(0, 56));
        sidebarHeader.setBorder(new EmptyBorder(0, 14, 0, 14));
        sidebarHeader.setOpaque(false);

        JLabel appName = new JLabel("ChitChat");
        appName.setFont(new Font("Segoe UI", Font.BOLD, 17));
        appName.setForeground(Color.WHITE);

        disconnectButton = makeIconButton("✖", new Color(200, 50, 50), "Disconnect");
        disconnectButton.addActionListener(e -> disconnect());

        sidebarHeader.add(appName, BorderLayout.WEST);
        sidebarHeader.add(disconnectButton, BorderLayout.EAST);

        // Search bar
        JPanel searchBar = new JPanel(new BorderLayout(8, 0));
        searchBar.setBackground(APP_SIDEBAR_BG);
        searchBar.setBorder(new EmptyBorder(8, 12, 8, 12));

        JTextField searchField = new JTextField("Search or start new chat");
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setForeground(Color.GRAY);
        searchField.setBackground(new Color(243, 243, 243));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(new Color(200, 200, 200), 1, 20),
                new EmptyBorder(6, 12, 6, 12)));
        searchBar.add(searchField, BorderLayout.CENTER);

        // Conversations list
        conversationsListModel.addElement("All (Group)");
        conversationsList = new JList<>(conversationsListModel);
        conversationsList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        conversationsList.setBackground(APP_SIDEBAR_BG);
        conversationsList.setFixedCellHeight(60);
        conversationsList.setBorder(null);
        conversationsList.setCellRenderer(new ConversationCellRenderer());
        conversationsList.setSelectedIndex(0);
        conversationsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int idx = conversationsList.locationToIndex(e.getPoint());
                if (idx >= 0)
                    setCurrentConversation(conversationsListModel.getElementAt(idx));
            }
        });

        JScrollPane convScroll = new JScrollPane(conversationsList);
        convScroll.setBorder(null);
        convScroll.getVerticalScrollBar().setUnitIncrement(16);

        // Online users section
        JLabel usersLabel = new JLabel("  Online Now");
        usersLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        usersLabel.setForeground(APP_TIME_COLOR);
        usersLabel.setBorder(new EmptyBorder(8, 12, 4, 12));

        usersList = new JList<>(usersListModel);
        usersList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        usersList.setBackground(APP_SIDEBAR_BG);
        usersList.setFixedCellHeight(44);
        usersList.setBorder(null);
        usersList.setCellRenderer(new UserCellRenderer());
        usersList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int idx = usersList.locationToIndex(e.getPoint());
                    if (idx >= 0) {
                        String user = usersListModel.getElementAt(idx);
                        if (!user.equals(userName))
                            startPrivateChat(user);
                    }
                }
            }
        });

        JScrollPane usersScroll = new JScrollPane(usersList);
        usersScroll.setBorder(null);
        usersScroll.setPreferredSize(new Dimension(0, 160));

        JPanel bottomSidebar = new JPanel(new BorderLayout());
        bottomSidebar.setBackground(APP_SIDEBAR_BG);
        bottomSidebar.add(usersLabel, BorderLayout.NORTH);
        bottomSidebar.add(usersScroll, BorderLayout.CENTER);

        JPanel sidebarContent = new JPanel(new BorderLayout());
        sidebarContent.setBackground(APP_SIDEBAR_BG);
        sidebarContent.add(searchBar, BorderLayout.NORTH);
        sidebarContent.add(convScroll, BorderLayout.CENTER);
        sidebarContent.add(bottomSidebar, BorderLayout.SOUTH);

        sidebar.add(sidebarHeader, BorderLayout.NORTH);
        sidebar.add(sidebarContent, BorderLayout.CENTER);
        return sidebar;
    }

    // ─── RIGHT CHAT AREA ─────────────────────────────────────────────────────
    private JPanel buildChatArea() {
        JPanel chatArea = new JPanel(new BorderLayout());
        chatArea.setBackground(APP_BG);

        // Chat header
        JPanel chatHeader = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(APP_HEADER_BG);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        chatHeader.setPreferredSize(new Dimension(0, 56));
        chatHeader.setBorder(new EmptyBorder(0, 14, 0, 14));
        chatHeader.setOpaque(false);

        // Avatar circle
        JLabel avatarLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(APP_GREEN);
                g2.fillOval(0, 0, 38, 38);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                String ch = currentConversation.isEmpty() ? "?"
                        : String.valueOf(currentConversation.charAt(0)).toUpperCase();
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(ch, (38 - fm.stringWidth(ch)) / 2, (38 + fm.getAscent() - fm.getDescent()) / 2);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(38, 38);
            }

            @Override
            public boolean isOpaque() {
                return false;
            }
        };

        JPanel namePanel = new JPanel(new GridLayout(2, 1, 0, 1));
        namePanel.setOpaque(false);
        headerNameLabel = new JLabel("All (Group)");
        headerNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        headerNameLabel.setForeground(Color.WHITE);
        headerStatusLabel = new JLabel("Group Chat");
        headerStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        headerStatusLabel.setForeground(new Color(180, 200, 195));
        namePanel.add(headerNameLabel);
        namePanel.add(headerStatusLabel);

        // ── Video call button in header ──
        videoCallButton = makeIconButton("\uD83D\uDCF9", APP_GREEN, "Video Call");
        videoCallButton.addActionListener(e -> startVideoCall());

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        headerRight.setOpaque(false);
        headerRight.add(videoCallButton);

        chatHeader.add(avatarLabel, BorderLayout.WEST);
        chatHeader.add(namePanel, BorderLayout.CENTER);
        chatHeader.add(headerRight, BorderLayout.EAST);

        // Messages area
        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(APP_BG);
        messagesPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane msgScroll = new JScrollPane(messagesPanel);
        msgScroll.setBorder(null);
        msgScroll.getVerticalScrollBar().setUnitIncrement(16);
        msgScroll.setBackground(APP_BG);

        chatArea.add(chatHeader, BorderLayout.NORTH);
        chatArea.add(msgScroll, BorderLayout.CENTER);
        chatArea.add(buildInputBar(), BorderLayout.SOUTH);
        return chatArea;
    }

    // ─── Input bar ───────────────────────────────────────────────────────────
    private JPanel buildInputBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(242, 242, 242));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        bar.setBorder(new EmptyBorder(8, 10, 8, 10));
        bar.setOpaque(false);

        sendImageButton = makeIconButton("\uD83D\uDCF7", APP_GREEN, "Send Image");
        sendImageButton.addActionListener(e -> sendImage());

        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setBackground(Color.WHITE);
        inputField.setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(new Color(210, 210, 210), 1, 22),
                new EmptyBorder(8, 14, 8, 14)));
        inputField.setPreferredSize(new Dimension(0, 44));
        inputField.addActionListener(e -> sendMessage());

        sendButton = makeIconButton("\u27A4", APP_SEND_BTN, "Send");
        sendButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        sendButton.addActionListener(e -> sendMessage());

        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftBtns.setOpaque(false);
        leftBtns.add(sendImageButton);

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightBtns.setOpaque(false);
        rightBtns.add(sendButton);

        bar.add(leftBtns, BorderLayout.WEST);
        bar.add(inputField, BorderLayout.CENTER);
        bar.add(rightBtns, BorderLayout.EAST);
        return bar;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────
    private JButton makeIconButton(String icon, Color bg, String tooltip) {
        JButton btn = new JButton(icon) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? bg.brighter() : bg);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }

            @Override
            public boolean isOpaque() {
                return false;
            }
        };
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setPreferredSize(new Dimension(40, 40));
        btn.setToolTipText(tooltip);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void setCurrentConversation(String conv) {
        currentConversation = conv.replace(" (Group)", "");
        headerNameLabel.setText(conv);
        headerStatusLabel.setText(
                currentConversation.equals("All") ? "Group Chat" : "Private Chat");
        messagesPanel.removeAll();
        addInfoMessage("Conversation: " + conv);
        messagesPanel.revalidate();
        messagesPanel.repaint();
        inputField.requestFocusInWindow();
    }

    private void startPrivateChat(String user) {
        if (!conversationsListModel.contains(user))
            conversationsListModel.addElement(user);
        conversationsList.setSelectedValue(user, true);
        setCurrentConversation(user);
    }

    // ─── Video call ───────────────────────────────────────────────────────────
    private void startVideoCall() {
        String target = currentConversation.equals("All") ? "Group" : currentConversation;

        // Notify chat
        if (out != null) {
            String notif = "[" + userName + " started a video call]";
            if (currentConversation.equals("All")) {
                out.println(notif);
            } else {
                out.println("MSG_PRIVATE:" + currentConversation + ":" + notif);
            }
        }
        addInfoMessage("You started a video call with " + target);

        // Open video call window
        new VideoCallWindow(this, userName, target).setVisible(true);
    }

    // ─── Message sending ─────────────────────────────────────────────────────
    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty() || out == null)
            return;

        if (currentConversation.equals("All")) {
            out.println(text);
            addBubble(text, true, null);
        } else {
            out.println("MSG_PRIVATE:" + currentConversation + ":" + text);
            addBubble(text, true, "To " + currentConversation);
        }
        inputField.setText("");
    }

    private void sendImage() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Choose Image");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Images", "jpg", "jpeg", "png", "gif", "bmp"));

        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                BufferedImage img = ImageIO.read(fc.getSelectedFile());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "png", baos);
                String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());

                if (currentConversation.equals("All")) {
                    out.println("IMAGE:" + b64);
                    addImageBubble(img, true, null);
                } else {
                    out.println("IMG_PRIVATE:" + currentConversation + ":" + b64);
                    addImageBubble(img, true, "To " + currentConversation);
                }
            } catch (IOException ex) {
                addError("Failed to send image: " + ex.getMessage());
            }
        }
    }

    private void disconnect() {
        connected = false;
        if (out != null) {
            out.println("/quit");
            out.close();
        }
        usersListModel.clear();
        conversationsListModel.clear();
        addInfoMessage("You have disconnected.");
        inputField.setEnabled(false);
        sendButton.setEnabled(false);
        sendImageButton.setEnabled(false);
        videoCallButton.setEnabled(false);
        disconnectButton.setEnabled(false);
        headerStatusLabel.setText("Disconnected");
    }

    // ─── Bubble rendering ────────────────────────────────────────────────────
    private void addBubble(String text, boolean sent, String label) {
        String time = LocalTime.now().format(timeFmt);
        Color bg = sent ? APP_SENT_BUBBLE : APP_RECV_BUBBLE;

        JPanel bubble = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
            }

            @Override
            public boolean isOpaque() {
                return false;
            }
        };
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(new EmptyBorder(8, 12, 8, 12));

        if (label != null) {
            JLabel lbl = new JLabel(label);
            lbl.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            lbl.setForeground(APP_GREEN);
            bubble.add(lbl);
            bubble.add(Box.createVerticalStrut(2));
        }

        JLabel msg = new JLabel(
                "<html><body style='width:240px'>" + escapeHtml(text) + "</body></html>");
        msg.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        msg.setForeground(new Color(30, 30, 30));
        bubble.add(msg);

        JLabel timeLabel = new JLabel(time);
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        timeLabel.setForeground(APP_TIME_COLOR);
        bubble.add(Box.createVerticalStrut(2));
        bubble.add(timeLabel);

        addBubbleRow(bubble, sent);
    }

    private void addSenderBubble(String sender, String text) {
        String time = LocalTime.now().format(timeFmt);
        JPanel bubble = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(APP_RECV_BUBBLE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
            }

            @Override
            public boolean isOpaque() {
                return false;
            }
        };
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(new EmptyBorder(8, 12, 8, 12));

        JLabel senderLbl = new JLabel(sender);
        senderLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        senderLbl.setForeground(APP_GREEN);
        bubble.add(senderLbl);
        bubble.add(Box.createVerticalStrut(3));

        JLabel msg = new JLabel(
                "<html><body style='width:240px'>" + escapeHtml(text) + "</body></html>");
        msg.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        msg.setForeground(new Color(30, 30, 30));
        bubble.add(msg);

        JLabel timeLabel = new JLabel(time);
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        timeLabel.setForeground(APP_TIME_COLOR);
        bubble.add(Box.createVerticalStrut(2));
        bubble.add(timeLabel);

        addBubbleRow(bubble, false);
    }

    private void addImageBubble(BufferedImage img, boolean sent, String label) {
        Color bg = sent ? APP_SENT_BUBBLE : APP_RECV_BUBBLE;
        JPanel bubble = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
            }

            @Override
            public boolean isOpaque() {
                return false;
            }
        };
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(new EmptyBorder(6, 8, 6, 8));

        if (label != null) {
            JLabel lbl = new JLabel(label);
            lbl.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            lbl.setForeground(APP_GREEN);
            bubble.add(lbl);
            bubble.add(Box.createVerticalStrut(4));
        }

        Image scaled = img.getScaledInstance(220, -1, Image.SCALE_SMOOTH);
        bubble.add(new JLabel(new ImageIcon(scaled)));
        addBubbleRow(bubble, sent);
    }

    private void addBubbleRow(JPanel bubble, boolean sent) {
        JPanel row = new JPanel(new FlowLayout(sent ? FlowLayout.RIGHT : FlowLayout.LEFT, 4, 3));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        row.add(bubble);
        SwingUtilities.invokeLater(() -> {
            messagesPanel.add(row);
            messagesPanel.add(Box.createVerticalStrut(4));
            messagesPanel.revalidate();
            messagesPanel.repaint();
            scrollToBottom();
        });
    }

    private void addInfoMessage(String text) {
        JPanel pill = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(225, 245, 254, 200));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
            }

            @Override
            public boolean isOpaque() {
                return false;
            }
        };
        pill.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 4));
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lbl.setForeground(new Color(90, 90, 90));
        pill.add(lbl);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        row.add(pill);

        SwingUtilities.invokeLater(() -> {
            messagesPanel.add(row);
            messagesPanel.add(Box.createVerticalStrut(6));
            messagesPanel.revalidate();
            messagesPanel.repaint();
            scrollToBottom();
        });
    }

    private void addError(String text) {
        addInfoMessage("⚠ " + text);
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            Container vp = messagesPanel.getParent();
            if (vp instanceof JViewport) {
                JScrollPane sp = (JScrollPane) vp.getParent();
                sp.getVerticalScrollBar().setValue(sp.getVerticalScrollBar().getMaximum());
            }
        });
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ─── Connection ───────────────────────────────────────────────────────────
    public void connect() {
        try {
            Socket socket = new Socket("localhost", 9999);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String name = JOptionPane.showInputDialog(this,
                    "Enter your username:", "ChitChat Login", JOptionPane.PLAIN_MESSAGE);
            if (name == null || name.trim().isEmpty())
                name = "User" + (int) (Math.random() * 1000);
            userName = name.trim();
            out.println("USERNAME:" + userName);
            setTitle("ChitChat — " + userName);
            headerStatusLabel.setText("Online");

            addInfoMessage("Connected as \"" + userName + "\"");

            Thread reader = new Thread(() -> {
                try {
                    String line;
                    while (connected && (line = in.readLine()) != null) {
                        final String msg = line;
                        SwingUtilities.invokeLater(() -> processMessage(msg));
                    }
                } catch (IOException e) {
                    if (connected)
                        SwingUtilities.invokeLater(
                                () -> addInfoMessage("Disconnected from server."));
                }
            });
            reader.setDaemon(true);
            reader.start();

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    disconnect();
                }
            });

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Could not connect to server.\n" + e.getMessage(),
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    // ─── Message processing ───────────────────────────────────────────────────
    private void processMessage(String message) {
        if (message.startsWith("USERS_LIST:")) {
            String[] users = message.substring(11).split(",");
            usersListModel.clear();
            for (String u : users)
                if (!u.trim().isEmpty() && !u.trim().equals(userName))
                    usersListModel.addElement(u.trim());
            int count = usersListModel.size();
            headerStatusLabel.setText(count > 0 ? count + " online" : "No one else online");

        } else if (message.startsWith("PRIVATE:")) {
            String[] parts = message.split(":", 3);
            if (parts.length >= 3) {
                String sender = parts[1], content = parts[2];
                if (!conversationsListModel.contains(sender))
                    conversationsListModel.addElement(sender);
                // Check for video call invite in private message
                if (content.contains("started a video call")) {
                    addVideoCallInvite(sender, sender);
                } else {
                    addSenderBubble(sender + " (private)", content);
                }
            }
        } else if (message.startsWith("PRIVATE_TO:")) {
            String[] parts = message.split(":", 3);
            if (parts.length >= 3)
                addBubble(parts[2], true, "To " + parts[1]);

        } else if (message.startsWith("IMAGE_PRIVATE:")) {
            String[] parts = message.split(":", 3);
            if (parts.length >= 3) {
                String sender = parts[1], imgData = parts[2];
                if (!conversationsListModel.contains(sender))
                    conversationsListModel.addElement(sender);
                try {
                    byte[] bytes = Base64.getDecoder().decode(imgData);
                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                    addImageBubble(img, false, "From " + sender);
                } catch (IOException ex) {
                    addError("Failed to load image");
                }
            }
        } else if (message.startsWith("IMAGE_PRIVATE_TO:")) {
            String[] parts = message.split(":", 3);
            if (parts.length >= 3) {
                try {
                    byte[] bytes = Base64.getDecoder().decode(parts[2]);
                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                    addImageBubble(img, true, "To " + parts[1]);
                } catch (IOException ex) {
                    addError("Failed to load image");
                }
            }
        } else if (message.startsWith("SYSTEM:")) {
            addInfoMessage("ℹ " + message.substring(7));
        } else if (message.startsWith("ERROR:")) {
            addError(message.substring(6));
        } else {
            addReceivedBroadcast(message);
        }
    }

    private void addReceivedBroadcast(String message) {
        int colon = message.indexOf(':');
        if (colon > 0) {
            String sender = message.substring(0, colon).trim();
            String payload = message.substring(colon + 1).trim();

            if (payload.startsWith("IMAGE:")) {
                // Broadcast image
                String b64 = payload.substring(6);
                try {
                    byte[] bytes = Base64.getDecoder().decode(b64);
                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                    if (img != null)
                        addImageBubble(img, false, sender);
                    else
                        addError("Received corrupt image from " + sender);
                } catch (Exception ex) {
                    addError("Failed to decode image from " + sender);
                }
            } else if (payload.contains("started a video call")) {
                // Video call invite — show Join button
                addVideoCallInvite(sender, "Group");
            } else {
                addSenderBubble(sender, payload);
            }
        } else {
            addBubble(message, false, null);
        }
    }

    /**
     * Shows a special video call invite card with a Join button.
     * caller = who started the call, target = who/group to join into.
     */
    private void addVideoCallInvite(String caller, String target) {
        // Dark card background
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(30, 42, 50));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
            }

            @Override
            public boolean isOpaque() {
                return false;
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(12, 16, 12, 16));

        // Video icon + caller line
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        topRow.setOpaque(false);

        JLabel icon = new JLabel("\uD83D\uDCF9");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        icon.setForeground(Color.WHITE);

        JLabel callerLbl = new JLabel(caller + " is calling...");
        callerLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        callerLbl.setForeground(Color.WHITE);

        topRow.add(icon);
        topRow.add(callerLbl);
        card.add(topRow);
        card.add(Box.createVerticalStrut(4));

        JLabel subLbl = new JLabel("Video call invitation");
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subLbl.setForeground(new Color(37, 211, 102));
        subLbl.setBorder(new EmptyBorder(0, 0, 8, 0));
        card.add(subLbl);

        // Join button
        JButton joinBtn = new JButton("\uD83D\uDCF9  Join Call") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(45, 220, 115) : new Color(37, 211, 102));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI Emoji", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }

            @Override
            public boolean isOpaque() {
                return false;
            }
        };
        joinBtn.setFont(new Font("Segoe UI Emoji", Font.BOLD, 13));
        joinBtn.setFocusPainted(false);
        joinBtn.setBorderPainted(false);
        joinBtn.setContentAreaFilled(false);
        joinBtn.setPreferredSize(new Dimension(160, 36));
        joinBtn.setMaximumSize(new Dimension(160, 36));
        joinBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        joinBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        joinBtn.addActionListener(e -> {
            joinBtn.setText("Joining...");
            joinBtn.setEnabled(false);
            // Open video call window for this client
            new VideoCallWindow(WhatsAppClient.this, userName, caller).setVisible(true);
        });

        // Decline button
        JButton declineBtn = new JButton("Decline") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(200, 50, 50) : new Color(170, 40, 40));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }

            @Override
            public boolean isOpaque() {
                return false;
            }
        };
        declineBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        declineBtn.setFocusPainted(false);
        declineBtn.setBorderPainted(false);
        declineBtn.setContentAreaFilled(false);
        declineBtn.setPreferredSize(new Dimension(100, 36));
        declineBtn.setMaximumSize(new Dimension(100, 36));
        declineBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        declineBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        declineBtn.addActionListener(e -> {
            addInfoMessage("You declined " + caller + "'s video call.");
            card.setVisible(false);
            messagesPanel.revalidate();
        });

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setOpaque(false);
        btnRow.add(joinBtn);
        btnRow.add(declineBtn);
        card.add(btnRow);

        // Time label
        JLabel timeLbl = new JLabel(LocalTime.now().format(timeFmt));
        timeLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        timeLbl.setForeground(new Color(120, 140, 150));
        timeLbl.setBorder(new EmptyBorder(6, 0, 0, 0));
        card.add(timeLbl);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 3));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        row.add(card);

        SwingUtilities.invokeLater(() -> {
            messagesPanel.add(row);
            messagesPanel.add(Box.createVerticalStrut(6));
            messagesPanel.revalidate();
            messagesPanel.repaint();
            scrollToBottom();
        });
    }

    // ─── Custom cell renderers ────────────────────────────────────────────────
    class ConversationCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean selected, boolean focused) {
            JPanel cell = new JPanel(new BorderLayout(10, 0));
            cell.setBackground(selected ? new Color(235, 235, 235) : APP_SIDEBAR_BG);
            cell.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, APP_DIVIDER),
                    new EmptyBorder(8, 12, 8, 12)));

            String name = value.toString().replace(" (Group)", "");
            JLabel avatar = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(name.equals("All") ? APP_DARK : APP_GREEN);
                    g2.fillOval(0, 2, 36, 36);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
                    String ch = name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase();
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(ch, (36 - fm.stringWidth(ch)) / 2,
                            2 + (36 + fm.getAscent() - fm.getDescent()) / 2);
                }

                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(38, 40);
                }

                @Override
                public boolean isOpaque() {
                    return false;
                }
            };

            JLabel nameLbl = new JLabel(value.toString());
            nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
            nameLbl.setForeground(new Color(40, 40, 40));

            cell.add(avatar, BorderLayout.WEST);
            cell.add(nameLbl, BorderLayout.CENTER);
            return cell;
        }
    }

    class UserCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean selected, boolean focused) {
            JPanel cell = new JPanel(new BorderLayout(8, 0));
            cell.setBackground(selected ? new Color(235, 235, 235) : APP_SIDEBAR_BG);
            cell.setBorder(new EmptyBorder(6, 12, 6, 12));

            JLabel dot = new JLabel("\u25CF");
            dot.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            dot.setForeground(new Color(37, 211, 102));

            JLabel nameLbl = new JLabel(value.toString());
            nameLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            nameLbl.setForeground(new Color(50, 50, 50));

            cell.add(dot, BorderLayout.WEST);
            cell.add(nameLbl, BorderLayout.CENTER);
            return cell;
        }
    }

    // ─── Rounded border helper ────────────────────────────────────────────────
    static class RoundedLineBorder extends AbstractBorder {
        private final Color color;
        private final int thickness, radius;

        RoundedLineBorder(Color c, int t, int r) {
            color = c;
            thickness = t;
            radius = r;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.draw(new RoundRectangle2D.Float(x, y, w - 1, h - 1, radius, radius));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(thickness, thickness, thickness, thickness);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    // ─── entry point ─────────────────────────────────────────────────────────
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> {
            WhatsAppClient c = new WhatsAppClient();
            c.setVisible(true);
            c.connect();
        });
    }

    // =========================================================================
    // ── VIDEO CALL WINDOW ────────────────────────────────────────────────────
    // =========================================================================
    static class VideoCallWindow extends JDialog {

        private static final Color VC_BG = new Color(15, 20, 25);
        private static final Color VC_DARK = new Color(25, 35, 45);
        private static final Color VC_GREEN = new Color(37, 211, 102);
        private static final Color VC_RED = new Color(220, 50, 50);
        private static final Color VC_BTN_BG = new Color(45, 55, 65);

        private boolean muted = false;
        private boolean camOff = false;
        private Timer callTimer;
        private int seconds = 0;
        private JLabel timerLabel;
        private JPanel mainVideoPanel;
        private JLabel statusLabel;

        VideoCallWindow(Frame parent, String myName, String target) {
            super(parent, "Video Call — " + target, false);
            setSize(700, 520);
            setLocationRelativeTo(parent);
            setResizable(true);
            buildCallUI(myName, target);

            // Start call timer
            callTimer = new Timer(1000, e -> {
                seconds++;
                int m = seconds / 60, s = seconds % 60;
                timerLabel.setText(String.format("%02d:%02d", m, s));
            });
            callTimer.start();

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    callTimer.stop();
                }
            });
        }

        private void buildCallUI(String myName, String target) {
            JPanel root = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    g.setColor(VC_BG);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            };
            root.setOpaque(false);

            // ── Main "remote" video area ──────────────────────────────────
            mainVideoPanel = new JPanel(new GridBagLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    GradientPaint gp = new GradientPaint(0, 0, new Color(20, 40, 50),
                            0, getHeight(), new Color(5, 15, 25));
                    g2.setPaint(gp);
                    g2.fillRect(0, 0, getWidth(), getHeight());

                    if (camOff) {
                        // "Camera off" placeholder
                        g2.setColor(new Color(40, 50, 60));
                        int r = 70;
                        int cx = getWidth() / 2, cy = getHeight() / 2 - 30;
                        g2.fillOval(cx - r, cy - r, r * 2, r * 2);
                        g2.setColor(new Color(90, 110, 120));
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 38));
                        String ch = target.isEmpty() ? "?" : String.valueOf(target.charAt(0)).toUpperCase();
                        FontMetrics fm = g2.getFontMetrics();
                        g2.drawString(ch, cx - fm.stringWidth(ch) / 2,
                                cy + (fm.getAscent() - fm.getDescent()) / 2);
                    }
                }
            };

            // Avatar / name in centre
            JPanel centerInfo = new JPanel();
            centerInfo.setOpaque(false);
            centerInfo.setLayout(new BoxLayout(centerInfo, BoxLayout.Y_AXIS));

            JLabel avatarCircle = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(30, 140, 120));
                    g2.fillOval(0, 0, 90, 90);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 40));
                    String ch = target.isEmpty() ? "?" : String.valueOf(target.charAt(0)).toUpperCase();
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(ch, (90 - fm.stringWidth(ch)) / 2, (90 + fm.getAscent() - fm.getDescent()) / 2);
                }

                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(90, 90);
                }

                @Override
                public boolean isOpaque() {
                    return false;
                }
            };
            avatarCircle.setAlignmentX(CENTER_ALIGNMENT);

            JLabel targetName = new JLabel(target);
            targetName.setFont(new Font("Segoe UI", Font.BOLD, 22));
            targetName.setForeground(Color.WHITE);
            targetName.setAlignmentX(CENTER_ALIGNMENT);

            statusLabel = new JLabel("Connecting...");
            statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            statusLabel.setForeground(new Color(37, 211, 102));
            statusLabel.setAlignmentX(CENTER_ALIGNMENT);

            timerLabel = new JLabel("00:00");
            timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            timerLabel.setForeground(new Color(200, 200, 200));
            timerLabel.setAlignmentX(CENTER_ALIGNMENT);

            // Simulate "Connecting..." → "Connected" after 2s
            Timer connectTimer = new Timer(2000, e -> {
                statusLabel.setText("Connected");
                statusLabel.setForeground(VC_GREEN);
            });
            connectTimer.setRepeats(false);
            connectTimer.start();

            centerInfo.add(Box.createVerticalGlue());
            centerInfo.add(avatarCircle);
            centerInfo.add(Box.createVerticalStrut(12));
            centerInfo.add(targetName);
            centerInfo.add(Box.createVerticalStrut(6));
            centerInfo.add(statusLabel);
            centerInfo.add(Box.createVerticalStrut(8));
            centerInfo.add(timerLabel);
            centerInfo.add(Box.createVerticalGlue());

            mainVideoPanel.add(centerInfo);

            // ── Self preview (bottom-right corner) ───────────────────────
            JPanel selfPreview = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(30, 45, 55));
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                    if (camOff) {
                        g2.setColor(new Color(60, 70, 80));
                        g2.fillOval(getWidth() / 2 - 20, getHeight() / 2 - 20, 40, 40);
                        g2.setColor(Color.WHITE);
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                        FontMetrics fm = g2.getFontMetrics();
                        String ch = String.valueOf(myName.charAt(0)).toUpperCase();
                        g2.drawString(ch, (getWidth() - fm.stringWidth(ch)) / 2,
                                getHeight() / 2 + (fm.getAscent() - fm.getDescent()) / 2);
                    } else {
                        // Simulated camera "green noise" effect
                        g2.setColor(new Color(20, 60, 50, 180));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                        g2.setColor(new Color(255, 255, 255, 80));
                        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                        g2.drawString("You", 6, 16);
                    }
                }

                @Override
                public boolean isOpaque() {
                    return false;
                }
            };
            selfPreview.setBounds(0, 0, 130, 90);
            selfPreview.setPreferredSize(new Dimension(130, 90));

            JPanel videoLayer = new JPanel(null); // absolute layout for overlay
            videoLayer.setOpaque(false);
            videoLayer.add(mainVideoPanel).setBounds(0, 0, 700, 400);

            // ── Bottom controls bar ────────────────────────────────────────
            JPanel controls = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    g.setColor(VC_DARK);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }

                @Override
                public boolean isOpaque() {
                    return false;
                }
            };
            controls.setLayout(new FlowLayout(FlowLayout.CENTER, 18, 14));

            // Mute button — reads muteBg[] at paint time so toggle colour works
            final Color[] muteBg = { VC_BTN_BG };
            JButton muteBtn = new JButton("\uD83C\uDF99") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getModel().isRollover() ? muteBg[0].brighter() : muteBg[0]);
                    g2.fillOval(0, 0, getWidth(), getHeight());
                    g2.setColor(Color.WHITE);
                    g2.setFont(getFont());
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                    int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                    g2.drawString(getText(), tx, ty);
                    g2.dispose();
                }

                @Override
                public boolean isOpaque() {
                    return false;
                }
            };
            muteBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            muteBtn.setFocusPainted(false);
            muteBtn.setBorderPainted(false);
            muteBtn.setContentAreaFilled(false);
            muteBtn.setPreferredSize(new Dimension(56, 56));
            muteBtn.setToolTipText("Mute");
            muteBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            muteBtn.addActionListener(e -> {
                muted = !muted;
                muteBg[0] = muted ? VC_RED : VC_BTN_BG;
                muteBtn.setToolTipText(muted ? "Unmute" : "Mute");
                muteBtn.repaint();
            });

            // Camera toggle button
            JButton camBtn = makeCallButton("\uD83D\uDCF9", VC_BTN_BG, "Toggle Camera");
            camBtn.addActionListener(e -> {
                camOff = !camOff;
                camBtn.setToolTipText(camOff ? "Enable Camera" : "Disable Camera");
                mainVideoPanel.repaint();
                selfPreview.repaint();
            });

            // Screen share button
            JButton screenBtn = makeCallButton("\uD83D\uDDB5", VC_BTN_BG, "Screen Share (demo)");
            screenBtn.addActionListener(e -> JOptionPane.showMessageDialog(this,
                    "Screen sharing would start here.", "Screen Share", JOptionPane.INFORMATION_MESSAGE));

            // End call button
            JButton endBtn = makeCallButton("\uD83D\uDCF5", VC_RED, "End Call");
            endBtn.addActionListener(e -> {
                callTimer.stop();
                dispose();
            });

            controls.add(muteBtn);
            controls.add(camBtn);
            controls.add(screenBtn);
            controls.add(endBtn);

            // ── Self preview wrapper ──
            JLayeredPane layered = new JLayeredPane();
            layered.setPreferredSize(new Dimension(700, 400));
            layered.setOpaque(false);
            mainVideoPanel.setBounds(0, 0, 700, 400);
            selfPreview.setBounds(700 - 140, 400 - 100, 130, 90);
            layered.add(mainVideoPanel, JLayeredPane.DEFAULT_LAYER);
            layered.add(selfPreview, JLayeredPane.PALETTE_LAYER);

            root.add(layered, BorderLayout.CENTER);
            root.add(controls, BorderLayout.SOUTH);
            setContentPane(root);
        }

        private JButton makeCallButton(String icon, Color bg, String tooltip) {
            JButton btn = new JButton(icon) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getModel().isRollover() ? bg.brighter() : bg);
                    g2.fillOval(0, 0, getWidth(), getHeight());
                    g2.setColor(Color.WHITE);
                    g2.setFont(getFont());
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                    int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                    g2.drawString(getText(), tx, ty);
                    g2.dispose();
                }

                @Override
                public boolean isOpaque() {
                    return false;
                }
            };
            btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setPreferredSize(new Dimension(56, 56));
            btn.setToolTipText(tooltip);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return btn;
        }
    }
}