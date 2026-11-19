package agent.ui;

import agent.Agent;
import agent.Config;
import agent.Tool;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Yerel Ajan — AGI Deneysel V1 arayüzü.
 * 3040 estetiği: neon gradyanlar, cam paneller, balon mesajlar,
 * animasyonlu durum göstergeleri ve zengin bilgi paneli.
 */
public final class MainGUI {
    private static final Color BG = new Color(8, 11, 20);
    private static final Color PANEL = new Color(15, 20, 34);
    private static final Color CARD = new Color(21, 28, 46);
    private static final Color FG = new Color(226, 236, 255);
    private static final Color MUTED = new Color(130, 152, 190);
    private static final Color CYAN = new Color(64, 224, 255);
    private static final Color PURPLE = new Color(168, 120, 255);
    private static final Color GREEN = new Color(80, 240, 170);
    private static final Color AMBER = new Color(255, 205, 100);
    private static final Color RED = new Color(255, 110, 130);

    private final Agent agent;
    private final JFrame frame;
    private final JTextPane chat = new JTextPane();
    private final JTextField input = new JTextField();
    private final JTextArea log = new JTextArea();
    private final JTextPane infoPane = new JTextPane();
    private final JTextArea memoryArea = new JTextArea();
    private final JTextArea graphArea = new JTextArea();
    private final JTextArea goalsArea = new JTextArea();
    private final JTextArea nlpArea = new JTextArea();
    private final JTextArea immuneArea = new JTextArea();
    private final JProgressBar progress = new JProgressBar();
    private final TypingIndicator typing = new TypingIndicator();
    private final JLabel statusLeft = new JLabel();
    private final JLabel statusRight = new JLabel();
    private final DefaultListModel<String> toolsModel = new DefaultListModel<String>();
    private final DefaultListModel<String> sourcesModel = new DefaultListModel<String>();
    private final List<String> history = new ArrayList<String>();
    private int histIndex = -1;
    private PrintStream originalOut;
    private String currentModel;

