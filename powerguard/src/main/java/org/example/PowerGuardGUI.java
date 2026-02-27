package org.example;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.ChartUtils;
import java.io.File;

public class PowerGuardGUI extends JFrame {
    private LinearRegressionModel predictor;
    private final Map<String, Map<String, Integer>> deviceLibrary = new HashMap<>();
    private final double UNIT_RATE = 8.0;

    private JTextField txtQuantity, txtHours, txtBudget, txtSearch;
    private JLabel lblResult, lblCarbon;
    private ChartPanel chartPanel;
    private DefaultCategoryDataset dataset;
    private JPanel pnlStatus;
    private JComboBox<String> comboCompany, comboDevice;
    private DefaultTableModel tableModel;
    private JTable historyTable;

    // 2026 Modern Color Palette
    private final Color COLOR_BG = new Color(18, 18, 18);
    private final Color COLOR_CARD = new Color(30, 30, 35);
    private final Color COLOR_ACCENT = new Color(0, 120, 255);
    private final Color COLOR_SUCCESS = new Color(0, 230, 118);
    private final Color COLOR_DANGER = new Color(255, 82, 82);

    public PowerGuardGUI(LinearRegressionModel predictor) {
        this.predictor = predictor;
        try { this.predictor.initializeHeader(); } catch (Exception e) {}
        try { UIManager.setLookAndFeel(new FlatDarkLaf()); } catch (Exception e) {}

        initializeData();

        setTitle("PowerGuard Professional | AI Energy Analytics");
        setSize(1300, 850);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(COLOR_BG);
        setLayout(new BorderLayout(15, 15));

        setupChart();
        add(createSidebar(), BorderLayout.WEST);
        add(createMainDashboard(), BorderLayout.CENTER);
        add(createInputCard(), BorderLayout.EAST);
    }

