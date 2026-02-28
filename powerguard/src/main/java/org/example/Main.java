package org.example;

import javax.swing.*;
import java.io.File;

public class Main {
    public static void main(String[] args) {
        String arffPath = "src/main/resources/data/household_power.arff";
        String modelPath = "src/main/resources/data/power_model.model";
        String mergedCsvPath = "src/main/resources/data/final electricity dataset.csv";

        LinearRegressionModel predictor = new LinearRegressionModel();

        try {
            File modelFile = new File(modelPath);
            File sourceFile = new File(mergedCsvPath);

            if (!sourceFile.exists()) {
                throw new IllegalStateException("Merged dataset not found at: " + mergedCsvPath);
            }

            boolean retrainNeeded = !modelFile.exists() || sourceFile.lastModified() > modelFile.lastModified();

            if (retrainNeeded) {
                System.out.println("Preparing merged dataset for training...");
                DataHandler.convertCSVtoARFF(mergedCsvPath, arffPath);

                System.out.println("Training model... Please wait.");
                predictor.trainModel(arffPath);
                predictor.saveModel(modelPath);

                JOptionPane.showMessageDialog(null, "Model updated from merged dataset!");
            } else {
                System.out.println("Loading pre-trained model...");
                predictor.loadModel(modelPath);
            }

            SwingUtilities.invokeLater(() -> new PowerGuardGUI(predictor).setVisible(true));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
