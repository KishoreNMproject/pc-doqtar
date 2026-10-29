package org.pcdoqtar.connection;

import java.net.InetAddress;
import java.time.ZoneId;
import java.util.List;

import org.pcdoqtar.model.DiagnosticResult;
import org.pcdoqtar.model.SystemHealthSnapshot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Converts a client SystemHealthSnapshot into the server's DiagnosticUploadRequest JSON. */
final class SnapshotMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SnapshotMapper() {
    }

    static String toJson(String sessionId, SystemHealthSnapshot snapshot) {
        ObjectNode n = MAPPER.createObjectNode();

        n.put("sessionId", limit(sessionId, 64));
        n.put("hostname", limit(hostname(), 128));
        n.put("osInfo", limit(System.getProperty("os.name") + " "
                + System.getProperty("os.version") + " ("
                + System.getProperty("os.arch") + ")", 255));

        putPercent(n, "cpuUsagePercent", snapshot.getCpu());
        putPercent(n, "memoryUsagePercent", snapshot.getMemory());

        // Highest usage across all drives.
        List<DiagnosticResult> storage = snapshot.getStorage();
        double worst = -1;
        for (DiagnosticResult r : storage) {
            if (isPercent(r)) {
                worst = Math.max(worst, r.getValue());
            }
        }
        if (worst >= 0) {
            n.put("storageUsagePercent", clamp(worst));
        }

        n.put("runningProcessCount",
                (int) Math.min(Integer.MAX_VALUE,
                        ProcessHandle.allProcesses().count()));
        n.put("overallHealthStatus", snapshot.getOverallStatus().name());
        n.put("diagnosticSummary", limit(
                snapshot.getWarningCount() + " warning(s) detected.", 1000));
        n.put("capturedAt", snapshot.getTimestamp()
                .atZone(ZoneId.systemDefault()).toInstant().toString());

        return n.toString();
    }

    private static void putPercent(ObjectNode n, String field, DiagnosticResult r) {
        if (isPercent(r)) {
            n.put(field, clamp(r.getValue()));
        }
    }

    private static boolean isPercent(DiagnosticResult r) {
        return r != null
                && "%".equals(r.getUnit())
                && !Double.isNaN(r.getValue())
                && !Double.isInfinite(r.getValue());
    }

    private static double clamp(double v) {
        return Math.round(Math.max(0, Math.min(100, v)) * 10.0) / 10.0;
    }

    private static String limit(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown-host";
        }
    }
}
