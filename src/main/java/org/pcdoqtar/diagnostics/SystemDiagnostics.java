
package org.pcdoqtar.diagnostics;

import org.pcdoqtar.diagnostics.cpu.CpuDiagnostics;
import org.pcdoqtar.diagnostics.memory.MemoryDiagnostics;
import org.pcdoqtar.diagnostics.network.NetworkDiagnostics;
import org.pcdoqtar.diagnostics.network.NetworkPerformanceTest;
import org.pcdoqtar.diagnostics.process.ProcessDiagnostics;
import org.pcdoqtar.diagnostics.storage.StorageDiagnostics;
import org.pcdoqtar.model.DiagnosticResult;
import org.pcdoqtar.model.SystemHealthSnapshot;

import java.time.LocalDateTime;

public class SystemDiagnostics {

    private final CpuDiagnostics cpuDiagnostics;
    private final MemoryDiagnostics memoryDiagnostics;
    private final StorageDiagnostics storageDiagnostics;
    private final NetworkDiagnostics networkDiagnostics;
    private final ProcessDiagnostics processDiagnostics;
    private final NetworkPerformanceTest networkPerformanceTest;
    
    public SystemDiagnostics() {
        cpuDiagnostics = new CpuDiagnostics();
        memoryDiagnostics = new MemoryDiagnostics();
        storageDiagnostics = new StorageDiagnostics();
        networkDiagnostics = new NetworkDiagnostics();
        processDiagnostics = new ProcessDiagnostics();
        networkPerformanceTest = new NetworkPerformanceTest();
    }

    public void runBasicCheck() {
        System.out.println();
        System.out.println("################################");
        System.out.println("       PC DOQTAR HEALTH CHECK");
        System.out.println("################################");

        cpuDiagnostics.runCheck();
        memoryDiagnostics.runCheck();
        storageDiagnostics.runCheck();
        networkDiagnostics.runCheck();
        networkPerformanceTest.runCheck();
        processDiagnostics.runCheck();
        

        System.out.println("PC DOQTAR Master PC Check complete.");
    }
    public SystemHealthSnapshot collectSnapshot() {
        DiagnosticResult cpu = cpuDiagnostics.diagnose();
        DiagnosticResult memory = memoryDiagnostics.diagnose();

        java.util.List<DiagnosticResult> storage =
                storageDiagnostics.diagnose();

        return new SystemHealthSnapshot(
                LocalDateTime.now(),
                cpu,
                memory,
                storage);
    }
}
