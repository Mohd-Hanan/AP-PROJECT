package org.example;

import weka.classifiers.functions.LinearRegression;
import weka.core.Instances;
import weka.core.Instance;
import weka.core.DenseInstance;
import weka.core.Attribute;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.SerializationHelper;
import java.util.ArrayList;

public class LinearRegressionModel {
    private LinearRegression model;
    private Instances datasetHeader;
    private boolean trainStatus = false;

    /**
     * Initializes the dataset structure to prevent NullPointerExceptions.
     * This method MUST be called in the GUI constructor.
     */
    public void initializeHeader() {
        if (this.datasetHeader != null) return;

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
        attributes.add(new Attribute("electricitybill"));
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

        // Capture the header from the actual training data
        this.datasetHeader = new Instances(data, 0);
        model = new LinearRegression();

        System.out.println("Training model on UCI power data...");
        model.buildClassifier(data);
        this.trainStatus = true;
    }

    public double predict(double rawKWh) throws Exception {
        if (!trainStatus && datasetHeader == null) {
            throw new IllegalStateException("Model not initialized. Call initializeHeader() or trainModel().");
        }

        // Create a single-instance container for the prediction
        double[] features = new double[datasetHeader.numAttributes()];
        features[0] = rawKWh;

        Instance instance = new DenseInstance(1.0, features);
        instance.setDataset(datasetHeader); // Uses the initialized header

        return model.classifyInstance(instance);
    }

    public void saveModel(String path) throws Exception {
        SerializationHelper.write(path, model);
    }

    public void loadModel(String path) throws Exception {
        this.model = (LinearRegression) SerializationHelper.read(path);
        this.trainStatus = true;
        // Ensure header is still valid after loading a saved model
        if(this.datasetHeader == null) initializeHeader();
    }
}
