package org.example;

import weka.classifiers.functions.LinearRegression;
import weka.core.Instances;
import weka.core.Instance;
import weka.core.DenseInstance;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.SerializationHelper;

public class LinearRegressionModel {
    private LinearRegression model;
    private Instances datasetHeader;
    private boolean trainStatus = false;
    public void saveModel(String path) throws Exception {
        SerializationHelper.write(path, model);
        System.out.println("Model saved to: " + path);
    }

    public void loadModel(String path) throws Exception {
        this.model = (LinearRegression) SerializationHelper.read(path);
        this.trainStatus = true;
        System.out.println("Model loaded successfully!");
    }
    public void trainModel(String datasetPath) throws Exception {
        DataSource source = new DataSource(datasetPath);
        Instances data = source.getDataSet();

        // NEW: Remove Date and Time (Attributes 0 and 1) to speed up training by 100x
        data.deleteAttributeAt(0); // Removes Date
        data.deleteAttributeAt(0); // Removes Time (now at index 0)

        if (data.classIndex() == -1) {
            data.setClassIndex(data.numAttributes() - 1);
        }

        this.datasetHeader = new Instances(data, 0);
        model = new LinearRegression();

        System.out.println("Starting mathematical training on simplified data...");
        model.buildClassifier(data);
        this.trainStatus = true;
        System.out.println("Model trained successfully.");
    }

    /**
     * Updated to handle a single kWh input by wrapping it into the
     * required feature array format for the UCI dataset structure.
     */
    public double predict(double rawKWh) throws Exception {
        if (!trainStatus) {
            throw new IllegalStateException("Model must be trained before prediction.");
        }

        // Now the feature array is smaller because we deleted Date and Time
        double[] features = new double[datasetHeader.numAttributes()];
        features[0] = rawKWh;

        Instance instance = new DenseInstance(1.0, features);
        instance.setDataset(datasetHeader);
        return model.classifyInstance(instance);
    }
}