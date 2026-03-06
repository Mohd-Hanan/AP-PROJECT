package org.example;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.AdditiveRegression;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.trees.REPTree;
import weka.classifiers.trees.RandomForest;

import weka.core.*;
import weka.core.converters.ConverterUtils.DataSource;

public class ApplianceModel {

    private Classifier model;
    private Instances datasetHeader;
    private boolean trainStatus = false;
    private String modelName = "Unknown"; // Add this field
    private double lastR2 = 0.0;          // Add this field
    private double lastRMSE = 0.0;
    /* =======================
       Evaluation Result
    ======================== */
    public static class ModelEvaluationResult {

        public final String modelName;
        public final double mae;
        public final double rmse;
        public final double r2;
        public final int trainRows;
        public final int testRows;

        public ModelEvaluationResult(String modelName,
                                     double mae,
                                     double rmse,
                                     double r2,
                                     int trainRows,
                                     int testRows) {
            this.modelName = modelName;
            this.mae = mae;
            this.rmse = rmse;
            this.r2 = r2;
            this.trainRows = trainRows;
            this.testRows = testRows;
        }

        @Override
        public String toString() {
            return String.format(
                    "%s -> Train: %d, Test: %d | MAE: %.5f | RMSE: %.5f | R²: %.5f",
                    modelName, trainRows, testRows, mae, rmse, r2
            );
        }
    }

    /* =======================
       Train & Benchmark
    ======================== */
    public ModelEvaluationResult trainAndSelectBestModel(String datasetPath,
                                                         double testRatio,
                                                         long seed) throws Exception {

        DataSource source = new DataSource(datasetPath);
        Instances dataset = source.getDataSet();

        if (dataset.classIndex() == -1)
            dataset.setClassIndex(dataset.numAttributes() - 1);

        Instances shuffled = new Instances(dataset);
        shuffled.randomize(new Random(seed));

        int testSize = Math.max(1, (int) (shuffled.numInstances() * testRatio));
        int trainSize = shuffled.numInstances() - testSize;

        Instances trainData = new Instances(shuffled, 0, trainSize);
        Instances testData = new Instances(shuffled, trainSize, testSize);

        LinkedHashMap<String, Classifier> candidates = new LinkedHashMap<>();

        candidates.put("LinearRegression", new LinearRegression());
        candidates.put("RandomForest", new RandomForest());
        candidates.put("DecisionTree", new REPTree());

        AdditiveRegression gradientBoost = new AdditiveRegression();
        gradientBoost.setClassifier(new REPTree());
        candidates.put("GradientBoosting", gradientBoost);

        candidates.put("NeuralNetwork", new MultilayerPerceptron());

        ModelEvaluationResult bestResult = null;
        Classifier bestModel = null;

        for (Map.Entry<String, Classifier> entry : candidates.entrySet()) {

            String name = entry.getKey();
            Classifier candidate = entry.getValue();

            candidate.buildClassifier(trainData);

            Evaluation eval = new Evaluation(trainData);
            eval.evaluateModel(candidate, testData);

            double correlation = eval.correlationCoefficient();
            double r2 = Double.isNaN(correlation) ? 0 : correlation * correlation;

            ModelEvaluationResult result =
                    new ModelEvaluationResult(
                            name,
                            eval.meanAbsoluteError(),
                            eval.rootMeanSquaredError(),
                            r2,
                            trainData.numInstances(),
                            testData.numInstances()
                    );

            System.out.println(result);

            // RMSE-based selection (industry preferred)
            if (bestResult == null || result.rmse < bestResult.rmse) {
                bestResult = result;
                bestModel = candidate;
            }
        }

        if (bestModel == null)
            throw new IllegalStateException("No valid model selected.");

        this.model = bestModel;
        this.modelName = bestResult.modelName; // Capture the name
        this.lastR2 = bestResult.r2;           // Capture R2
        this.lastRMSE = bestResult.rmse;
        this.datasetHeader = new Instances(trainData, 0);
        this.trainStatus = true;

        System.out.println("Best model selected: " + bestResult.modelName);

        return bestResult;
    }
    public double predictUnits(double fan, double fridge, double ac, double tv, double monitor) throws Exception {
        if (!trainStatus || model == null) throw new IllegalStateException("Model not trained.");

        double[] values = new double[datasetHeader.numAttributes()];
        // Fill the attributes in the order they appear in the ARFF
        if(datasetHeader.attribute("fan") != null) values[datasetHeader.attribute("fan").index()] = fan;
        if(datasetHeader.attribute("refrigerator") != null) values[datasetHeader.attribute("refrigerator").index()] = fridge;
        if(datasetHeader.attribute("airconditioner") != null) values[datasetHeader.attribute("airconditioner").index()] = ac;
        if(datasetHeader.attribute("television") != null) values[datasetHeader.attribute("television").index()] = tv;
        if(datasetHeader.attribute("monitor") != null) values[datasetHeader.attribute("monitor").index()] = monitor;

        // Set target as missing
        values[datasetHeader.classIndex()] = weka.core.Utils.missingValue();

        Instance instance = new DenseInstance(1.0, values);
        instance.setDataset(datasetHeader);

        return model.classifyInstance(instance);
    }

    /* =======================
       Backward Compatibility Wrapper
    ======================= */
    public void trainModel(String datasetPath) throws Exception {
        trainAndSelectBestModel(datasetPath, 0.2, 42);
    }

    /* =======================
       Save / Load
    ======================== */
    public void saveModel(String path) throws Exception {
        // Save the model, header, name, R2, and RMSE
        Object[] payload = new Object[]{model, datasetHeader, modelName, lastR2, lastRMSE};
        SerializationHelper.write(path, payload);
    }

    public void loadModel(String path) throws Exception {
        Object[] payload = (Object[]) SerializationHelper.read(path);
        this.model = (Classifier) payload[0];
        this.datasetHeader = (Instances) payload[1];

        // Load the saved stats if they exist
        if (payload.length >= 5) {
            this.modelName = (String) payload[2];
            this.lastR2 = (Double) payload[3];
            this.lastRMSE = (Double) payload[4];
        }
        this.trainStatus = true;
    }
    public String getModelName() { return modelName; }
    public double getLastR2() { return lastR2; }
    public double getLastRMSE() { return lastRMSE; }
}
