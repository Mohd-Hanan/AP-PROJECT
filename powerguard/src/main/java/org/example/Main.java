package org.example;

import javax.swing.*;
import java.io.File;

public class Main {

    public static void main(String[] args) {

        String arffPath = "target/final_electricity_dataset.arff";
        String modelPath = "target/power_model.model";
        File targetDir = new File("target");
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        String csvPath = Main.class
                .getClassLoader()
                .getResource("data/final_electricity_dataset.csv")
                .getPath();

        LinearRegressionModel predictor = new LinearRegressionModel();

        try {

            File modelFile = new File(modelPath);
            File sourceFile = new File(csvPath);

            if (!sourceFile.exists()) {
                throw new IllegalStateException("Dataset not found at: " + csvPath);
            }

            LinearRegressionModel.ModelEvaluationResult bestResult;

            boolean retrainNeeded =
                    !modelFile.exists() ||
                            sourceFile.lastModified() > modelFile.lastModified();

            if (retrainNeeded) {

                System.out.println("Step 1/3 - Preprocessing CSV...");
                DataHandler.convertCSVtoARFF(csvPath, arffPath);

                System.out.println("Step 2/3 - Benchmarking models...");
                bestResult = predictor.trainAndSelectBestModel(arffPath, 0.2, 42);

                System.out.println("Step 3/3 - Saving best model...");
                predictor.saveModel(modelPath);

                System.out.println("======================================");
                System.out.println("Best Model Selected: " + bestResult.modelName);
                System.out.println(bestResult);
                System.out.println("======================================");
            } else {

                System.out.println("Loading existing trained model...");
                predictor.loadModel(modelPath);
                System.out.println("Model loaded successfully.");

                bestResult = new LinearRegressionModel.ModelEvaluationResult(
                        "Previously Trained Model",
                        0,
                        0,
                        -1,     // use -1 to indicate unknown
                        0,
                        0
                );
            }

            LinearRegressionModel.ModelEvaluationResult finalResult = bestResult;

            SwingUtilities.invokeLater(() ->
                    new PowerGuardGUI(
                            predictor,
                            finalResult.modelName,
                            finalResult.r2,
                            finalResult.rmse
                    ).setVisible(true)
            );

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "Startup Error: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}