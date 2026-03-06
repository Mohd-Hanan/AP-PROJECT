package org.example;

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.File;

public class Main extends Application {

    private static ApplianceModel predictor;
    private static ApplianceModel.ModelEvaluationResult finalResult;

    public static void main(String[] args) {
        System.err.close();
        System.setErr(System.out);

        java.util.logging.Logger.getLogger("com.github.fommil").setLevel(java.util.logging.Level.OFF);
        java.util.logging.LogManager.getLogManager().reset();

        String arffPath = "target/appliance_dataset.arff";
        String modelPath = "target/appliance_model.model";

        String csvPath = Main.class
                .getClassLoader()
                .getResource("data/final_electricity_dataset.csv")
                .getPath();

        predictor = new ApplianceModel();

        try {

            File modelFile = new File(modelPath);
            File sourceFile = new File(csvPath);

            if (!sourceFile.exists()) {
                throw new IllegalStateException("Dataset not found at: " + csvPath);
            }

            ApplianceModel.ModelEvaluationResult bestResult;

            boolean retrainNeeded =
                    !modelFile.exists() ||
                            sourceFile.lastModified() > modelFile.lastModified();

            if (retrainNeeded) {

                System.out.println("Step 1/3 - Preprocessing CSV...");
                DataHandler.convertCSVtoApplianceARFF(csvPath, arffPath);

                System.out.println("Step 2/3 - Benchmarking models...");
                bestResult = predictor.trainAndSelectBestModel(arffPath, 0.2, 42);

                System.out.println("Step 3/3 - Saving best model...");
                predictor.saveModel(modelPath);

                System.out.println("Best Model Selected: " + bestResult.modelName);
                System.out.println(bestResult);

            } else {
                System.out.println("Loading existing trained model...");
                predictor.loadModel(modelPath);

                // FIX: Instead of hardcoding, get the stats from the loaded model
                bestResult = new ApplianceModel.ModelEvaluationResult(
                        predictor.getModelName(),
                        0,
                        predictor.getLastRMSE(),
                        predictor.getLastR2(),
                        0,
                        0
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