    public MainGUI(String model) {
        this.currentModel = model;
        setupUi();
        this.agent = new Agent(model);

        frame = new JFrame("YEREL AJAN  /  AGI Deneysel V1");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(1320, 860);
        frame.setMinimumSize(new Dimension(1024, 680));
        frame.setLocationRelativeTo(null);

        frame.setJMenuBar(createMenu());
        frame.add(new HeaderPanel(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar(), chatPanel());
        split.setDividerLocation(430);
        split.setDividerSize(4);
        split.setResizeWeight(0);
        frame.add(split, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(PANEL);
        progress.setIndeterminate(false);
        progress.setForeground(CYAN);
        progress.setPreferredSize(new Dimension(100, 3));
        bottom.add(progress, BorderLayout.NORTH);
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(PANEL);
        bar.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 14));
        statusLeft.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLeft.setForeground(MUTED);
        statusRight.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusRight.setForeground(MUTED);
        bar.add(statusLeft, BorderLayout.WEST);
        bar.add(statusRight, BorderLayout.EAST);
        bottom.add(bar, BorderLayout.SOUTH);
        frame.add(bottom, BorderLayout.SOUTH);

        shortcuts();
        dragDrop();

        try {
            agent.check();
            appendSystem("Sistem çevrimiçi. Tüm katmanlar arka planda aktif.");
            refreshAll();
        } catch (Exception e) {
            appendError("Bağlantı hatası: " + e.getMessage());
        }

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                if (originalOut != null) System.setOut(originalOut);
                frame.dispose(); System.exit(0);
            }
        });
        originalOut = System.out;
        System.setOut(new GuiPrintStream(originalOut, log));
        frame.setVisible(true);
        input.requestFocus();
    }

    private void setupUi() {
        try { UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel"); } catch (Exception ignored) {}
        UIManager.put("Panel.background", PANEL);
        UIManager.put("Label.foreground", FG);
        UIManager.put("TextArea.background", BG);
        UIManager.put("TextArea.foreground", FG);
        UIManager.put("TextPane.background", BG);
        UIManager.put("TextPane.foreground", FG);
        UIManager.put("TextField.background", new Color(13, 18, 30));
        UIManager.put("TextField.foreground", FG);
        UIManager.put("List.background", BG);
        UIManager.put("List.foreground", FG);
        UIManager.put("List.selectionBackground", CYAN.darker());
        UIManager.put("TabbedPane.background", PANEL);
        UIManager.put("TabbedPane.foreground", FG);
        UIManager.put("TabbedPane.underlineColor", CYAN);
        UIManager.put("MenuBar.background", PANEL);
        UIManager.put("Menu.background", PANEL);
        UIManager.put("Menu.foreground", FG);
        UIManager.put("MenuItem.background", PANEL);
        UIManager.put("MenuItem.foreground", FG);
        UIManager.put("ComboBox.background", BG);
        UIManager.put("ComboBox.foreground", FG);
        UIManager.put("CheckBox.background", PANEL);
        UIManager.put("CheckBox.foreground", FG);
        UIManager.put("ScrollBar.thumb", new Color(60, 80, 115));
        UIManager.put("ScrollBar.track", PANEL);
    }

    private JMenuBar createMenu() {
        JMenuBar bar = new JMenuBar();
        bar.setBackground(PANEL);
        bar.setBorder(BorderFactory.createLineBorder(new Color(40, 60, 95), 1));
        JMenu file = new JMenu("Sistem");
        file.setBackground(PANEL); file.setForeground(FG);
        file.add(item("Yeni Oturum", e -> { agent.clearHistory(); chat.setText(""); appendSystem("Yeni oturum."); }));
        file.add(item("Sohbeti Kaydet", e -> saveTranscript()));
        file.add(item("Çıkış", e -> frame.dispatchEvent(new java.awt.event.WindowEvent(
                frame, java.awt.event.WindowEvent.WINDOW_CLOSING))));
        JMenu view = new JMenu("Görünüm");
        view.setBackground(PANEL); view.setForeground(FG);
        view.add(item("AGI Profili", e -> { infoPane.setContentType("text/html"); infoPane.setText(agiHtml()); showTab("Bilgi"); }));
        view.add(item("Kısayollar", e -> JOptionPane.showMessageDialog(frame,
                "Ctrl+N Yeni oturum | Ctrl+S Kaydet | Ctrl+L Temizle | Ctrl+O Görüntü",
                "Kısayollar", JOptionPane.INFORMATION_MESSAGE)));
        bar.add(file); bar.add(view);
        return bar;
    }

    private JMenuItem item(String t, ActionListener l) {
        JMenuItem m = new JMenuItem(t); m.addActionListener(l); return m;
    }

    /** Animasyonlu neon başlık. */
    private final class HeaderPanel extends JPanel {
        private float phase = 0;
        HeaderPanel() {
            setPreferredSize(new Dimension(100, 96));
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 24));
            JPanel left = new JPanel();
            left.setOpaque(false);
            left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
            JLabel title = new JLabel("YEREL  AJAN");
            title.setFont(new Font("Segoe UI", Font.BOLD, 26));
            title.setForeground(Color.WHITE);
            JLabel sub = new JLabel("A G I   •   D E N E Y S E L   V 1     /     model-as-tool mimarisi");
            sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            sub.setForeground(new Color(180, 215, 255));
            left.add(Box.createVerticalStrut(18)); left.add(title); left.add(sub);
            add(left, BorderLayout.WEST);
            new Timer(60, e -> { phase += 0.02f; repaint(); }).start();
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            float a = (float) (0.5 + 0.5 * Math.sin(phase));
            g2.setPaint(new GradientPaint(0, 0, new Color(12, 20 + (int)(a*20), 44),
                    w, h, new Color(28, 12, 52)));
            g2.fillRect(0, 0, w, h);
            for (int i = 0; i < 5; i++) {
                int x = (int) ((i * 260 + phase * 60) % (w + 200)) - 100;
                g2.setColor(new Color(64, 224, 255, 18));
                g2.fillOval(x, h - 60, 120, 120);
            }
            g2.setColor(CYAN);
            g2.fillRect(0, h - 3, w, 3);
            g2.dispose();
        }
    }

    private JPanel sidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(PANEL);
        side.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 8));

        JPanel card = new JPanel(new java.awt.GridLayout(4, 1, 4, 4));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JLabel lbl = new JLabel("KATMAN KONTROLLERİ");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(CYAN);
        card.add(lbl);
        JPanel t1 = row(new JCheckBox("Planner", agent.plannerEnabled()),
                        new JCheckBox("Reflection", agent.reflectionEnabled()));
        JPanel t2 = row(new JCheckBox("Çok Ajan", agent.multiAgentEnabled()),
                        new JCheckBox("Dünya Modeli", agent.graphEnabled()));
        JPanel t3 = row(new JCheckBox("Özerklik", agent.autonomyEnabled()),
                        new JCheckBox("Hızlı Mod", agent.fastMode()));
        card.add(t1); card.add(t2); card.add(t3);
        side.add(card, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabs.addTab("Bilgi", scroll(infoPane));
        tabs.addTab("Bellek", area(memoryArea));
        tabs.addTab("Grafik", area(graphArea));
        tabs.addTab("Hedefler", area(goalsArea));
        tabs.addTab("Metin", area(nlpArea));
        tabs.addTab("Bağışıklık", area(immuneArea));
        tabs.addTab("Araçlar", list(toolsModel));
        tabs.addTab("Kaynaklar", sources(sourcesModel));
        tabs.addTab("Log", area(log));
        side.add(tabs, BorderLayout.CENTER);
        return side;
    }

    private JPanel row(JCheckBox a, JCheckBox b) {
        JPanel p = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 2, 2));
        p.setBackground(CARD);
        for (JCheckBox c : new JCheckBox[]{a, b}) {
            c.setBackground(CARD); c.setForeground(FG); c.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            p.add(c);
        }
        a.addActionListener(e -> { agent.setPlanner(a.isSelected()); updateStatus(); });
        b.addActionListener(e -> { agent.setReflection(b.isSelected()); updateStatus(); });
        return p;
    }

    private JScrollPane area(JTextArea a) {
        a.setEditable(false); a.setLineWrap(true); a.setWrapStyleWord(true);
        a.setFont(new Font("Consolas", Font.PLAIN, 12)); a.setBackground(BG); a.setForeground(FG);
        return new JScrollPane(a);
    }

    private JScrollPane scroll(JTextPane p) {
        p.setEditable(false); p.setBackground(BG); p.setForeground(FG);
        return new JScrollPane(p);
    }

    private JPanel list(DefaultListModel<String> m) {
        JPanel p = new JPanel(new BorderLayout());
        JList<String> l = new JList<String>(m);
        l.setFont(new Font("Consolas", Font.PLAIN, 12));
        p.add(new JScrollPane(l), BorderLayout.CENTER);
        return p;
    }

    private JPanel sources(DefaultListModel<String> m) {
        JPanel p = list(m);
        NeonButton add = new NeonButton("+  KAYNAK EKLE", PURPLE);
        add.addActionListener(e -> {
            String url = JOptionPane.showInputDialog(frame, "Kaynak URL:");
            if (url != null && !url.trim().isEmpty()) { agent.addSource(url.trim()); refreshAll(); }
        });
        p.add(add, BorderLayout.SOUTH);
        return p;
    }

    private JPanel chatPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JScrollPane scroll = new JScrollPane(chat);
        scroll.setBorder(null);
        scroll.setBackground(BG);
        scroll.getViewport().setBackground(BG);
        scroll.getViewport().setOpaque(true);
        chat.setOpaque(true);
        chat.setEditable(false);
        chat.setBackground(BG);
        chat.setForeground(FG);
        chat.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        p.add(scroll, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(8, 4));
        south.setBackground(BG);
        south.add(typing, BorderLayout.NORTH);
        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setBackground(BG);
        input.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        input.setBackground(new Color(13, 18, 30));
        input.setForeground(FG);
        input.setCaretColor(CYAN);
        input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(50, 120, 170), 1, true),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        input.addActionListener(e -> send());
        inputRow.add(input, BorderLayout.CENTER);
        NeonButton send = new NeonButton("GÖNDER  ↵", CYAN);
        send.addActionListener(e -> send());
        inputRow.add(send, BorderLayout.EAST);
        NeonButton img = new NeonButton("⊕  GÖRÜNTÜ", PURPLE);
        img.addActionListener(e -> chooseImage());
        south.add(img, BorderLayout.WEST);
        south.add(inputRow, BorderLayout.CENTER);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    /** Neon yuvarlak buton. */
    private static final class NeonButton extends JButton {
        private final Color base;
        private boolean hover;
        NeonButton(String text, Color base) {
            super(text);
            this.base = base;
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
            setContentAreaFilled(false); setFocusPainted(false); setOpaque(false);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(hover ? base.brighter() : base);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Animasyonlu "yazıyor" göstergesi. */
    private static final class TypingIndicator extends JPanel {
        private int frame = 0;
        TypingIndicator() {
            setOpaque(false);
            setPreferredSize(new Dimension(80, 14));
            setVisible(false);
            new Timer(120, e -> { frame = (frame + 1) % 4; repaint(); }).start();
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (!isVisible()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            for (int i = 0; i < 3; i++) {
                g2.setColor(i == frame ? CYAN : new Color(70, 90, 120));
                g2.fillOval(8 + i * 18, 4, 8, 8);
            }
            g2.dispose();
        }
    }

    private void send() {
        String text = input.getText().trim();
        if (text.isEmpty()) return;
        input.setText("");
        history.add(text); histIndex = history.size();
        appendBubble("SEN", text, true, GREEN);
        run(type(text) ? "cmd" : "mesaj", text);
    }

    private boolean type(String s) { return s.startsWith("/"); }

    private void run(final String kind, final String data) {
        typing.setVisible(true);
        progress.setIndeterminate(true);
        statusLeft.setText("●  Biliş işleniyor...");
        SwingWorker<String, Void> w = new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                if (kind.equals("cmd")) return command(data);
                return agent.respond(data);
            }
            @Override protected void done() {
                typing.setVisible(false);
                progress.setIndeterminate(false);
                try {
                    String r = get();
                    if (!kind.equals("cmd") && r != null) appendBubble("AGI", r, false, AMBER);
                    refreshAll();
                } catch (Exception e) {
                    appendBubble("SİSTEM", "Hata: " + e.getMessage(), false, RED);
                }
            }
        };
        w.execute();
    }

    /** Görünmez komut desteği — kullanıcı komut yazmak zorunda değil. */
    private String command(String raw) {
        String c = raw.toLowerCase();
        if (c.equals("/clear")) { chat.setText(""); return ""; }
        if (c.equals("/agi") || c.equals("/core") || c.equals("/selfmodel")) {
            String r = c.equals("/agi") ? agent.agiReport()
                    : c.equals("/core") ? agent.coreReport() : agent.selfModelText();
            infoPane.setContentType("text/html"); infoPane.setText(agiHtml());
            appendSystem(r); return "";
        }
        if (c.startsWith("/nlp ")) {
            String r = agent.analyzeText(raw.substring(5));
            nlpArea.setText(r); appendSystem(r); return "";
        }
        if (c.startsWith("/research ")) { appendSystem(agent.researchUrl(raw.substring(10))); return ""; }
        if (c.equals("/research-all")) { appendSystem(agent.researchSources(3)); return ""; }
        if (c.startsWith("/model ")) {
            agent.setModel(raw.substring(7).trim()); currentModel = agent.model();
            appendSystem("Model: " + currentModel); return "";
        }
        if (c.equals("/eval")) { appendSystem(agent.runEvaluation()); return ""; }
        if (c.equals("/bench")) { appendSystem(agent.runBenchmark()); return ""; }
        appendSystem("Bilinmeyen komut: " + raw);
        return "";
    }

    private void appendBubble(String who, String text, boolean user, Color color) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = chat.getStyledDocument();
            SimpleAttributeSet head = new SimpleAttributeSet();
            StyleConstants.setForeground(head, color);
            StyleConstants.setBold(head, true);
            StyleConstants.setFontSize(head, 12);
            StyleConstants.setAlignment(head, user ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT);
            SimpleAttributeSet body = new SimpleAttributeSet();
            StyleConstants.setForeground(body, FG);
            StyleConstants.setFontSize(body, 15);
            StyleConstants.setAlignment(body, user ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT);
            try {
                doc.insertString(doc.getLength(), (user ? "" : "") + who + "  ", head);
                doc.insertString(doc.getLength(), text == null ? "" : text + "\n\n", body);
                doc.setParagraphAttributes(doc.getLength() - (text == null ? 2 : text.length() + 2),
                        (text == null ? 2 : text.length() + 2), user ? head : body, false);
                chat.setCaretPosition(doc.getLength());
            } catch (BadLocationException ignored) {}
        });
    }

    private void appendSystem(String text) { appendBubble("SİSTEM", text, false, MUTED); }
    private void appendError(String text) { appendBubble("HATA", text, false, RED); }

    private String agiHtml() {
        String a = agent.agiReport().replace("\n", "<br>");
        String s = agent.selfModelText().replace("\n", "<br>");
        String c = agent.coreReport().replace("\n", "<br>");
        String i = agent.immuneReport().replace("\n", "<br>");
        return "<html><body style='background:#0b0f19;color:#dce8ff;font-family:Segoe UI;font-size:12px'>"
                + "<h2 style='color:#40e0ff;margin:4px'>YEREL AJAN · AGI DENEYSEL V1</h2>"
                + "<p style='color:#8fa8d0'>Model-as-Tool mimarisi · her katar arka planda çalışır</p>"
                + "<hr color='#2a3a5a'>"
                + "<h3 style='color:#a878ff'>YETENEK PROFİLİ</h3><p>" + a + "</p>"
                + "<h3 style='color:#a878ff'>ÖZ-MODEL</h3><p>" + s + "</p>"
                + "<h3 style='color:#a878ff'>BİLİŞSEL ÇEKİRDEK</h3><p>" + c + "</p>"
                + "<h3 style='color:#a878ff'>BAĞIŞIKLIK</h3><p>" + i + "</p>"
                + "<h3 style='color:#a878ff'>MODEL YÖNLENDİRME</h3><p>"
                + "Hızlı: " + agent.model() + "<br>Akıl: " + agent.reasoningModel()
                + "<br>Yedek: " + agent.fallbackChain().replace("→", "›") + "</p>"
                + "<h3 style='color:#a878ff'>PERFORMANS</h3><p>" + agent.metrics().summary().replace("\n", "<br>") + "</p>"
                + "<h3 style='color:#a878ff'>DÜNYA MODELİ</h3><p>" + agent.worldStats()
                + "<br>" + agent.curriculumReport() + "</p>"
                + "</body></html>";
    }

    private void refreshAll() {
        infoPane.setContentType("text/html"); infoPane.setText(agiHtml());
        memoryArea.setText(agent.memoryList());
        graphArea.setText(agent.graphStats() + "\n\nVarlıklar:\n  " +
                String.join("\n  ", agent.graphEntities()) + "\n\nİlişkiler:\n  " +
                String.join("\n  ", agent.graphRelations()));
        goalsArea.setText(agent.renderGoals());
        immuneArea.setText(agent.immuneReport() + "\n\n" + agent.healingReport() + "\n\n" +
                "Koruma: kullanıcı · web · ajanlar · hedefler · zihin virüsü");
        toolsModel.clear();
        for (Tool t : agent.tools()) toolsModel.addElement(t.name() + " — " + t.description());
        sourcesModel.clear();
        for (String s : agent.researchSeeds().split("\n")) if (!s.trim().isEmpty()) sourcesModel.addElement(s.trim());
        updateStatus();
    }

    private void updateStatus() {
        statusLeft.setText("  ⬤  MODEL " + agent.model() +
                "   |   AKIL " + agent.reasoningModel() +
                "   |   PLANNER " + on(agent.plannerEnabled()) +
                "   |   REFLECTION " + on(agent.reflectionEnabled()) +
                "   |   MOD " + (agent.multiAgentEnabled() ? "ÇOK AJAN" : "TEK AJAN"));
        statusRight.setText("AGI-v1 DENEYSEL   |   HAFIZA " + agent.memorySize() +
                "   |   ÖĞRENEN " + agent.learnedCount() +
                "   |   BAĞIŞIKLIK " + agent.immuneBlocked() +
                "   |   " + agent.graphStats() + "   ");
    }

    private static String on(boolean b) { return b ? "AÇIK" : "KAPALI"; }
    private void showTab(String name) {
        for (java.awt.Component c : frame.getComponents()) {
            if (c instanceof JSplitPane) {
                java.awt.Component l = ((JSplitPane) c).getLeftComponent();
                if (l instanceof JTabbedPane) {
                    JTabbedPane t = (JTabbedPane) l;
                    for (int i = 0; i < t.getTabCount(); i++) {
                        if (t.getTitleAt(i).equals(name)) { t.setSelectedIndex(i); return; }
                    }
                }
            }
        }
    }

    private void shortcuts() {
        bind("ctrl N", e -> { agent.clearHistory(); chat.setText(""); appendSystem("Yeni oturum."); });
        bind("ctrl S", e -> saveTranscript());
        bind("ctrl L", e -> chat.setText(""));
        bind("ctrl O", e -> chooseImage());
    }

    private void bind(String key, ActionListener l) {
        input.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), key);
        input.getActionMap().put(key, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { l.actionPerformed(e); }
        });
    }

    private void chooseImage() {
        JFileChooser fc = new JFileChooser(".");
        if (fc.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        String q = JOptionPane.showInputDialog(frame, "Bu görüntü hakkında ne sormak istersiniz?", "Bu resimde ne var?");
        if (q == null || q.trim().isEmpty()) return;
        appendBubble("SEN", "[görüntü: " + f.getName() + "] " + q, true, GREEN);
        final String question = q;
        final String path = f.getAbsolutePath();
        typing.setVisible(true);
        SwingWorker<String, Void> w = new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception { return agent.respondWithImage(path, question); }
            @Override protected void done() {
                typing.setVisible(false);
                try { appendBubble("AGI", get(), false, AMBER); }
                catch (Exception e) { appendError(e.getMessage()); }
                refreshAll();
            }
        };
        w.execute();
    }

    private void dragDrop() {
        chat.setDropTarget(new java.awt.dnd.DropTarget() {
            @Override public void drop(java.awt.dnd.DropTargetDropEvent e) {
                try {
                    e.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY);
                    java.util.List<?> files = (java.util.List<?>) e.getTransferable()
                            .getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
                    if (files != null && !files.isEmpty()) {
                        File f = (File) files.get(0);
                        input.setText("[görüntü: " + f.getAbsolutePath() + "] Bu resimde ne var?");
                    }
                    e.dropComplete(true);
                } catch (Exception ex) { e.dropComplete(false); }
            }
        });
    }

    private void saveTranscript() {
        JFileChooser fc = new JFileChooser(".");
        fc.setSelectedFile(new File("sohbet.txt"));
        if (fc.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        try {
            java.nio.file.Files.write(fc.getSelectedFile().toPath(),
                    chat.getText().getBytes(StandardCharsets.UTF_8));
            appendSystem("Sohbet kaydedildi.");
        } catch (Exception e) { appendError("Kaydedilemedi: " + e.getMessage()); }
    }

    private static final class GuiPrintStream extends PrintStream {
        private final PrintStream original;
        private final JTextArea area;
        GuiPrintStream(PrintStream original, JTextArea area) {
            super(new OutputStream() { @Override public void write(int b) {} });
            this.original = original; this.area = area;
        }
        @Override public void write(byte[] b, int off, int len) {
            String s = new String(b, off, len, StandardCharsets.UTF_8);
            original.print(s);
            SwingUtilities.invokeLater(() -> {
                area.append(s);
                area.setCaretPosition(area.getDocument().getLength());
            });
        }
        @Override public void write(int b) { write(new byte[]{(byte) b}, 0, 1); }
    }

    public static void main(String[] args) {
        String model = args.length > 0 ? args[0] : Config.load().model;
        SwingUtilities.invokeLater(() -> new MainGUI(model));
    }
}
