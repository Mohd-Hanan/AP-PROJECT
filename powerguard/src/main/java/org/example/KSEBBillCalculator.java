package org.example;

public class KSEBBillCalculator {

    public static double calculate(double units) {
        if (units <= 0) {
            return 0.0;
        }

        double remaining = units;
        double total = 0.0;

        total += applySlab(remaining, 50, 3.15);
        remaining -= 50;
        if (remaining <= 0) {
            return total;
        }

        total += applySlab(remaining, 50, 3.70);
        remaining -= 50;
        if (remaining <= 0) {
            return total;
        }

        total += applySlab(remaining, 50, 4.80);
        remaining -= 50;
        if (remaining <= 0) {
            return total;
        }

        total += applySlab(remaining, 50, 6.40);
        remaining -= 50;
        if (remaining <= 0) {
            return total;
        }

        total += applySlab(remaining, 50, 7.60);
        remaining -= 50;
        if (remaining <= 0) {
            return total;
        }

        total += remaining * 8.80;

        return total;
    }

    private static double applySlab(double units, double slabSize, double rate) {
        double billable = Math.min(units, slabSize);
        return billable * rate;
    }
}
