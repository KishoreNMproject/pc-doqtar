package org.pcdoqtar.diagnostics.network;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.SystemInfo;
import oshi.hardware.NetworkIF;

public class NetworkDiagnostics {

    public List<AdapterInfo> collectAdapters() throws SocketException {
        List<AdapterInfo> adapters = new ArrayList<>();

        Map<String, NetworkIF> hardwareInterfaces = new HashMap<>();

        for (NetworkIF networkIF :
                new SystemInfo().getHardware().getNetworkIFs()) {
            networkIF.updateAttributes();
            hardwareInterfaces.put(networkIF.getName(), networkIF);
        }

        Enumeration<NetworkInterface> interfaces =
                NetworkInterface.getNetworkInterfaces();

        if (interfaces == null) {
            return adapters;
        }

        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();

            NetworkIF hardware =
                    hardwareInterfaces.get(networkInterface.getName());

            StringBuilder ipv4 = new StringBuilder();
            StringBuilder ipv6 = new StringBuilder();

            var addresses = networkInterface.getInetAddresses();

            while (addresses.hasMoreElements()) {
                var address = addresses.nextElement();

                if (address.isLoopbackAddress()) {
                    continue;
                }

                if (address instanceof java.net.Inet4Address) {
                    appendAddress(ipv4, address.getHostAddress());
                } else if (address instanceof java.net.Inet6Address) {
                    appendAddress(ipv6, address.getHostAddress());
                }
            }

            String macAddress = "Unavailable";

            try {
                byte[] mac = networkInterface.getHardwareAddress();

                if (mac != null && mac.length > 0) {
                    StringBuilder formatted = new StringBuilder();

                    for (byte value : mac) {
                        if (!formatted.isEmpty()) {
                            formatted.append("-");
                        }

                        formatted.append(
                                String.format("%02X", value & 0xFF)
                        );
                    }

                    macAddress = formatted.toString();
                }
            } catch (SocketException ignored) {
                // Keep the MAC address marked as unavailable.
            }

            long receivedBytes = -1;
            long sentBytes = -1;
            long linkSpeedMbps = -1;

            if (hardware != null) {
                receivedBytes = hardware.getBytesRecv();
                sentBytes = hardware.getBytesSent();

                long speedBitsPerSecond = hardware.getSpeed();

                if (speedBitsPerSecond > 0) {
                    linkSpeedMbps =
                            speedBitsPerSecond / 1_000_000;
                }
            }

            adapters.add(new AdapterInfo(
                    networkInterface.getName(),
                    networkInterface.getDisplayName(),
                    networkInterface.isUp(),
                    networkInterface.isLoopback(),
                    networkInterface.isVirtual(),
                    macAddress,
                    ipv4.toString(),
                    ipv6.toString(),
                    receivedBytes,
                    sentBytes,
                    linkSpeedMbps
            ));
        }

        return adapters;
    }

    private void appendAddress(StringBuilder target, String address) {
        if (!target.isEmpty()) {
            target.append(", ");
        }

        target.append(address);
    }

    public void runCheck() {
        System.out.println("===== NETWORK DIAGNOSTICS =====");

        try {
            List<AdapterInfo> adapters = collectAdapters();

            for (AdapterInfo adapter : adapters) {
                System.out.println("Adapter: " + adapter.getDisplayName());
                System.out.println("Interface: " + adapter.getName());
                System.out.println("Status: "
                        + (adapter.isUp() ? "UP" : "DOWN"));
                System.out.println("MAC: " + adapter.getMacAddress());
                System.out.println("IPv4: " + adapter.getIpv4());
                System.out.println("IPv6: " + adapter.getIpv6());
                System.out.println("Link speed: "
                        + formatMbps(adapter.getLinkSpeedMbps()));
                System.out.println("Received: "
                        + formatBytes(adapter.getReceivedBytes()));
                System.out.println("Sent: "
                        + formatBytes(adapter.getSentBytes()));
                System.out.println("--------------------------------");
            }

            long active = adapters.stream()
                    .filter(AdapterInfo::isUp)
                    .filter(adapter -> !adapter.isLoopback())
                    .count();

            System.out.println("Active non-loopback interfaces: " + active);

        } catch (Exception exception) {
            System.err.println(
                    "Unable to inspect network interfaces: "
                            + exception.getMessage()
            );
        }

        System.out.println("Network check complete.");
        System.out.println("===============================");
    }

    private String formatMbps(long speed) {
        return speed < 0 ? "Unavailable" : speed + " Mbps";
    }

    private String formatBytes(long bytes) {
        if (bytes < 0) {
            return "Unavailable";
        }

        return String.format(
                "%.2f MB", bytes / (1024.0 * 1024.0)
        );
    }

    public static class AdapterInfo {
        private final String name;
        private final String displayName;
        private final boolean up;
        private final boolean loopback;
        private final boolean virtual;
        private final String macAddress;
        private final String ipv4;
        private final String ipv6;
        private final long receivedBytes;
        private final long sentBytes;
        private final long linkSpeedMbps;

        public AdapterInfo(
                String name,
                String displayName,
                boolean up,
                boolean loopback,
                boolean virtual,
                String macAddress,
                String ipv4,
                String ipv6,
                long receivedBytes,
                long sentBytes,
                long linkSpeedMbps) {

            this.name = name;
            this.displayName = displayName;
            this.up = up;
            this.loopback = loopback;
            this.virtual = virtual;
            this.macAddress = macAddress;
            this.ipv4 = ipv4;
            this.ipv6 = ipv6;
            this.receivedBytes = receivedBytes;
            this.sentBytes = sentBytes;
            this.linkSpeedMbps = linkSpeedMbps;
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isUp() {
            return up;
        }

        public boolean isLoopback() {
            return loopback;
        }

        public boolean isVirtual() {
            return virtual;
        }

        public String getMacAddress() {
            return macAddress;
        }

        public String getIpv4() {
            return ipv4.isBlank() ? "Unavailable" : ipv4;
        }

        public String getIpv6() {
            return ipv6.isBlank() ? "Unavailable" : ipv6;
        }

        public long getReceivedBytes() {
            return receivedBytes;
        }

        public long getSentBytes() {
            return sentBytes;
        }

        public long getLinkSpeedMbps() {
            return linkSpeedMbps;
        }
    }
}