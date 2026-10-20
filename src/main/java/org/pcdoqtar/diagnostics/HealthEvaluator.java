
package org.pcdoqtar.diagnostics;

import org.pcdoqtar.model.DiagnosticResult;
import org.pcdoqtar.model.HealthStatus;

public class HealthEvaluator {

    private static final double WARNING_THRESHOLD = 75.0;
    private static final double CRITICAL_THRESHOLD = 90.0;

    public DiagnosticResult evaluateUtilisation(
            String metric,
            double percentage) {

        if (Double.isNaN(percentage)
                || Double.isInfinite(percentage)
                || percentage < 0.0
                || percentage > 100.0) {

            return new DiagnosticResult(
                    metric,
                    percentage,
                    "%",
                    HealthStatus.UNAVAILABLE,
                    "Utilisation value is invalid or unavailable.");
        }

        if (percentage > CRITICAL_THRESHOLD) {
            return new DiagnosticResult(
                    metric,
                    percentage,
                    "%",
                    HealthStatus.CRITICAL,
                    "Critical utilisation: exceeds 90%.");
        }

        if (percentage > WARNING_THRESHOLD) {
            return new DiagnosticResult(
                    metric,
                    percentage,
                    "%",
                    HealthStatus.WARNING,
                    "High utilisation: exceeds 75%.");
        }

        return new DiagnosticResult(
                metric,
                percentage,
                "%",
                HealthStatus.NORMAL,
                "Utilisation is within the normal range.");
    }
}
