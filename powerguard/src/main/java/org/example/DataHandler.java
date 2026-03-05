package org.example;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

public class DataHandler {

    private static final String FILE_PATH = "src/main/resources/data/history.csv";

    /* ============================
       Read Saved Appliance Costs
    ============================ */
    public List<Double> getHistoryCosts() {
        List<Double> costs = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;

            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length > 1) {
                    costs.add(Double.parseDouble(values[1]));
                }
            }

        } catch (IOException e) {
            System.err.println("Error reading history: " + e.getMessage());
        }

        return costs;
    }

    /* ============================
       Convert CSV → ARFF
    ============================ */
    public static void convertCSVtoARFF(String sourcePath, String destPath) throws Exception {
        Instances processed = preprocessMergedCSV(sourcePath, 10000, 42);
        saveInstancesAsARFF(processed, destPath);
    }

    /* ============================
       Save ARFF File
    ============================ */
    public static void saveInstancesAsARFF(Instances data, String destPath) throws Exception {

        ArffSaver saver = new ArffSaver();
        saver.setInstances(data);
        saver.setFile(new File(destPath));
        saver.writeBatch();

        System.out.println("Conversion successful: " + destPath);
    }

    /* ============================
       Preprocessing Pipeline
    ============================ */
    public static Instances preprocessMergedCSV(String sourcePath,
                                                int maxRows,
                                                int seed) throws Exception {

        System.out.println("Reading dataset: " + sourcePath);

        CSVLoader loader = new CSVLoader();
        loader.setSource(new File(sourcePath));
        loader.setFieldSeparator(",");
        loader.setMissingValue("?");

        Instances data = loader.getDataSet();
        System.out.println("Total records found: " + data.numInstances());

        // Set class attribute (last column)
        if (data.classIndex() == -1) {
            data.setClassIndex(data.numAttributes() - 1);
        }

        // Downsample for performance
        if (maxRows > 0 && data.numInstances() > maxRows) {
            System.out.println("Downsampling to " + maxRows + " rows...");
            data.randomize(new Random(seed));
            data = new Instances(data, 0, maxRows);
        }

        // Handle missing values
        ReplaceMissingValues missingFilter = new ReplaceMissingValues();
        missingFilter.setInputFormat(data);
        Instances missingHandled = Filter.useFilter(data, missingFilter);

        Instances withConnectedLoad = ensureConnectedLoadKw(missingHandled);

        // Remove leakage if predicting units
        Instances deLeaked = removeElectricityBillIfUnitsTarget(withConnectedLoad);

        Instances simplified = keepOnlyConnectedLoadAndUnits(deLeaked);

        // Normalize features
        Normalize normalizeFilter = new Normalize();
        normalizeFilter.setInputFormat(simplified);
        Instances normalized = Filter.useFilter(simplified, normalizeFilter);

        normalized.setClassIndex(normalized.numAttributes() - 1);

        System.out.println("Preprocessing complete. Rows: "
                + normalized.numInstances()
                + ", Columns: "
                + normalized.numAttributes());

       return normalized;
    }

    /* ============================
       Ensure connected_load_kw
    ============================ */
    private static Instances ensureConnectedLoadKw(Instances data) {
        if (data.attribute("connected_load_kw") != null) {
            return data;
        }

        int fanIndex = indexOrThrow(data, "fan") + 1;
        int refrigeratorIndex = indexOrThrow(data, "refrigerator") + 1;
        int airconditionerIndex = indexOrThrow(data, "airconditioner") + 1;
        int televisionIndex = indexOrThrow(data, "television") + 1;
        int monitorIndex = indexOrThrow(data, "monitor") + 1;

        Instances updated = new Instances(data);
        Attribute connectedLoad = new Attribute("connected_load_kw");
        updated.insertAttributeAt(connectedLoad, 0);

        int connectedIndex = updated.attribute("connected_load_kw").index();
        int classIndex = data.classIndex();   // use original dataset index

        for (int i = 0; i < updated.numInstances(); i++) {
            Instance instance = updated.instance(i);
            double connectedLoadKw =
                    (instance.value(fanIndex)
                            + instance.value(refrigeratorIndex)
                            + instance.value(airconditionerIndex)
                            + instance.value(televisionIndex)
                            + instance.value(monitorIndex)) / 1000.0;
            instance.setValue(connectedIndex, connectedLoadKw);
        }

        if (classIndex >= 0 && classIndex < updated.numAttributes()) {
            updated.setClassIndex(updated.numAttributes() - 1);
        }

        return updated;
    }

    private static int indexOrThrow(Instances data, String attributeName) {
        Attribute attribute = data.attribute(attributeName);
        if (attribute == null) {
            throw new IllegalStateException("Missing attribute: " + attributeName);
        }
        return attribute.index();
    }

    private static Instances keepOnlyConnectedLoadAndUnits(Instances data) throws Exception {
        Attribute connectedLoad = data.attribute("connected_load_kw");
        Attribute units = data.attribute("units");
        if (connectedLoad == null || units == null) {
            throw new IllegalStateException("Dataset must contain 'connected_load_kw' and 'units'.");
        }

        int connectedIndex = connectedLoad.index();
        int unitsIndex = units.index();

        Remove remove = new Remove();
        remove.setAttributeIndicesArray(new int[]{connectedIndex, unitsIndex});
        remove.setInvertSelection(true);
        remove.setInputFormat(data);

        Instances filtered = Filter.useFilter(data, remove);
        filtered.setClassIndex(filtered.attribute("units").index());
        return filtered;
    }

    /* ============================
       Prevent Target Leakage
    ============================ */
    private static Instances removeElectricityBillIfUnitsTarget(Instances data) throws Exception {

        int classIndex = data.classIndex();
        String className = data.classAttribute().name().toLowerCase();

        if (data.attribute("electricitybill") != null) {

            int billIndex = data.attribute("electricitybill").index();

            if ("units".equals(className) && billIndex != classIndex) {

                Remove remove = new Remove();
                remove.setAttributeIndicesArray(new int[]{billIndex});
                remove.setInputFormat(data);

                Instances cleaned = Filter.useFilter(data, remove);
                cleaned.setClassIndex(cleaned.numAttributes() - 1);

                System.out.println("Removed 'electricitybill' to avoid leakage.");
                return cleaned;
            }
        }

        return data;
    }

    /* ============================
       Save Prediction History
    ============================ */
    public void saveRecord(String appliance, double cost) {

        try (FileWriter fw = new FileWriter(FILE_PATH, true);
             PrintWriter out = new PrintWriter(fw)) {

            out.println(appliance + "," + cost);
            System.out.println("Record saved to history.csv");

        } catch (IOException e) {
            System.err.println("Error saving data: " + e.getMessage());
        }
    }
}
