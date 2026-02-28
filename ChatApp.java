import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Main launcher for the WhatsApp-style Chat Application.
 * Provides options to start the server or the chat client.
 */
public class ChatApp extends JFrame {

    public ChatApp() {
        setTitle("ChitChat");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 550);
        setLocationRelativeTo(null);
        setResizable(true);
        initializeUI();
    }

    private void initializeUI() {
        // Use a custom panel with gradient background
        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(18, 140, 126), 0, getHeight(),
                        new Color(7, 94, 84));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        // ---- Top logo/header area ----
        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(new EmptyBorder(50, 20, 30, 20));

        // WhatsApp icon (circle with letter)
        JLabel iconLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(Color.WHITE);
                g2d.fillOval(0, 0, 80, 80);
                g2d.setColor(new Color(18, 140, 126));
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 42));
                FontMetrics fm = g2d.getFontMetrics();
                String text = "C";
                int x = (80 - fm.stringWidth(text)) / 2;
                int y = (80 + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(text, x, y);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(80, 80);
            }
        };
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("ChitChat");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Chat. Connect. Together.");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(255, 255, 255, 180));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(iconLabel);
        headerPanel.add(Box.createVerticalStrut(16));
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(6));
        headerPanel.add(subtitleLabel);

        // ---- Card panel (white rounded card) ----
        JPanel cardPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(Color.WHITE);
                g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 24, 24));
            }

            @Override
            public boolean isOpaque() {
                return false;
            }
        };
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel chooseLabel = new JLabel("Choose a mode to get started");
        chooseLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chooseLabel.setForeground(new Color(120, 120, 120));
        chooseLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        cardPanel.add(chooseLabel);
        cardPanel.add(Box.createVerticalStrut(20));

        // Start Server button
        GreenButton startServerBtn = new GreenButton(" Start Server", new Color(18, 140, 126), new Color(7, 94, 84));
        startServerBtn.addActionListener(e -> startServer());
        cardPanel.add(startServerBtn);

        cardPanel.add(Box.createVerticalStrut(14));

        // Start Client button
        GreenButton startClientBtn = new GreenButton(" Start Client", new Color(37, 211, 102),
                new Color(18, 140, 126));
        startClientBtn.addActionListener(e -> startClient());
        cardPanel.add(startClientBtn);

        cardPanel.add(Box.createVerticalStrut(20));

        JLabel noteLabel = new JLabel(
                "<html><center>Start the server first, then<br>connect multiple clients.</center></html>");
        noteLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        noteLabel.setForeground(new Color(150, 150, 150));
        noteLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(noteLabel);

        // ---- Wrap card with padding ----
        JPanel cardWrapper = new JPanel(new BorderLayout());
        cardWrapper.setOpaque(false);
        cardWrapper.setBorder(new EmptyBorder(0, 24, 30, 24));
        cardWrapper.add(cardPanel, BorderLayout.CENTER);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(cardWrapper, BorderLayout.CENTER);

        setContentPane(mainPanel);
    }

    /** A custom rounded green button */
    static class GreenButton extends JButton {
        private final Color c1, c2;
        private boolean hovered = false;

        GreenButton(String text, Color c1, Color c2) {
            super(text);
            this.c1 = c1;
            this.c2 = c2;
            setFont(new Font("Segoe UI", Font.BOLD, 15));
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            setPreferredSize(new Dimension(260, 50));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.CENTER_ALIGNMENT);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color bright = hovered ? c1.brighter() : c1;
            Color dark = hovered ? c2.brighter() : c2;
            GradientPaint gp = new GradientPaint(0, 0, bright, getWidth(), 0, dark);
            g2d.setPaint(gp);
            g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));

            // Draw text manually so it is always visible
            g2d.setColor(Color.WHITE);
            g2d.setFont(getFont());
            FontMetrics fm = g2d.getFontMetrics();
            int textX = (getWidth() - fm.stringWidth(getText())) / 2;
            int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            g2d.drawString(getText(), textX, textY);

            g2d.dispose();
        }
    }

    private void startServer() {
        int result = JOptionPane.showConfirmDialog(
                this,
                "Start the chat server on port 9999?\nKeep this window open while clients are connected.",
                "Start Server",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            Thread serverThread = new Thread(() -> {
                try {
                    WhatsAppServer.main(new String[] {});
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                            "Error starting server: " + ex.getMessage(),
                            "Server Error", JOptionPane.ERROR_MESSAGE));
                }
            });
            serverThread.setDaemon(false);
            serverThread.start();

            JOptionPane.showMessageDialog(this,
                    "✅ Server started on port 9999\nWaiting for clients to connect...",
                    "Server Running", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void startClient() {
        SwingUtilities.invokeLater(() -> {
            try {
                WhatsAppClient client = new WhatsAppClient();
                client.setVisible(true);
                client.connect();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed to start client: " + ex.getMessage(),
                        "Client Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> new ChatApp().setVisible(true));
    }
}