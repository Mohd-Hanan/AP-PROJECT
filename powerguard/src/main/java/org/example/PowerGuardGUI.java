package org.example;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
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
    private final double UNIT_RATE = 8.0; // Your rate of 8.0 per unit
    private JTextField txtAppliance, txtRating, txtQuantity, txtHours;
    private JLabel lblResult;
    private ChartPanel chartPanel; // Fixed: Declared missing variable
    private DefaultCategoryDataset dataset;
    private JTextField txtBudget;
    private JPanel pnlStatus;


    public PowerGuardGUI(LinearRegressionModel predictor) {
        this.predictor = predictor;
        // Apply modern look
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            System.err.println("Failed to initialize FlatLaf");
        }
        // Inside the constructor
        // Inside PowerGuardGUI constructor
// KITCHEN & FOOD
        Map<String, Integer> samsung = new HashMap<>();
        samsung.put("Smart Refrigerator", 400);
        samsung.put("Microwave Oven", 1100);
        samsung.put("Dishwasher", 1200);

        Map<String, Integer> lg = new HashMap<>();
        lg.put("InstaView Fridge", 350);
        lg.put("Inverter AC (1.5 Ton)", 1450);
        lg.put("OLED TV (65 inch)", 150);

        Map<String, Integer> whirlpool = new HashMap<>();
        whirlpool.put("Triple Door Fridge", 300);
        whirlpool.put("Front Load Washer", 2100);
        whirlpool.put("Air Purifier", 50);

// HIGH POWER & EV
        Map<String, Integer> mobility = new HashMap<>();
        mobility.put("Tesla Wall Connector", 11500);
        mobility.put("Ather 450X Charger", 850);
        mobility.put("Ola S1 Pro Charger", 750);

// COMPUTING
        Map<String, Integer> apple = new HashMap<>();
        apple.put("MacBook Pro (M3)", 65);
        apple.put("Studio Display", 30);
        apple.put("iPad Pro Charger", 20);

