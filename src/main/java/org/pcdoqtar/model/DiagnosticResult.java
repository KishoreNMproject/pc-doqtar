
package org.pcdoqtar.model;

public class DiagnosticResult {

    private final String metric;
    private final double value;
    private final String unit;
    private final HealthStatus status;
    private final String message;

    public DiagnosticResult(
            String metric,
            double value,
            String unit,
            HealthStatus status,
            String message) {

        this.metric = metric;
        this.value = value;
        this.unit = unit;
        this.status = status;
        this.message = message;
    }

    public String getMetric() {
        return metric;
    }

    public double getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    public HealthStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
