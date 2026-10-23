package org.pcdoqtar.lifecycle;

import java.util.Objects;

public final class DashboardPageContextFactory {

    private DashboardPageContextFactory() {
        // Utility class.
    }

    public static DashboardPageContext create(
            ApplicationServices services) {

        Objects.requireNonNull(
                services, "Application services are required.");

        return new DashboardPageContext(
                services.getSystemDiagnostics(),
                services.getNetworkDiagnostics(),
                services.getDiagnosticTaskManager());
    }
}