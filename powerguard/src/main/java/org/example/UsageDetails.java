package org.example;

public class UsageDetails {
    private int quantity;
    private int usageHours;
    private int usageMinutes;

    public UsageDetails(int quantity, int hours, int minutes) {
        this.quantity = quantity;
        this.usageHours = hours;
        this.usageMinutes = minutes;
    }

    // This method fulfills the 'CalculateConsumption' requirement in your UML
    public double calculateConsumption(double powerRatingWatts) {
        double totalHours = usageHours + (usageMinutes / 60.0);
        // Energy (kWh) = (Watts * Hours * Quantity) / 1000
        return (powerRatingWatts * totalHours * quantity) / 1000.0;
    }
}