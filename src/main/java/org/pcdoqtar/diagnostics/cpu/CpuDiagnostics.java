
package org.pcdoqtar.diagnostics.cpu;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

import org.pcdoqtar.diagnostics.HealthEvaluator;
import org.pcdoqtar.model.DiagnosticResult;
import org.pcdoqtar.model.HealthStatus;

public class CpuDiagnostics {

    private final CentralProcessor processor;
    private final HealthEvaluator healthEvaluator;
    private long[] previousTicks;

    public CpuDiagnostics() {
        SystemInfo systemInfo = new SystemInfo();
        processor = systemInfo.getHardware().getProcessor();
        healthEvaluator = new HealthEvaluator();
        previousTicks = processor.getSystemCpuLoadTicks();
    }

    public double getCpuUsage() {
        double usage =
                processor.getSystemCpuLoadBetweenTicks(previousTicks) * 100;

        previousTicks = processor.getSystemCpuLoadTicks();

        return Math.max(0, Math.min(100, usage));
    }

    public String getProcessorName() {
        return processor.getProcessorIdentifier().getName();
    }

    public int getLogicalProcessorCount() {
        return processor.getLogicalProcessorCount();
    }

    public DiagnosticResult diagnose() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            return new DiagnosticResult(
                    "CPU utilisation",
                    Double.NaN,
                    "%",
                    HealthStatus.UNAVAILABLE,
                    "CPU check interrupted.");
        }

        double usage = getCpuUsage();

        return healthEvaluator.evaluateUtilisation(
                "CPU utilisation", usage);
    }

    public void runCheck() {
        System.out.println("===== CPU DIAGNOSTICS =====");
        System.out.println("Processor: " + getProcessorName());
        System.out.println(
                "Logical processors: " + getLogicalProcessorCount());

        DiagnosticResult result = diagnose();

        System.out.printf(
                "CPU utilisation: %.2f%%%n", result.getValue());

        System.out.println("Health status: " + result.getStatus());

        if (result.getStatus() != HealthStatus.NORMAL) {
            System.out.println("NOTICE: " + result.getMessage());
        }

        System.out.println("===========================");
    }
}
