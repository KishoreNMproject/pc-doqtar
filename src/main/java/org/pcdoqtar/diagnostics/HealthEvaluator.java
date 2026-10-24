
package org.pcdoqtar.diagnostics;

import org.pcdoqtar.model.DiagnosticResult;
import org.pcdoqtar.model.HealthStatus;
import org.pcdoqtar.settings.AppSettings;

public class HealthEvaluator {

    private static final double CRITICAL_THRESHOLD = 90.0;

    public DiagnosticResult evaluateUtilisation(
            String metric,
            double percentage) {

        if (!Double.isFinite(percentage)
                || percentage < 0.0
                || percentage > 100.0) {

            return new DiagnosticResult(
                    metric,
                    percentage,
                    "%",
                    HealthStatus.UNAVAILABLE,
                    "Utilisation value is invalid or unavailable.");
        }

        // Read the user's saved warning threshold dynamically.
        double warningThreshold =
                AppSettings.getWarningThreshold();

        if (percentage > CRITICAL_THRESHOLD) {
            return new DiagnosticResult(
                    metric,
                    percentage,
                    "%",
                    HealthStatus.CRITICAL,
                    "Critical utilisation: exceeds 90%.");
        }

        if (percentage > warningThreshold) {
            return new DiagnosticResult(
                    metric,
                    percentage,
                    "%",
                    HealthStatus.WARNING,
                    String.format(
                            "High utilisation: exceeds %.0f%%.",
                            warningThreshold));
        }

        return new DiagnosticResult(
                metric,
                percentage,
                "%",
                HealthStatus.NORMAL,
                "Utilisation is within the normal range.");
    }
}
