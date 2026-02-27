package org.example;

public class Appliance {
    private String applianceId;
    private String applianceName;
    private double defaultPowerRating; // in Watts

    public Appliance(String id, String name, double rating) {
        this.applianceId = id;
        this.applianceName = name;
        this.defaultPowerRating = rating;
    }

    // Methods defined in your UML
    public double getpowerRating() {
        return defaultPowerRating;
    }

    public void updatepowerRating(double newRating) {
        this.defaultPowerRating = newRating;
    }

    public String getApplianceName() {
        return applianceName;
    }
}