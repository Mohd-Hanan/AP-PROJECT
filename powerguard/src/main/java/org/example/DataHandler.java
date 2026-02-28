package org.example;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import java.io.File;

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
        System.out.println("Reading large dataset: " + sourcePath);
        CSVLoader loader = new CSVLoader();
        loader.setSource(new File(sourcePath));
        loader.setFieldSeparator(",");
        loader.setMissingValue("?");

        // This triggers the actual reading of the file
        Instances data = loader.getDataSet();

        // Progress print after loading into memory
        System.out.println("Total records found: " + data.numInstances());

        if (data.numInstances() > 10000) {
            System.out.println("Downsampling to 10,000 rows for performance...");
            data = new Instances(data, 0, 10000);
        }

        ArffSaver saver = new ArffSaver();
        saver.setInstances(data);
        saver.setFile(new File(destPath));
        saver.writeBatch();
        System.out.println("Conversion successful: " + destPath);
    }
    public void saveRecord(String appliance, double cost) {
        try (FileWriter fw = new FileWriter(FILE_PATH, true);
             PrintWriter out = new PrintWriter(fw)) {
            // Format: ApplianceName, Cost
            out.println(appliance + "," + cost);
            System.out.println("Record saved to history.csv");
        } catch (IOException e) {
            System.err.println("Error saving data: " + e.getMessage());
        }
    }
}
