package org.example;

public class EnergyUsageService {

    private static final int DAYS_PER_MONTH = 30;
    // Typical grid emission factor used for India-scale estimate (kg CO2 per kWh).
    private static final double CO2_KG_PER_KWH = 0.708;
    private static final double MODEL_UNITS_BASELINE = 4533.84;
    private static final double MIN_MODEL_FACTOR = 0.70;
    private static final double MAX_MODEL_FACTOR = 1.30;

    public PredictionMetrics calculate(
            String deviceName,
            int wattage,
            int quantity,
            double dailyHours,
            ApplianceModel predictor
    ) {
        double monthlyUnitsPhysical = (wattage * quantity * dailyHours * DAYS_PER_MONTH) / 1000.0;
        double modelFactor = 1.0;
        double modelRawUnits = 0.0;

        if (predictor != null) {
            try {
                double[] features = buildFeatureVector(deviceName, wattage, quantity, dailyHours);
                modelRawUnits = predictor.predictUnits(
                        features[0], features[1], features[2], features[3], features[4]
                );
                if (modelRawUnits > 0) {
                    double ratio = modelRawUnits / MODEL_UNITS_BASELINE;
                    modelFactor = clamp(ratio, MIN_MODEL_FACTOR, MAX_MODEL_FACTOR);
                }
            } catch (Exception ignored) {
                modelFactor = 1.0;
            }
        }

        double monthlyUnitsAdjusted = monthlyUnitsPhysical * modelFactor;
        double bill = KSEBBillCalculator.calculate(monthlyUnitsAdjusted);
        double co2 = monthlyUnitsAdjusted * CO2_KG_PER_KWH;

        return new PredictionMetrics(
                monthlyUnitsPhysical,
                monthlyUnitsAdjusted,
                modelRawUnits,
                modelFactor,
                bill,
                co2
        );
    }

    private double[] buildFeatureVector(String deviceName, int wattage, int quantity, double dailyHours) {
        double dailyKWh = (wattage * quantity * dailyHours) / 1000.0;
        double fan = 0;
        double fridge = 0;
        double ac = 0;
        double tv = 0;
        double monitor = 0;
        String name = deviceName == null ? "" : deviceName.toLowerCase();

        if (name.contains("ac") || name.contains("connector")) {
            ac = dailyKWh;
        } else if (name.contains("refrigerator") || name.contains("fridge")) {
            fridge = dailyKWh;
        } else if (name.contains("tv") || name.contains("playstation")) {
            tv = dailyKWh;
        } else if (name.contains("fan")) {
            fan = dailyKWh;
        } else {
            monitor = dailyKWh;
        }
        return new double[]{fan, fridge, ac, tv, monitor};
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public record PredictionMetrics(
            double physicalUnits,
            double adjustedUnits,
            double modelRawUnits,
            double modelFactor,
            double billAmount,
            double co2Kg
    ) {
    }
}
