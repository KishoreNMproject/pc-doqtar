
package org.pcdoqtar.diagnostics.cpu;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

public class CpuDiagnostics {

    private final CentralProcessor processor;
    private long[] previousTicks;

    public CpuDiagnostics() {
        SystemInfo systemInfo = new SystemInfo();
        processor = systemInfo.getHardware().getProcessor();
        previousTicks = processor.getSystemCpuLoadTicks();
    }

    public double getCpuUsage() {
        double usage = processor.getSystemCpuLoadBetweenTicks(previousTicks) * 100;
        previousTicks = processor.getSystemCpuLoadTicks();

        return Math.max(0, Math.min(100, usage));
    }

    public String getProcessorName() {
        return processor.getProcessorIdentifier().getName();
    }

    public int getLogicalProcessorCount() {
        return processor.getLogicalProcessorCount();
    }

    public void runCheck() {
        System.out.println("===== CPU DIAGNOSTICS =====");
        System.out.println("Processor: " + getProcessorName());
        System.out.println("Logical processors: " + getLogicalProcessorCount());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("CPU check interrupted.");
            return;
        }

        System.out.printf("CPU utilisation: %.2f%%%n", getCpuUsage());
        System.out.println("===========================");
    }
}
