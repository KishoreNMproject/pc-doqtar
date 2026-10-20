
package org.pcdoqtar.diagnostics.process;

import java.util.Comparator;
import java.util.List;

import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

public class ProcessDiagnostics {

    private final OperatingSystem operatingSystem;

    public ProcessDiagnostics() {
        SystemInfo systemInfo = new SystemInfo();
        operatingSystem = systemInfo.getOperatingSystem();
    }

    public void runCheck() {
        System.out.println("===== PROCESS DIAGNOSTICS =====");

        List<OSProcess> processes = operatingSystem.getProcesses(
                null,
                Comparator.comparingLong(
                        OSProcess::getResidentMemory).reversed(),
                10);

        if (processes.isEmpty()) {
            System.out.println("No processes detected.");
            return;
        }

        System.out.println("Top 10 processes by RAM usage:");

        for (OSProcess process : processes) {
            double memoryMB =
                    process.getResidentMemory() / (1024.0 * 1024);

            double cpuUsage =
                    process.getProcessCpuLoadCumulative() * 100;

            System.out.printf(
                    "PID: %d | Name: %s | RAM: %.2f MB | CPU: %.2f%%%n",
                    process.getProcessID(),
                    process.getName(),
                    memoryMB,
                    cpuUsage);
        }

        System.out.println("Process check complete.");
        System.out.println("===============================");
    }
}