    private void initializeData() {
        // [Data logic remains same as provided in previous turn]
        Map<String, Integer> samsung = new HashMap<>();
        samsung.put("Smart Refrigerator", 400);
        samsung.put("Inverter AC (1.5 Ton)", 1500);

        Map<String, Integer> sony = new HashMap<>();
        sony.put("PlayStation 5", 200);
        sony.put("Bravia 4K TV", 180);

        Map<String, Integer> tesla = new HashMap<>();
        tesla.put("Wall Connector (11.5kW)", 11500);

        deviceLibrary.put("Samsung", samsung);
        deviceLibrary.put("Sony", sony);
        deviceLibrary.put("Tesla (EV)", tesla);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 25));
        sidebar.setBackground(new Color(25, 25, 30));
        sidebar.setPreferredSize(new Dimension(180, 700));

        JLabel lblLogo = new JLabel("POWERGUARD");
        lblLogo.setForeground(COLOR_ACCENT);
        lblLogo.setFont(new Font("Inter", Font.BOLD, 20));
        sidebar.add(lblLogo);

        sidebar.add(new JSeparator(SwingConstants.HORIZONTAL));

        JButton btnReset = createModernButton("RESET ALL", new Color(60, 60, 65));
        btnReset.addActionListener(e -> {
            dataset.clear();
            tableModel.setRowCount(0);
        });
        sidebar.add(btnReset);

        JButton btnExport = createModernButton("EXPORT PDF", COLOR_ACCENT);
        btnExport.addActionListener(e -> saveChartImage());
        sidebar.add(btnExport);

        return sidebar;
    }

    private JPanel createMainDashboard() {
        JPanel dashboard = new JPanel(new BorderLayout(20, 20));
        dashboard.setBackground(COLOR_BG);
        dashboard.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Glassmorphism Analytics Header
        JLabel lblTitle = new JLabel("USAGE ANALYTICS ENGINE");
        lblTitle.setForeground(Color.GRAY);
        lblTitle.setFont(new Font("Inter", Font.BOLD, 12));
        dashboard.add(lblTitle, BorderLayout.NORTH);

        dashboard.add(chartPanel, BorderLayout.CENTER);

        // Modernized Table
        String[] columns = {"DEVICE", "COST (₹)", "CARBON (KG)", "STATUS"};
        tableModel = new DefaultTableModel(columns, 0);
        historyTable = new JTable(tableModel);
        historyTable.setBackground(new Color(25, 25, 30));
        historyTable.setRowHeight(40);
        historyTable.setGridColor(new Color(45, 45, 50));
        historyTable.setShowVerticalLines(false);

        JScrollPane scrollPane = new JScrollPane(historyTable);
        scrollPane.getViewport().setBackground(new Color(25, 25, 30));
        scrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(50, 50, 55)), "LOG HISTORY"));
        scrollPane.setPreferredSize(new Dimension(600, 250));
        dashboard.add(scrollPane, BorderLayout.SOUTH);

        return dashboard;
    }

    private JPanel createInputCard() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(COLOR_CARD);
        card.setPreferredSize(new Dimension(320, 800));
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.gridx = 0;

        // Search Section
        txtSearch = new JTextField("Search...");
        txtSearch.setBackground(new Color(45, 45, 50));
        txtSearch.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        gbc.gridy = 0; card.add(new JLabel("QUICK SEARCH"), gbc);
        gbc.gridy = 1; card.add(txtSearch, gbc);

        JButton btnSearch = createModernButton("FIND APPLIANCE", COLOR_ACCENT);
        gbc.gridy = 2; card.add(btnSearch, gbc);

        // Inputs
        gbc.gridy = 3; card.add(new JSeparator(), gbc);
        comboCompany = new JComboBox<>(deviceLibrary.keySet().toArray(new String[0]));
        comboDevice = new JComboBox<>();
        gbc.gridy = 4; card.add(new JLabel("MANUFACTURER"), gbc);
        gbc.gridy = 5; card.add(comboCompany, gbc);
        gbc.gridy = 6; card.add(new JLabel("MODEL"), gbc);
        gbc.gridy = 7; card.add(comboDevice, gbc);

        txtQuantity = new JTextField("1");
        txtHours = new JTextField("5.5");
        txtBudget = new JTextField("500");
        gbc.gridy = 8; card.add(new JLabel("QUANTITY"), gbc);
        gbc.gridy = 9; card.add(txtQuantity, gbc);
        gbc.gridy = 10; card.add(new JLabel("DAILY HOURS"), gbc);
        gbc.gridy = 11; card.add(txtHours, gbc);
        gbc.gridy = 12; card.add(new JLabel("LIMIT (₹)"), gbc);
        gbc.gridy = 13; card.add(txtBudget, gbc);

        // Dynamic Status Card
        pnlStatus = new JPanel();
        pnlStatus.setPreferredSize(new Dimension(280, 5));
        pnlStatus.setBackground(COLOR_SUCCESS);
        gbc.gridy = 14; card.add(new JLabel("BUDGET CAP"), gbc);
        gbc.gridy = 15; card.add(pnlStatus, gbc);

        JButton btnPredict = createModernButton("PREDICT BILL", COLOR_ACCENT);
        btnPredict.addActionListener(e -> calculate());
        gbc.gridy = 16; card.add(btnPredict, gbc);

        // Results Card
        JPanel resPanel = new JPanel(new GridLayout(2, 1));
        resPanel.setBackground(new Color(40, 40, 45));
        lblResult = new JLabel("₹0.00", SwingConstants.CENTER);
        lblResult.setFont(new Font("Inter", Font.BOLD, 28));
        lblResult.setForeground(COLOR_SUCCESS);
        lblCarbon = new JLabel("0.00 kg CO2", SwingConstants.CENTER);
        lblCarbon.setForeground(Color.GRAY);
        resPanel.add(lblResult);
        resPanel.add(lblCarbon);
        gbc.gridy = 17; card.add(resPanel, gbc);

        comboCompany.addActionListener(e -> updateDeviceList());
        btnSearch.addActionListener(e -> filterDevices(txtSearch.getText().trim()));
        updateDeviceList();

        return card;
    }

    private JButton createModernButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        btn.setFont(new Font("Inter", Font.BOLD, 12));
        return btn;
    }

    private void setupChart() {
        dataset = new DefaultCategoryDataset();
        JFreeChart chart = ChartFactory.createBarChart(null, null, "Cost (₹)", dataset);
        chart.setBackgroundPaint(COLOR_BG);

        org.jfree.chart.plot.CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(COLOR_BG);
        plot.setOutlineVisible(false); // Borderless look
        plot.setRangeGridlinePaint(new Color(45, 45, 50));

        chartPanel = new ChartPanel(chart);
        chartPanel.setBackground(COLOR_BG);
    }

    private void filterDevices(String query) {
        // [Existing filter logic remains same]
        if (query.isEmpty() || query.equals("Search...")) { updateDeviceList(); return; }
        String lowerQuery = query.toLowerCase().trim();
        comboDevice.removeAllItems();
        boolean found = false;
        for (String company : deviceLibrary.keySet()) {
            boolean brandMatch = company.toLowerCase().contains(lowerQuery);
            Map<String, Integer> devices = deviceLibrary.get(company);
            for (String deviceName : devices.keySet()) {
                if (brandMatch || deviceName.toLowerCase().contains(lowerQuery)) {
                    if (!found) { comboCompany.setSelectedItem(company); found = true; }
                    comboDevice.addItem(deviceName);
                }
            }
        }
    }

    private void updateDeviceList() {
        comboDevice.removeAllItems();
        String selected = (String) comboCompany.getSelectedItem();
        if (selected != null) { deviceLibrary.get(selected).keySet().forEach(comboDevice::addItem); }
    }

    private void calculate() {
        try {
            String company = (String) comboCompany.getSelectedItem();
            String device = (String) comboDevice.getSelectedItem();
            int rating = deviceLibrary.get(company).get(device);
            double hours = Double.parseDouble(txtHours.getText());
            double budgetLimit = Double.parseDouble(txtBudget.getText());

            double hourlyKW = (rating / 1000.0) * Integer.parseInt(txtQuantity.getText());
            double predictedHourlyUnits = predictor.predict(hourlyKW);
            double totalUnits = predictedHourlyUnits * hours;
            double cost = totalUnits * UNIT_RATE;
            double carbon = totalUnits * 0.85;

            lblResult.setText(String.format("₹%.2f", cost));
            lblCarbon.setText(String.format("%.2f kg CO2", carbon));

            boolean over = cost > budgetLimit;
            pnlStatus.setBackground(over ? COLOR_DANGER : COLOR_SUCCESS);
            lblResult.setForeground(over ? COLOR_DANGER : COLOR_SUCCESS);

            dataset.addValue(cost, "Cost", device + " (" + (dataset.getColumnCount() + 1) + ")");
            tableModel.addRow(new Object[]{device, String.format("₹%.2f", cost), String.format("%.2f kg", carbon), over ? "OVER" : "SAFE"});

            org.jfree.chart.plot.CategoryPlot plot = chartPanel.getChart().getCategoryPlot();
            org.jfree.chart.renderer.category.BarRenderer renderer = (org.jfree.chart.renderer.category.BarRenderer) plot.getRenderer();
            renderer.setSeriesPaint(0, over ? COLOR_DANGER : COLOR_ACCENT);

        } catch (Exception e) { JOptionPane.showMessageDialog(this, "System Error: " + e.getMessage()); }
    }

    private void saveChartImage() {
        try {
            JFileChooser fc = new JFileChooser();
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                ChartUtils.saveChartAsPNG(fc.getSelectedFile(), chartPanel.getChart(), 800, 600);
            }
        } catch (Exception e) {}
    }
}
