import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.geom.*;
import java.sql.*;
import java.util.*;
import java.text.SimpleDateFormat;

/**
 * =====================================================
 *   PERSONAL EXPENSE ANALYZER
 *   Built with: Java Swing + JDBC + Graphics2D Charts
 *
 *   Features:
 *   ✅ Add expenses (amount, category, description, date)
 *   ✅ Delete expenses
 *   ✅ View all expenses in a table
 *   ✅ BAR CHART — spending by category
 *   ✅ PIE CHART — category breakdown %
 *   ✅ Monthly total calculation
 *   ✅ Highest spending category highlight
 *   ✅ Summary cards (Total, Avg, Highest Category)
 *   ✅ Filter by month
 *
 *  ⚠ CHANGE DB_PASSWORD below to your MySQL password!
 * =====================================================
 */
public class ExpenseAnalyzer extends JFrame {

    // ─── DATABASE CONFIG ───────────────────────────────────────────
    private static final String DB_URL      = "jdbc:mysql://localhost:3306/expense_db";
    private static final String DB_USER     = "root";
    private static final String DB_PASSWORD = "0709";  // ← CHANGE THIS

    // ─── COLORS ────────────────────────────────────────────────────
    private static final Color BG       = new Color(15,  23,  42);   // Dark navy
    private static final Color CARD     = new Color(30,  41,  59);   // Card bg
    private static final Color BORDER   = new Color(51,  65,  85);   // Border
    private static final Color TEXT     = new Color(226, 232, 240);  // Light text
    private static final Color MUTED    = new Color(148, 163, 184);  // Muted text
    private static final Color PRIMARY  = new Color(99,  102, 241);  // Indigo
    private static final Color SUCCESS  = new Color(34,  197, 94);   // Green
    private static final Color DANGER   = new Color(239, 68,  68);   // Red
    private static final Color WARNING  = new Color(251, 191, 36);   // Yellow
    private static final Color WHITE    = Color.WHITE;

    // Category colors for charts
    private static final Color[] CAT_COLORS = {
        new Color(99,  102, 241),  // Indigo   — Food
        new Color(34,  197, 94),   // Green    — Transport
        new Color(251, 191, 36),   // Yellow   — Shopping
        new Color(239, 68,  68),   // Red      — Health
        new Color(14,  165, 233),  // Blue     — Education
        new Color(168, 85,  247),  // Purple   — Entertainment
        new Color(249, 115, 22),   // Orange   — Bills
        new Color(236, 72,  153),  // Pink     — Other
    };

    private static final String[] CATEGORIES = {
        "Food", "Transport", "Shopping", "Health",
        "Education", "Entertainment", "Bills", "Other"
    };

    // ─── SWING COMPONENTS ──────────────────────────────────────────
    private JTextField amountField, descField;
    private JComboBox<String> categoryCombo, monthFilter;
    private JTable expenseTable;
    private DefaultTableModel tableModel;
    private JLabel totalLabel, avgLabel, topCatLabel, countLabel;
    private ChartPanel barChartPanel, pieChartPanel;
    private JLabel statusLabel;
    private int selectedId = -1;

    // ─── DATA ──────────────────────────────────────────────────────
    private Map<String, Double> categoryTotals = new LinkedHashMap<>();
    private double grandTotal = 0;

