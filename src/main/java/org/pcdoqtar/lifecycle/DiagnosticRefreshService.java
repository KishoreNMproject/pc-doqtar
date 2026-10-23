package org.pcdoqtar.lifecycle;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.pcdoqtar.diagnostics.DiagnosticTaskManager;
import org.pcdoqtar.diagnostics.network.NetworkDiagnostics.AdapterInfo;
import org.pcdoqtar.model.SystemHealthSnapshot;

public final class DiagnosticRefreshService {

    private final DiagnosticTaskManager taskManager;

    public DiagnosticRefreshService(DiagnosticTaskManager taskManager) {
        this.taskManager = Objects.requireNonNull(
                taskManager, "Diagnostic task manager is required.");
    }

    public CompletableFuture<SystemHealthSnapshot> refreshSystemHealth() {
        return taskManager.collectSnapshot();
    }

    public CompletableFuture<List<AdapterInfo>> refreshNetworkAdapters() {
        return taskManager.collectNetworkAdapters();
    }

    public boolean isClosed() {
        return taskManager.isClosed();
    }
}