package org.example;

import java.util.ArrayList;
import java.util.Random;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.LinearRegression;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ConverterUtils.DataSource;

public class LinearRegressionModel {
    private LinearRegression model;
    private Instances datasetHeader;
    private boolean trainStatus = false;

    public static class ModelEvaluationResult {
        public final double mae;
        public final double rmse;
        public final double r2;
        public final double accuracyPercent;
        public final int trainRows;
        public final int testRows;

        public ModelEvaluationResult(double mae, double rmse, double r2, int trainRows, int testRows) {
            this.mae = mae;
            this.rmse = rmse;
            this.r2 = r2;
            this.accuracyPercent = Math.max(0.0, Math.min(100.0, r2 * 100.0));
            this.trainRows = trainRows;
            this.testRows = testRows;
        }

        @Override
        public String toString() {
            return String.format("Testing complete -> Train: %d, Test: %d, MAE: %.5f, RMSE: %.5f, R²: %.5f (Accuracy: %.2f%%)",
                    trainRows, testRows, mae, rmse, r2, accuracyPercent);
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

        System.out.println("Training complete on merged electricity dataset. Rows: " + data.numInstances());
    }

    public ModelEvaluationResult trainAndTestModel(Instances dataset, double testRatio, long seed) throws Exception {
        if (dataset.classIndex() == -1) {
            dataset.setClassIndex(dataset.numAttributes() - 1);
        }

        Instances shuffled = new Instances(dataset);
        shuffled.randomize(new Random(seed));

        int testSize = Math.max(1, (int) Math.round(shuffled.numInstances() * testRatio));
        int trainSize = Math.max(1, shuffled.numInstances() - testSize);

        Instances trainData = new Instances(shuffled, 0, trainSize);
        Instances testData = new Instances(shuffled, trainSize, shuffled.numInstances() - trainSize);

        model = new LinearRegression();
        model.buildClassifier(trainData);

        Evaluation evaluation = new Evaluation(trainData);
        evaluation.evaluateModel(model, testData);

        this.datasetHeader = new Instances(trainData, 0);
        this.trainStatus = true;

        double correlation = evaluation.correlationCoefficient();
        double rSquared = correlation * correlation;

        return new ModelEvaluationResult(
                evaluation.meanAbsoluteError(),
                evaluation.rootMeanSquaredError(),
                rSquared,
                trainData.numInstances(),
                testData.numInstances()
        );
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

    public void saveModel(String path) throws Exception {
        Object[] payload = new Object[]{model, datasetHeader};
        SerializationHelper.write(path, payload);
    }

    public void loadModel(String path) throws Exception {
        Object saved = SerializationHelper.read(path);

        if (saved instanceof Object[]) {
            Object[] payload = (Object[]) saved;
            this.model = (LinearRegression) payload[0];
            if (payload.length > 1 && payload[1] instanceof Instances) {
                this.datasetHeader = (Instances) payload[1];
            }
        } else {
            this.model = (LinearRegression) saved;
        }

        this.trainStatus = true;
        if (this.datasetHeader == null) {
            initializeHeader();
        }
    }
}
