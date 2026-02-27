package org.example;

public class PredictionResult {
    private double predictedEnergyUnits; // From UML [cite: 163]
    private double estimatedCost; // From UML [cite: 163]

    public PredictionResult(double units, double costPerUnit) {
        this.predictedEnergyUnits = units;
        this.estimatedCost = units * costPerUnit;
    }

    // Fulfills "User views prediction result" [cite: 141, 166]
    public void displayResult() {
        System.out.println("Predicted Consumption: " + predictedEnergyUnits + " kWh");
        System.out.println("Estimated Monthly Cost: ₹" + estimatedCost);
    }
}