package org.example;

import javax.swing.*;
import java.io.File;

public class Main {

    public static void main(String[] args) {

        String arffPath = "src/main/resources/data/final_electricity_dataset.arff";
        String modelPath = "src/main/resources/data/power_model.model";
        String mergedCsvPath = "src/main/resources/data/final electricity dataset.csv";

        LinearRegressionModel predictor = new LinearRegressionModel();

        try {

            File modelFile = new File(modelPath);
            File sourceFile = new File(mergedCsvPath);

            if (!sourceFile.exists()) {
                throw new IllegalStateException("Dataset not found at: " + mergedCsvPath);
            }

            boolean retrainNeeded =
                    !modelFile.exists() ||
                    sourceFile.lastModified() > modelFile.lastModified();

            if (retrainNeeded) {

                System.out.println("Step 1/3 - Preprocessing CSV...");
                DataHandler.convertCSVtoARFF(mergedCsvPath, arffPath);

                System.out.println("Step 2/3 - Benchmarking models...");
                LinearRegressionModel.ModelEvaluationResult bestResult =
                        predictor.trainAndSelectBestModel(arffPath, 0.2, 42);

                System.out.println("Step 3/3 - Saving best model...");
                predictor.saveModel(modelPath);

                System.out.println("Best Model Selected: " + bestResult.modelName);
                System.out.println(bestResult);

                JOptionPane.showMessageDialog(
                        null,
                        "Training Complete!\n\nBest Model: "
                                + bestResult.modelName
                                + "\n"
                                + bestResult
                );

            } else {
                System.out.println("Loading existing trained model...");
                predictor.loadModel(modelPath);
            }

            SwingUtilities.invokeLater(() ->
                    new PowerGuardGUI(predictor).setVisible(true)
            );

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "Error: " + e.getMessage(),
                    "Startup Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}