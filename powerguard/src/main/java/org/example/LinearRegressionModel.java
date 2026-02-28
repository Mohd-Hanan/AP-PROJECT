package org.example;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.AdditiveRegression;
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

    public static class ModelEvaluationResult {
        public final String modelName;
        public final double mae;
        public final double rmse;
        public final double r2;
        public final double accuracyPercent;
        public final int trainRows;
        public final int testRows;

        public ModelEvaluationResult(String modelName, double mae, double rmse, double r2, int trainRows, int testRows) {
            this.modelName = modelName;
            this.mae = mae;
            this.rmse = rmse;
            this.r2 = r2;
            this.accuracyPercent = Math.max(0.0, Math.min(100.0, r2 * 100.0));
            this.trainRows = trainRows;
            this.testRows = testRows;
        }

        @Override
        public String toString() {
            return String.format("%s -> Train: %d, Test: %d, MAE: %.5f, RMSE: %.5f, R²: %.5f (Accuracy: %.2f%%)",
                    modelName, trainRows, testRows, mae, rmse, r2, accuracyPercent);
        }
    }

    public static class ModelBenchmarkResult {
        public final ModelEvaluationResult bestResult;
        public final Map<String, ModelEvaluationResult> allResults;

        public ModelBenchmarkResult(ModelEvaluationResult bestResult, Map<String, ModelEvaluationResult> allResults) {
            this.bestResult = bestResult;
            this.allResults = allResults;
        }
    }

    public void initializeHeader() {
        if (this.datasetHeader != null) {
            return;
        }

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
        attributes.add(new Attribute("units"));

        this.datasetHeader = new Instances("PowerPredictionStructure", attributes, 0);
        this.datasetHeader.setClassIndex(this.datasetHeader.numAttributes() - 1);
    }

    public void trainModel(String datasetPath) throws Exception {
        DataSource source = new DataSource(datasetPath);
        Instances data = source.getDataSet();

        if (data.classIndex() == -1) {
            data.setClassIndex(data.numAttributes() - 1);
        }

        model = new LinearRegression();
        model.buildClassifier(data);

        this.datasetHeader = new Instances(data, 0);
        this.trainStatus = true;

        System.out.println("Training complete (LinearRegression). Rows: " + data.numInstances());
    }

    public ModelBenchmarkResult trainAndSelectBestModel(Instances dataset, double testRatio, long seed) throws Exception {
        if (dataset == null || dataset.numInstances() < 2) {
            throw new IllegalArgumentException("Need at least 2 rows to train/test models.");
        }

        if (dataset.classIndex() == -1) {
            dataset.setClassIndex(dataset.numAttributes() - 1);
        }

        Instances shuffled = new Instances(dataset);
        shuffled.randomize(new Random(seed));

        int testSize = Math.max(1, (int) Math.round(shuffled.numInstances() * testRatio));
        if (testSize >= shuffled.numInstances()) {
            testSize = shuffled.numInstances() - 1;
        }
        int trainSize = shuffled.numInstances() - testSize;

        Instances trainData = new Instances(shuffled, 0, trainSize);
        Instances testData = new Instances(shuffled, trainSize, testSize);

        LinkedHashMap<String, Classifier> candidates = new LinkedHashMap<>();
        candidates.put("LinearRegression", new LinearRegression());
        candidates.put("RandomForest", new RandomForest());
        candidates.put("DecisionTree", new REPTree());

        AdditiveRegression gradientBoosting = new AdditiveRegression();
        gradientBoosting.setClassifier(new REPTree());
        candidates.put("GradientBoosting", gradientBoosting);
        candidates.put("MultilayerPerceptron", new MultilayerPerceptron());

        Map<String, ModelEvaluationResult> allResults = new LinkedHashMap<>();
        ModelEvaluationResult bestResult = null;
        Classifier bestModel = null;

        for (Map.Entry<String, Classifier> entry : candidates.entrySet()) {
            String modelName = entry.getKey();
            Classifier candidate = entry.getValue();

            candidate.buildClassifier(trainData);

            Evaluation evaluation = new Evaluation(trainData);
            evaluation.evaluateModel(candidate, testData);

            double correlation = evaluation.correlationCoefficient();
            if (Double.isNaN(correlation)) {
                correlation = 0.0;
            }
            double rSquared = correlation * correlation;

            ModelEvaluationResult result = new ModelEvaluationResult(
                    modelName,
                    evaluation.meanAbsoluteError(),
                    evaluation.rootMeanSquaredError(),
                    rSquared,
                    trainData.numInstances(),
                    testData.numInstances()
            );

            allResults.put(modelName, result);

            if (bestResult == null || result.rmse < bestResult.rmse) {
                bestResult = result;
                bestModel = candidate;
            }
        }

        if (bestModel == null || bestResult == null) {
            throw new IllegalStateException("Model benchmarking produced no valid model.");
        }

        this.model = bestModel;
        this.datasetHeader = new Instances(trainData, 0);
        this.trainStatus = true;

        return new ModelBenchmarkResult(bestResult, allResults);
    }

    public double predict(double rawKWh) throws Exception {
        if (!trainStatus || model == null) {
            throw new IllegalStateException("Model not initialized. Train or load model first.");
        }
        if (datasetHeader == null) {
            throw new IllegalStateException("Dataset header not initialized. Train or load model with header first.");
        }

        double[] features = new double[datasetHeader.numAttributes()];
        features[0] = rawKWh;

        Instance instance = new DenseInstance(1.0, features);
        instance.setDataset(datasetHeader);

        return model.classifyInstance(instance);
    }


    public double predictUnits(double rawKWh) throws Exception {
        return predict(rawKWh);
    }

    public double predictElectricityBill(double rawKWh, double hours, double unitRate) throws Exception {
        double predictedHourlyUnits = predictUnits(rawKWh);
        double totalUnits = predictedHourlyUnits * hours;
        return totalUnits * unitRate;
    }

    public void saveModel(String path) throws Exception {
        Object[] payload = new Object[]{model, datasetHeader};
        SerializationHelper.write(path, payload);
    }

    public void loadModel(String path) throws Exception {
        Object saved = SerializationHelper.read(path);

        if (saved instanceof Object[]) {
            Object[] payload = (Object[]) saved;
            if (payload.length > 0 && payload[0] instanceof Classifier) {
                this.model = (Classifier) payload[0];
            }
            if (payload.length > 1 && payload[1] instanceof Instances) {
                this.datasetHeader = (Instances) payload[1];
            }
        } else if (saved instanceof Classifier) {
            this.model = (Classifier) saved;
        } else {
            throw new IllegalStateException("Unsupported model payload type: " + saved.getClass().getName());
        }

        this.trainStatus = true;
        if (this.datasetHeader == null) {
            initializeHeader();
        }
    }
}
