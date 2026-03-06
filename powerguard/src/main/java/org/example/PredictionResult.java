package org.example;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PredictionResult {

    private final StringProperty device;
    private final StringProperty cost;
    private final StringProperty carbon;
    private final StringProperty status;

    public PredictionResult(String device, String cost, String carbon, String status) {
        this.device = new SimpleStringProperty(device);
        this.cost = new SimpleStringProperty(cost);
        this.carbon = new SimpleStringProperty(carbon);
        this.status = new SimpleStringProperty(status);
    }

    public StringProperty deviceProperty() {
        return device;
    }

    public StringProperty costProperty() {
        return cost;
    }

    public StringProperty carbonProperty() {
        return carbon;
    }

    public StringProperty statusProperty() {
        return status;
    }
}