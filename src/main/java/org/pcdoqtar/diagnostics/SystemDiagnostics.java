
package org.pcdoqtar.diagnostics;

import org.pcdoqtar.diagnostics.cpu.CpuDiagnostics;
import org.pcdoqtar.diagnostics.memory.MemoryDiagnostics;

public class SystemDiagnostics {

    private final CpuDiagnostics cpuDiagnostics;
    private final MemoryDiagnostics memoryDiagnostics;

    public SystemDiagnostics() {
        cpuDiagnostics = new CpuDiagnostics();
        memoryDiagnostics = new MemoryDiagnostics();
    }

    public void runBasicCheck() {
        System.out.println();
        System.out.println("################################");
        System.out.println("       PC DOQTAR HEALTH CHECK");
        System.out.println("################################");

        cpuDiagnostics.runCheck();
        memoryDiagnostics.runCheck();

        System.out.println("PC DOQTAR basic health check complete.");
    }
}