    // ───────────────────────────────────────────────────────────────
    //  CONSTRUCTOR
    // ───────────────────────────────────────────────────────────────
    public ExpenseAnalyzer() {
        setTitle("💰 Personal Expense Analyzer");
        setSize(1500, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(BG);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG);
        setContentPane(root);

        // TOP BAR
        root.add(buildTopBar(), BorderLayout.NORTH);

        // CENTER — split into left (form+table) and right (charts)
        JPanel center = new JPanel(new BorderLayout(12, 0));
        center.setBackground(BG);
        center.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));

        JPanel leftSide = new JPanel(new BorderLayout(0, 12));
        leftSide.setBackground(BG);
        leftSide.setPreferredSize(new Dimension(480, 0));
        leftSide.add(buildFormPanel(), BorderLayout.NORTH);
        leftSide.add(buildTablePanel(), BorderLayout.CENTER);

        center.add(leftSide, BorderLayout.WEST);
        center.add(buildChartsPanel(), BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        // BOTTOM STATUS
        root.add(buildStatusBar(), BorderLayout.SOUTH);

        refreshAll();
        setVisible(true);
    }

    // ───────────────────────────────────────────────────────────────
    //  UI BUILDERS
    // ───────────────────────────────────────────────────────────────

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(CARD);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
            BorderFactory.createEmptyBorder(14, 20, 14, 20)
        ));

        JLabel title = new JLabel("💰  Personal Expense Analyzer");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(WHITE);

        JLabel sub = new JLabel("Track · Analyze · Save Smart");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(MUTED);

        // Summary cards (top right)
        JPanel cards = new JPanel(new GridLayout(1, 4, 10, 0));
        cards.setBackground(CARD);

        totalLabel  = makeSummaryCard("Total Spent",    "₹0.00",   PRIMARY);
        avgLabel    = makeSummaryCard("Avg / Expense",  "₹0.00",   SUCCESS);
        topCatLabel = makeSummaryCard("Top Category",   "—",        WARNING);
        countLabel  = makeSummaryCard("Expenses",       "0",        new Color(14,165,233));

        cards.add(totalLabel.getParent());
        cards.add(avgLabel.getParent());
        cards.add(topCatLabel.getParent());
        cards.add(countLabel.getParent());

        JPanel left = new JPanel(new GridLayout(2,1));
        left.setBackground(CARD);
        left.add(title);
        left.add(sub);

        bar.add(left, BorderLayout.WEST);
        bar.add(cards, BorderLayout.EAST);
        return bar;
    }

    private JLabel makeSummaryCard(String label, String value, Color accent) {
        JPanel card = new JPanel(new GridLayout(2, 1));
        card.setBackground(new Color(51, 65, 85));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(MUTED);

        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 16));
        val.setForeground(accent);

        card.add(lbl);
        card.add(val);
        return val; // return the value label so we can update it
    }

    private JPanel buildFormPanel() {
        JPanel card = darkCard();
        card.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(4, 4, 4, 4);

        JLabel header = new JLabel("➕  Add Expense");
        header.setFont(new Font("Segoe UI", Font.BOLD, 15));
        header.setForeground(WHITE);
        g.gridx=0; g.gridy=0; g.gridwidth=4;
        card.add(header, g);

        // Row 1: Amount + Category
        g.gridwidth=1; g.gridy=1;
        g.gridx=0; card.add(label("Amount (₹)"), g);
        g.gridx=1;
        amountField = darkField();
        amountField.setPreferredSize(new Dimension(120, 32));
        card.add(amountField, g);

        g.gridx=2; card.add(label("Category"), g);
        g.gridx=3;
        categoryCombo = new JComboBox<>(CATEGORIES);
        styleCombo(categoryCombo);
        card.add(categoryCombo, g);

        // Row 2: Description (full width)
        g.gridy=2; g.gridx=0; card.add(label("Description"), g);
        g.gridx=1; g.gridwidth=3;
        descField = darkField();
        card.add(descField, g);

        // Row 3: Buttons
        g.gridy=3; g.gridx=0; g.gridwidth=2;
        JButton addBtn = actionBtn("💾  Add Expense", PRIMARY);
        card.add(addBtn, g);
        g.gridx=2; g.gridwidth=1;
        JButton delBtn = actionBtn("🗑 Delete", DANGER);
        card.add(delBtn, g);
        g.gridx=3;
        JButton clrBtn = actionBtn("✖ Clear", BORDER);
        card.add(clrBtn, g);

        addBtn.addActionListener(e -> addExpense());
        delBtn.addActionListener(e -> deleteExpense());
        clrBtn.addActionListener(e -> clearForm());

        return card;
    }

    private JPanel buildTablePanel() {
        JPanel card = darkCard();
        card.setLayout(new BorderLayout(0, 8));

        // Header + filter
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setBackground(CARD);

        JLabel hdr = new JLabel("📋  Expense Records");
        hdr.setFont(new Font("Segoe UI", Font.BOLD, 14));
        hdr.setForeground(WHITE);

        String[] months = {"All Months","Jan","Feb","Mar","Apr","May","Jun",
                           "Jul","Aug","Sep","Oct","Nov","Dec"};
        monthFilter = new JComboBox<>(months);
        styleCombo(monthFilter);
        monthFilter.setPreferredSize(new Dimension(130, 28));

        JButton filterBtn = actionBtn("Filter", PRIMARY);
        filterBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        filterRow.setBackground(CARD);
        filterRow.add(new JLabel("Month:") {{ setForeground(MUTED); }});
        filterRow.add(monthFilter);
        filterRow.add(filterBtn);

        top.add(hdr, BorderLayout.WEST);
        top.add(filterRow, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        // Table
        String[] cols = {"ID", "Date", "Category", "Description", "Amount (₹)"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        expenseTable = new JTable(tableModel);
        expenseTable.setBackground(new Color(15, 23, 42));
        expenseTable.setForeground(TEXT);
        expenseTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        expenseTable.setRowHeight(26);
        expenseTable.setGridColor(BORDER);
        expenseTable.setSelectionBackground(new Color(99, 102, 241, 80));
        expenseTable.setSelectionForeground(WHITE);

        JTableHeader header = expenseTable.getTableHeader();
        header.setBackground(new Color(30, 41, 59));
        header.setForeground(MUTED);
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setPreferredSize(new Dimension(0, 32));

        expenseTable.getColumnModel().getColumn(0).setPreferredWidth(30);
        expenseTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        expenseTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        expenseTable.getColumnModel().getColumn(3).setPreferredWidth(160);
        expenseTable.getColumnModel().getColumn(4).setPreferredWidth(80);

        JScrollPane scroll = new JScrollPane(expenseTable);
        scroll.setBackground(BG);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getViewport().setBackground(BG);
        card.add(scroll, BorderLayout.CENTER);

        // Row selection
        expenseTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && expenseTable.getSelectedRow() != -1) {
                int row = expenseTable.getSelectedRow();
                selectedId = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
                setStatus("Selected expense ID " + selectedId);
            }
        });

        filterBtn.addActionListener(e -> loadExpenses());
        return card;
    }

    private JPanel buildChartsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 12));
        panel.setBackground(BG);

        // Bar chart card
        JPanel barCard = darkCard();
        barCard.setLayout(new BorderLayout(0, 5));
        JLabel barTitle = new JLabel("📊  Spending by Category");
        barTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        barTitle.setForeground(WHITE);
        barCard.add(barTitle, BorderLayout.NORTH);
        barChartPanel = new ChartPanel(false);
        barCard.add(barChartPanel, BorderLayout.CENTER);

        // Pie chart card
        JPanel pieCard = darkCard();
        pieCard.setLayout(new BorderLayout(0, 5));
        JLabel pieTitle = new JLabel("🥧  Category Breakdown (%)");
        pieTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        pieTitle.setForeground(WHITE);
        pieCard.add(pieTitle, BorderLayout.NORTH);
        pieChartPanel = new ChartPanel(true);
        pieCard.add(pieChartPanel, BorderLayout.CENTER);

        panel.add(barCard);
        panel.add(pieCard);
        return panel;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(CARD);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
            BorderFactory.createEmptyBorder(6, 15, 6, 15)
        ));
        statusLabel = new JLabel("✅ Ready. Add your first expense!");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(MUTED);
        bar.add(statusLabel);
        return bar;
    }

    // ───────────────────────────────────────────────────────────────
    //  DATABASE METHODS (JDBC)
    // ───────────────────────────────────────────────────────────────

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private void addExpense() {
        String amtStr = amountField.getText().trim();
        String desc   = descField.getText().trim();
        String cat    = (String) categoryCombo.getSelectedItem();

        if (amtStr.isEmpty()) { showWarn("Please enter an amount."); return; }

        double amount;
        try {
            amount = Double.parseDouble(amtStr);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showWarn("Amount must be a valid positive number."); return;
        }

        if (desc.isEmpty()) desc = cat; // default description to category

        String sql = "INSERT INTO expenses (amount, category, description, expense_date) VALUES (?, ?, ?, CURDATE())";
        try (Connection conn = getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setDouble(1, amount);
            pst.setString(2, cat);
            pst.setString(3, desc);
            pst.executeUpdate();

            clearForm();
            refreshAll();
            setStatus("✅ Added ₹" + String.format("%.2f", amount) + " for " + cat);

        } catch (SQLException ex) {
            showError("Could not add expense: " + ex.getMessage());
        }
    }

    private void deleteExpense() {
        if (selectedId == -1) { showWarn("Select an expense row first."); return; }

        int ok = JOptionPane.showConfirmDialog(this,
            "Delete expense ID " + selectedId + "?",
            "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;

        try (Connection conn = getConnection();
             PreparedStatement pst = conn.prepareStatement("DELETE FROM expenses WHERE id=?")) {
            pst.setInt(1, selectedId);
            pst.executeUpdate();
            selectedId = -1;
            refreshAll();
            setStatus("Expense deleted.");
        } catch (SQLException ex) {
            showError("Delete failed: " + ex.getMessage());
        }
    }

    private void loadExpenses() {
        tableModel.setRowCount(0);
        int monthIdx = monthFilter.getSelectedIndex(); // 0 = All

        String sql = monthIdx == 0
            ? "SELECT * FROM expenses ORDER BY expense_date DESC"
            : "SELECT * FROM expenses WHERE MONTH(expense_date) = ? ORDER BY expense_date DESC";

        try (Connection conn = getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            if (monthIdx != 0) pst.setInt(1, monthIdx);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("expense_date"),
                    rs.getString("category"),
                    rs.getString("description"),
                    String.format("%.2f", rs.getDouble("amount"))
                });
            }
        } catch (SQLException ex) {
            showError("Could not load expenses.\n\nMake sure:\n" +
                "1. MySQL is running\n2. You ran DatabaseSetup.sql\n3. Password is correct\n\nError: " + ex.getMessage());
        }
    }

    /**
     * loadAnalysis() — GROUP BY category to calculate totals for charts
     * This is the "analysis" logic — SQL does the math for us!
     * SELECT category, SUM(amount) GROUP BY category
     */
    private void loadAnalysis() {
        categoryTotals.clear();
        grandTotal = 0;
        int count  = 0;
        String topCat = "—";
        double topAmt = 0;

        int monthIdx = monthFilter.getSelectedIndex();
        String sql = monthIdx == 0
            ? "SELECT category, SUM(amount) as total, COUNT(*) as cnt FROM expenses GROUP BY category ORDER BY total DESC"
            : "SELECT category, SUM(amount) as total, COUNT(*) as cnt FROM expenses WHERE MONTH(expense_date)=? GROUP BY category ORDER BY total DESC";

        try (Connection conn = getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            if (monthIdx != 0) pst.setInt(1, monthIdx);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                String cat  = rs.getString("category");
                double tot  = rs.getDouble("total");
                int    cnt  = rs.getInt("cnt");
                categoryTotals.put(cat, tot);
                grandTotal += tot;
                count      += cnt;
                if (tot > topAmt) { topAmt = tot; topCat = cat; }
            }

        } catch (SQLException ex) {
            // silently ignore — error shown in loadExpenses
        }

        // Update summary cards
        totalLabel.setText("₹" + String.format("%.2f", grandTotal));
        avgLabel.setText(count > 0 ? "₹" + String.format("%.2f", grandTotal / count) : "₹0.00");
        topCatLabel.setText(topCat);
        countLabel.setText(String.valueOf(count));

        // Refresh charts
        barChartPanel.setData(categoryTotals, grandTotal);
        pieChartPanel.setData(categoryTotals, grandTotal);
    }

    private void refreshAll() {
        loadExpenses();
        loadAnalysis();
    }

    // ───────────────────────────────────────────────────────────────
    //  INNER CLASS — ChartPanel (draws Bar & Pie charts with Graphics2D)
    //  No external library needed — pure Java drawing!
    // ───────────────────────────────────────────────────────────────
    class ChartPanel extends JPanel {
        private boolean isPie;
        private Map<String, Double> data = new LinkedHashMap<>();
        private double total = 0;

        ChartPanel(boolean isPie) {
            this.isPie = isPie;
            setBackground(CARD);
        }

        void setData(Map<String, Double> data, double total) {
            this.data  = data;
            this.total = total;
            repaint(); // trigger paintComponent
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            // Smooth rendering
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (data.isEmpty()) {
                g2.setColor(MUTED);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                g2.drawString("No data yet. Add some expenses!", getWidth()/2 - 120, getHeight()/2);
                return;
            }

            if (isPie) drawPieChart(g2);
            else       drawBarChart(g2);
        }

        // ── BAR CHART ─────────────────────────────────────────────
        private void drawBarChart(Graphics2D g2) {
            int pad    = 40;
            int w      = getWidth()  - pad * 2;
            int h      = getHeight() - pad * 2 - 20;
            int n      = data.size();
            if (n == 0) return;

            double maxVal = data.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
            int barW  = Math.min(50, (w / n) - 10);
            int gap   = (w - barW * n) / (n + 1);

            int i = 0;
            for (Map.Entry<String, Double> entry : data.entrySet()) {
                double val   = entry.getValue();
                int    barH  = (int) ((val / maxVal) * h);
                int    x     = pad + gap + i * (barW + gap);
                int    y     = pad + h - barH;

                Color col = CAT_COLORS[Arrays.asList(CATEGORIES).indexOf(entry.getKey()) % CAT_COLORS.length];

                // Bar gradient
                GradientPaint gp = new GradientPaint(x, y, col.brighter(), x, y + barH, col.darker());
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(x, y, barW, barH, 6, 6));

                // Amount label on top
                g2.setColor(WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                String amt = "₹" + String.format("%.0f", val);
                int tw = g2.getFontMetrics().stringWidth(amt);
                g2.drawString(amt, x + barW/2 - tw/2, y - 4);

                // Category label below
                g2.setColor(MUTED);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                String cat = entry.getKey().length() > 7 ? entry.getKey().substring(0,6)+"." : entry.getKey();
                int cw = g2.getFontMetrics().stringWidth(cat);
                g2.drawString(cat, x + barW/2 - cw/2, pad + h + 14);

                i++;
            }

            // Y-axis line
            g2.setColor(BORDER);
            g2.drawLine(pad, pad, pad, pad + h);
            g2.drawLine(pad, pad + h, pad + w, pad + h);
        }

        // ── PIE CHART ─────────────────────────────────────────────
        private void drawPieChart(Graphics2D g2) {
            int cx   = getWidth()  / 2 - 60;
            int cy   = getHeight() / 2;
            int r    = Math.min(cx, cy) - 20;
            int diam = r * 2;

            double startAngle = 0;
            int i = 0;
            for (Map.Entry<String, Double> entry : data.entrySet()) {
                double pct   = entry.getValue() / total;
                double sweep = pct * 360;

                Color col = CAT_COLORS[Arrays.asList(CATEGORIES).indexOf(entry.getKey()) % CAT_COLORS.length];
                g2.setColor(col);
                g2.fill(new Arc2D.Double(cx - r, cy - r, diam, diam, startAngle, sweep, Arc2D.PIE));

                // Thin white border between slices
                g2.setColor(CARD);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new Arc2D.Double(cx - r, cy - r, diam, diam, startAngle, sweep, Arc2D.PIE));

                startAngle += sweep;
                i++;
            }

            // Center hole (donut effect)
            int holeR = r / 2;
            g2.setColor(CARD);
            g2.fillOval(cx - holeR, cy - holeR, holeR * 2, holeR * 2);

            // Total in center
            g2.setColor(WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            String totalStr = "₹" + String.format("%.0f", total);
            int tw = g2.getFontMetrics().stringWidth(totalStr);
            g2.drawString(totalStr, cx - tw/2, cy + 5);

            // Legend (right side)
            int lx = cx + r + 20;
            int ly = cy - (data.size() * 18) / 2;
            i = 0;
            for (Map.Entry<String, Double> entry : data.entrySet()) {
                Color col = CAT_COLORS[Arrays.asList(CATEGORIES).indexOf(entry.getKey()) % CAT_COLORS.length];
                g2.setColor(col);
                g2.fillRoundRect(lx, ly + i * 20, 12, 12, 4, 4);

                g2.setColor(TEXT);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                double pct = (entry.getValue() / total) * 100;
                g2.drawString(entry.getKey() + " " + String.format("%.1f%%", pct), lx + 16, ly + i * 20 + 11);
                i++;
            }
        }
    }

    // ───────────────────────────────────────────────────────────────
    //  UI HELPER METHODS
    // ───────────────────────────────────────────────────────────────

    private JPanel darkCard() {
        JPanel p = new JPanel();
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        return p;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(MUTED);
        return l;
    }

    private JTextField darkField() {
        JTextField f = new JTextField();
        f.setBackground(BG);
        f.setForeground(WHITE);
        f.setCaretColor(WHITE);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        return f;
    }

    private void styleCombo(JComboBox<?> c) {
        c.setBackground(BG);
        c.setForeground(WHITE);
        c.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    }

    private JButton actionBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setBackground(bg);
        b.setForeground(WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void clearForm() {
        amountField.setText("");
        descField.setText("");
        categoryCombo.setSelectedIndex(0);
        selectedId = -1;
        expenseTable.clearSelection();
    }

    private void setStatus(String s) { statusLabel.setText("  " + s); }
    private void showWarn(String s)  { JOptionPane.showMessageDialog(this, s, "Warning", JOptionPane.WARNING_MESSAGE); }
    private void showError(String s) { JOptionPane.showMessageDialog(this, s, "Error",   JOptionPane.ERROR_MESSAGE); }

    // ───────────────────────────────────────────────────────────────
    //  MAIN
    // ───────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(ExpenseAnalyzer::new);
    }
}
