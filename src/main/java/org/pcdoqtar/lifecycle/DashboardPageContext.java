package org.pcdoqtar.lifecycle;

import java.util.Objects;

import org.pcdoqtar.diagnostics.DiagnosticTaskManager;
import org.pcdoqtar.diagnostics.SystemDiagnostics;
import org.pcdoqtar.diagnostics.network.NetworkDiagnostics;

public final class DashboardPageContext {

    private final SystemDiagnostics systemDiagnostics;
    private final NetworkDiagnostics networkDiagnostics;
    private final DiagnosticRefreshService refreshService;

    public DashboardPageContext(
            SystemDiagnostics systemDiagnostics,
            NetworkDiagnostics networkDiagnostics,
            DiagnosticTaskManager taskManager) {

        this.systemDiagnostics = Objects.requireNonNull(
                systemDiagnostics, "System diagnostics is required.");

        this.networkDiagnostics = Objects.requireNonNull(
                networkDiagnostics, "Network diagnostics is required.");

        this.refreshService = new DiagnosticRefreshService(
                Objects.requireNonNull(
                        taskManager, "Diagnostic task manager is required."));
    }

    public SystemDiagnostics getSystemDiagnostics() {
        return systemDiagnostics;
    }

    public NetworkDiagnostics getNetworkDiagnostics() {
        return networkDiagnostics;
    }

    public DiagnosticRefreshService getRefreshService() {
        return refreshService;
    }
}