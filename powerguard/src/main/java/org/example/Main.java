package org.example;

import javax.swing.*;
import java.io.File;
import weka.core.Instances;

public class Main {
    public static void main(String[] args) {
        String arffPath = "src/main/resources/data/final_electricity_dataset.arff";
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
                System.out.println("Step 1/3 - Data preprocessing...");
                Instances processedData = DataHandler.preprocessMergedCSV(mergedCsvPath, 10000, 42);

                System.out.println("Step 2/3 - Data training...");
                LinearRegressionModel.ModelEvaluationResult evaluation = predictor.trainAndTestModel(processedData, 0.2, 42);

                System.out.println("Step 3/3 - Data testing...");
                System.out.println(evaluation);

                DataHandler.convertCSVtoARFF(mergedCsvPath, arffPath);
                predictor.saveModel(modelPath);

                JOptionPane.showMessageDialog(null, "Preprocessing, training, and testing complete.\n" + evaluation);
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

            SwingUtilities.invokeLater(() -> new PowerGuardGUI(predictor).setVisible(true));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
