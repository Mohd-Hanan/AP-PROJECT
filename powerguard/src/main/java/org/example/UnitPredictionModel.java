package org.example;

import weka.classifiers.Classifier;
import weka.core.*;

public class UnitPredictionModel {
    private Classifier model;
    private Instances datasetHeader;
    private boolean isLoaded = false;

    /**
     * Predicts a target value (for example, bill amount) from consumed units.
     */
    public double predictFromUnits(double unitsInput) throws Exception {
        if (!isLoaded || model == null) {
            throw new IllegalStateException("Unit Model not loaded.");
        }

        double[] values = new double[datasetHeader.numAttributes()];

        // Map the single input to the 'units' attribute index
        int unitsIdx = datasetHeader.attribute("units").index();
        values[unitsIdx] = unitsInput;

        // Set the class attribute (e.g., electricitybill) as missing for prediction
        values[datasetHeader.classIndex()] = Utils.missingValue();

        Instance instance = new DenseInstance(1.0, values);
        instance.setDataset(datasetHeader);

        return model.classifyInstance(instance);
    }

    public void loadModel(String path) throws Exception {
        Object[] payload = (Object[]) SerializationHelper.read(path);
        this.model = (Classifier) payload[0];
        this.datasetHeader = (Instances) payload[1];
        this.isLoaded = true;
    }
}
