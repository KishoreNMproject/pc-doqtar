
package org.pcdoqtar.model;

import java.time.LocalDateTime;
import java.util.List;

public class SystemHealthSnapshot {

    private final LocalDateTime timestamp;
    private final DiagnosticResult cpu;
    private final DiagnosticResult memory;
    private final List<DiagnosticResult> storage;

    public SystemHealthSnapshot(
            LocalDateTime timestamp,
            DiagnosticResult cpu,
            DiagnosticResult memory,
            List<DiagnosticResult> storage) {

        this.timestamp = timestamp;
        this.cpu = cpu;
        this.memory = memory;
        this.storage = List.copyOf(storage);
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public DiagnosticResult getCpu() {
        return cpu;
    }

    public DiagnosticResult getMemory() {
        return memory;
    }

    public List<DiagnosticResult> getStorage() {
        return storage;
    }

    public long getWarningCount() {
        long count = 0;

        if (cpu.getStatus() == HealthStatus.WARNING
                || cpu.getStatus() == HealthStatus.CRITICAL) {
            count++;
        }

        if (memory.getStatus() == HealthStatus.WARNING
                || memory.getStatus() == HealthStatus.CRITICAL) {
            count++;
        }

        count += storage.stream()
                .filter(result ->
                        result.getStatus() == HealthStatus.WARNING
                        || result.getStatus() == HealthStatus.CRITICAL)
                .count();

        return count;
    }

    public boolean hasUnavailableResults() {
        return cpu.getStatus() == HealthStatus.UNAVAILABLE
                || memory.getStatus() == HealthStatus.UNAVAILABLE
                || storage.stream().anyMatch(
                        result -> result.getStatus()
                                == HealthStatus.UNAVAILABLE);
    }

    public HealthStatus getOverallStatus() {
        if (cpu.getStatus() == HealthStatus.CRITICAL
                || memory.getStatus() == HealthStatus.CRITICAL
                || storage.stream().anyMatch(
                        result -> result.getStatus()
                                == HealthStatus.CRITICAL)) {
            return HealthStatus.CRITICAL;
        }

        if (cpu.getStatus() == HealthStatus.WARNING
                || memory.getStatus() == HealthStatus.WARNING
                || storage.stream().anyMatch(
                        result -> result.getStatus()
                                == HealthStatus.WARNING)) {
            return HealthStatus.WARNING;
        }

        if (hasUnavailableResults()) {
            return HealthStatus.UNAVAILABLE;
        }

        return HealthStatus.NORMAL;
    }
}