// Update your main deviceLibrary
        deviceLibrary.put("Samsung", samsung);
        deviceLibrary.put("LG", lg);
        deviceLibrary.put("Whirlpool", whirlpool);
        deviceLibrary.put("Mobility (EV)", mobility);
        deviceLibrary.put("Apple (Computing)", apple);

        Map<String, Integer> samsungDevices = new HashMap<>();
        samsungDevices.put("Inverter AC (1.5 Ton)", 1500);
        samsungDevices.put("Smart Fridge", 300);

        Map<String, Integer> lgDevices = new HashMap<>();
        lgDevices.put("OLED TV", 150);
        lgDevices.put("Washing Machine", 2000);
        this.predictor = new LinearRegressionModel();
        try {
            this.predictor.trainModel("src/main/resources/data/household_power.arff");
        } catch (Exception e) {
            System.err.println("Model training failed: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Critical Error: ML Dataset not found or corrupted.");
        }
        deviceLibrary.put("Samsung", samsungDevices);
        deviceLibrary.put("LG", lgDevices);
        setTitle("PowerGuard Professional");
        // Inside the constructor
        Map<String, Integer> whirlpoolDevices = new HashMap<>();
        whirlpoolDevices.put("Top Load Washer", 500);
        whirlpoolDevices.put("Side-by-Side Fridge", 450);
        whirlpoolDevices.put("Microwave Oven", 1200);

        deviceLibrary.put("Whirlpool", whirlpoolDevices);

        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // 1. Setup Input Card
        txtAppliance = new JTextField(15);
        txtRating = new JTextField("1500", 15);
        txtQuantity = new JTextField("1", 15);
        txtHours = new JTextField("5.5", 15);
        lblResult = new JLabel("Estimated Cost: ₹0.00");
        lblResult.setFont(new Font("SansSerif", Font.BOLD, 16));

        // 2. Build the UI Sections
        add(createSidebar(), BorderLayout.WEST);
        add(createMainDashboard(), BorderLayout.CENTER);
        add(createInputCard(), BorderLayout.EAST);
    }

    private JPanel createSidebar() {
        // 1. Initialize the panel FIRST to avoid the NullPointerException
        JPanel sidebar = new JPanel();

        // 2. Set the appearance and size
        sidebar.setBackground(new Color(33, 37, 41));
        sidebar.setPreferredSize(new Dimension(150, 700));
        sidebar.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 20));

        // 3. Add Branding
        JLabel lblLogo = new JLabel("POWERGUARD");
        lblLogo.setForeground(Color.WHITE);
        sidebar.add(lblLogo); // Error happened here because sidebar was null

        // 4. Add the Reset Button
        JButton btnReset = new JButton("Reset Chart");
        btnReset.addActionListener(e -> {
            dataset.clear();
            JOptionPane.showMessageDialog(this, "Chart history cleared.");
        });
        sidebar.add(btnReset);

        // 5. Add the Export Button
        JButton btnExport = new JButton("Export Chart");
        btnExport.addActionListener(e -> saveChartImage());
        sidebar.add(btnExport);

        return sidebar;
    }
    private JPanel createMainDashboard() {
        JPanel dashboard = new JPanel(new BorderLayout());
        dashboard.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        setupChart(); // Fixed: Method now defined
        dashboard.add(new JLabel("Usage Analytics"), BorderLayout.NORTH);
        dashboard.add(chartPanel, BorderLayout.CENTER);

        return dashboard;
    }
    // Replace JTextFields with JComboBox
    private JComboBox<String> comboCompany;
    private JComboBox<String> comboDevice;
    private JLabel lblUnitCost; // To display price along with device
    private JComboBox<String> comboCategory;

    private JPanel createInputCard() {
        // 1. Initialize the JComboBoxes FIRST
        // Get the keys from your deviceLibrary (Samsung, LG, etc.)

        String[] companies = deviceLibrary.keySet().toArray(new String[0]);
        comboCompany = new JComboBox<>(companies);
        comboDevice = new JComboBox<>();

        // 2. Initialize other UI components
        txtQuantity = new JTextField("1", 15);
        txtHours = new JTextField("5.5", 15);
        txtBudget = new JTextField("500", 15);
        lblResult = new JLabel("Estimated Cost: ₹0.00");
        pnlStatus = new JPanel();
        pnlStatus.setBackground(Color.GREEN);

        // 3. Setup the Panel Layout
        JPanel card = new JPanel(new GridLayout(16, 1, 5, 5));
        card.setBorder(BorderFactory.createTitledBorder("Categorized Selection"));

        // 4. Add components to the card
        card.add(new JLabel("Select Company:"));
        card.add(comboCompany);
        card.add(new JLabel("Select Device:"));
        card.add(comboDevice);
        card.add(new JLabel("Quantity:"));
        card.add(txtQuantity);
        card.add(new JLabel("Hours/Day:"));
        card.add(txtHours);
        card.add(new JLabel("Monthly Budget (₹):"));
        card.add(txtBudget);
        card.add(new JLabel("Budget Status:"));
        card.add(pnlStatus);

        JButton btnPredict = new JButton("Predict Bill");
        btnPredict.addActionListener(e -> calculate());
        card.add(btnPredict);
        card.add(lblResult);

        // 5. Setup Listeners
        // Add the listener so comboDevice updates when comboCompany changes
        comboCompany.addActionListener(e -> updateDeviceList());

        // 6. NOW it is safe to call updateDeviceList because comboCompany is not null
        updateDeviceList();

        return card;
    }

    private void setupChart() { // Fixed: Method now defined
        dataset = new DefaultCategoryDataset();
        JFreeChart chart = ChartFactory.createBarChart("Consumption Trend", "Record", "Cost (₹)", dataset);
        chartPanel = new ChartPanel(chart);
    }
    private void updateDeviceList() {
        // 1. Clear the previous list to avoid mixing appliances
        comboDevice.removeAllItems();

        // 2. Get current selection
        String selectedCategory = (String) comboCompany.getSelectedItem();

        // 3. Safety Check: Only proceed if category exists in our library
        if (selectedCategory != null && deviceLibrary.containsKey(selectedCategory)) {
            Map<String, Integer> devices = deviceLibrary.get(selectedCategory);

            // 4. Populate the dropdown with device names
            for (String deviceName : devices.keySet()) {
                comboDevice.addItem(deviceName);
            }
        }
    }

    private void updateUnitCost() {
        String category = (String) comboCategory.getSelectedItem();
        String device = (String) comboDevice.getSelectedItem();
        if (device != null && deviceLibrary.containsKey(category)) {
            int watts = deviceLibrary.get(category).get(device);
            // Standard energy formula: (Watts / 1000) * Rate per Unit
            double hourlyCost = (watts / 1000.0) * UNIT_RATE;
            lblUnitCost.setText(String.format("Device Rate: ₹%.2f / hr", hourlyCost));
        }
    }
    private void calculate() {
        try {
            String category = (String) comboCompany.getSelectedItem();
            String device = (String) comboDevice.getSelectedItem();

            if (device == null || category == null) {
                JOptionPane.showMessageDialog(this, "Please select both a company and a device.");
                return;
            }

            // 1. Get accurate numeric inputs
            int rating = deviceLibrary.get(category).get(device);
            int quantity = Integer.parseInt(txtQuantity.getText());
            double hours = Double.parseDouble(txtHours.getText());
            double budgetLimit = Double.parseDouble(txtBudget.getText());

            // 2. SCALE THE INPUT FOR ML
            // Most regression models trained on UCI data expect hourly kW
            double hourlyKW = (rating / 1000.0) * quantity;

            // 3. PREDICT
            // Predict the consumption for that specific power level
            double predictedHourlyUnits = predictor.predict(hourlyKW);

            // 4. CALCULATE FINAL COST
            // Multiply predicted hourly rate by actual hours used and the money rate
            double totalUnits = predictedHourlyUnits * hours;
            double cost = totalUnits * UNIT_RATE;

            // 5. UPDATE UI
            lblResult.setText(String.format("ML Predicted Cost: ₹%.2f", cost));
            pnlStatus.setBackground(cost > budgetLimit ? Color.RED : Color.GREEN);

            // 6. UPDATE CHART
            // Using device name as the category key ensures unique bars
            String entryLabel = device + " (" + (dataset.getColumnCount() + 1) + ")";
            dataset.addValue(cost, "Cost", entryLabel);

            // 7. SAVE
            new DataHandler().saveRecord(device, cost);

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Input Error: Please enter valid numbers for Quantity, Hours, and Budget.");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "ML Error: " + e.getMessage());
        }
    }
    private void saveChartImage() {
        try {
            // 1. Create a file chooser to let the user pick where to save
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Chart as Image");

            int userSelection = fileChooser.showSaveDialog(this);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                // Ensure the file has a .png extension
                String filePath = fileToSave.getAbsolutePath();
                if (!filePath.toLowerCase().endsWith(".png")) {
                    fileToSave = new File(filePath + ".png");
                }

                // 2. Export the JFreeChart from your chartPanel
                ChartUtils.saveChartAsPNG(fileToSave, chartPanel.getChart(), 800, 600);

                JOptionPane.showMessageDialog(this, "Chart saved successfully to: " + fileToSave.getName());
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error saving chart: " + e.getMessage());
        }
    }
}