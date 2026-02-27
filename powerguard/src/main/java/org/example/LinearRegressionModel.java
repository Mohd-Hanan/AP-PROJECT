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
        ArrayList<Attribute> attributes = new ArrayList<>();

        // The UCI dataset model expects 7 attributes after your training cleaning
        // We add dummy attributes to match the 'Dest' (7) required by the model
        attributes.add(new Attribute("Global_active_power")); // This is your rawKWh
        attributes.add(new Attribute("Global_reactive_power"));
        attributes.add(new Attribute("Voltage"));
        attributes.add(new Attribute("Global_intensity"));
        attributes.add(new Attribute("Sub_metering_1"));
        attributes.add(new Attribute("Sub_metering_2"));
        attributes.add(new Attribute("target_cost")); // The class index (7th attribute)

        this.datasetHeader = new Instances("PowerPredictionStructure", attributes, 0);
        this.datasetHeader.setClassIndex(this.datasetHeader.numAttributes() - 1);
    }

    public void trainModel(String datasetPath) throws Exception {
        DataSource source = new DataSource(datasetPath);
        Instances data = source.getDataSet();

        // Data cleaning: Remove Date and Time to focus on power usage
        data.deleteAttributeAt(0); // Removes Date
        data.deleteAttributeAt(0); // Removes Time

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
