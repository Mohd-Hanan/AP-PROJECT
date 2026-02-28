package org.example;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

public class DataHandler {
    private static final String FILE_PATH = "src/main/resources/data/history.csv";

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

    public static void convertCSVtoARFF(String sourcePath, String destPath) throws Exception {
        Instances processed = preprocessMergedCSV(sourcePath, 10000, 42);

        ArffSaver saver = new ArffSaver();
        saver.setInstances(processed);
        saver.setFile(new File(destPath));
        saver.writeBatch();
        System.out.println("Conversion successful: " + destPath);
    }

    public static Instances preprocessMergedCSV(String sourcePath, int maxRows, int seed) throws Exception {
        System.out.println("Reading dataset: " + sourcePath);

        CSVLoader loader = new CSVLoader();
        loader.setSource(new File(sourcePath));
        loader.setFieldSeparator(",");
        loader.setMissingValue("?");

        Instances data = loader.getDataSet();
        System.out.println("Total records found: " + data.numInstances());

        if (data.classIndex() == -1) {
            data.setClassIndex(data.numAttributes() - 1);
        }

        if (maxRows > 0 && data.numInstances() > maxRows) {
            System.out.println("Downsampling to " + maxRows + " rows for performance...");
            data.randomize(new Random(seed));
            data = new Instances(data, 0, maxRows);
        }

        ReplaceMissingValues missingFilter = new ReplaceMissingValues();
        missingFilter.setInputFormat(data);
        Instances missingHandled = Filter.useFilter(data, missingFilter);

        Normalize normalizeFilter = new Normalize();
        normalizeFilter.setInputFormat(missingHandled);
        Instances normalized = Filter.useFilter(missingHandled, normalizeFilter);
        normalized.setClassIndex(normalized.numAttributes() - 1);

        System.out.println("Preprocessing complete. Rows: " + normalized.numInstances() + ", Columns: " + normalized.numAttributes());
        return normalized;
    }

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
