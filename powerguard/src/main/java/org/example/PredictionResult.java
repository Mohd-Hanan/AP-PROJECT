package org.example;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PredictionResult {

    private final StringProperty device;
    private final StringProperty quantity;
    private final StringProperty hours;
    private final StringProperty units;
    private final StringProperty cost;
    private final StringProperty status;

    public PredictionResult(String device, String cost, String status) {
        this(device, "-", "-", "-", cost, status);
    }

    public PredictionResult(
            String device,
            String quantity,
            String hours,
            String units,
            String cost,
            String status
    ) {
        this.device = new SimpleStringProperty(device);
        this.quantity = new SimpleStringProperty(quantity);
        this.hours = new SimpleStringProperty(hours);
        this.units = new SimpleStringProperty(units);
        this.cost = new SimpleStringProperty(cost);
        this.status = new SimpleStringProperty(status);
    }

    public StringProperty deviceProperty() {
        return device;
    }

    public StringProperty costProperty() {
        return cost;
    }

    public StringProperty quantityProperty() {
        return quantity;
    }

    public StringProperty hoursProperty() {
        return hours;
    }

    public StringProperty unitsProperty() {
        return units;
    }

    public StringProperty statusProperty() {
        return status;
    }
}
