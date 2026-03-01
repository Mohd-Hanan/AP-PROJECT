package org.example;

import java.util.ArrayList;
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

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ConverterUtils.DataSource;

public class LinearRegressionModel {

    private Classifier model;
    private Instances datasetHeader;
    private boolean trainStatus = false;

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
       Initialize Header
    ======================== */
    public void initializeHeader() {

        if (this.datasetHeader != null)
            return;

        ArrayList<Attribute> attributes = new ArrayList<>();

        attributes.add(new Attribute("fan"));
        attributes.add(new Attribute("refrigerator"));
        attributes.add(new Attribute("airconditioner"));
        attributes.add(new Attribute("television"));
        attributes.add(new Attribute("monitor"));
        attributes.add(new Attribute("monthlyhours"));
        attributes.add(new Attribute("tariffrate"));
        attributes.add(new Attribute("forecastwindproduction"));
        attributes.add(new Attribute("systemloadea"));
        attributes.add(new Attribute("smpea"));
        attributes.add(new Attribute("co2intensity"));
        attributes.add(new Attribute("actualwindproduction"));
        attributes.add(new Attribute("num_rooms"));
        attributes.add(new Attribute("num_people"));
        attributes.add(new Attribute("housearea"));
        attributes.add(new Attribute("is_ac"));
        attributes.add(new Attribute("is_tv"));
        attributes.add(new Attribute("is_flat"));
        attributes.add(new Attribute("ave_monthly_income"));
        attributes.add(new Attribute("num_children"));
        attributes.add(new Attribute("is_urban"));

        attributes.add(new Attribute("units")); // target

        datasetHeader = new Instances("PowerPredictionStructure", attributes, 0);
        datasetHeader.setClassIndex(datasetHeader.numAttributes() - 1);
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
        this.datasetHeader = new Instances(trainData, 0);
        this.trainStatus = true;

        System.out.println("Best model selected: " + bestResult.modelName);

        return bestResult;
    }

    /* =======================
       Predict
    ======================== */
    public double predict(double rawFeatureValue) throws Exception {

        if (!trainStatus || model == null)
            throw new IllegalStateException("Model not trained.");

        double[] values = new double[datasetHeader.numAttributes()];
        values[0] = rawFeatureValue;

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
        Object[] payload = new Object[]{model, datasetHeader};
        SerializationHelper.write(path, payload);
    }

    public void loadModel(String path) throws Exception {

        Object[] payload = (Object[]) SerializationHelper.read(path);

        this.model = (Classifier) payload[0];

        if (payload.length > 1 && payload[1] instanceof Instances)
            this.datasetHeader = (Instances) payload[1];

        this.trainStatus = true;
    }
}