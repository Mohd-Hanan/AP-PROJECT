package org.example;

import javax.swing.*;
import java.io.File;

public class Main {
    public static void main(String[] args) {
        String arffPath = "src/main/resources/data/household_power.arff";
        String modelPath = "src/main/resources/data/power_model.model";
        String rawTxt = "src/main/resources/data/household_power_consumption.txt";

        LinearRegressionModel predictor = new LinearRegressionModel();

        try {
            File modelFile = new File(modelPath);

            if (!modelFile.exists()) {
                System.out.println("First-time setup: Converting data...");
                // This is where you use the method IntelliJ says has "no usage"
                DataHandler.convertCSVtoARFF(rawTxt, arffPath);

                System.out.println("Training model... Please wait.");
                predictor.trainModel(arffPath);
                predictor.saveModel(modelPath); // Save it so we never do this again

                JOptionPane.showMessageDialog(null, "Setup Complete!");
            } else {
                System.out.println("Loading pre-trained model...");
                predictor.loadModel(modelPath); // Instant loading
            }

            SwingUtilities.invokeLater(() -> new PowerGuardGUI(predictor).setVisible(true));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}