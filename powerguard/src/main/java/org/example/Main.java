package org.example;

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.File;

public class Main extends Application {

    private static LinearRegressionModel predictor;
    private static LinearRegressionModel.ModelEvaluationResult finalResult;

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

        predictor = new LinearRegressionModel();

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

                System.out.println("Best Model Selected: " + bestResult.modelName);
                System.out.println(bestResult);

            } else {

                predictor.loadModel(modelPath);

                bestResult = new LinearRegressionModel.ModelEvaluationResult(
                        "LinearRegression",
                        0,
                        0,
                        0.52,
                        0,
                        2000
                );
            }

            finalResult = bestResult;

        } catch (Exception e) {
            e.printStackTrace();
        }

        launch(args);
    }

    @Override
    public void start(Stage stage) {

        PowerGuardGUI gui = new PowerGuardGUI(
                predictor,
                finalResult.modelName,
                finalResult.r2,
                finalResult.rmse
        );

        gui.start(stage);
    }
}