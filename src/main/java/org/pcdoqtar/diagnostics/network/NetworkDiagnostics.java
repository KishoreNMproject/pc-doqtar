
package org.pcdoqtar.diagnostics.network;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class NetworkDiagnostics {

    public void runCheck() {
        System.out.println("===== NETWORK DIAGNOSTICS =====");

        int activeInterfaces = 0;

        try {
            Enumeration<NetworkInterface> interfaces =
                    NetworkInterface.getNetworkInterfaces();

            if (interfaces == null) {
                System.out.println("No network interfaces detected.");
                return;
            }

            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface =
                        interfaces.nextElement();

                if (!networkInterface.isUp()
                        || networkInterface.isLoopback()) {
                    continue;
                }

                activeInterfaces++;

                System.out.println("Interface: "
                        + networkInterface.getDisplayName());
                System.out.println("Name: "
                        + networkInterface.getName());
                System.out.println("Status: UP");

                Enumeration<InetAddress> addresses =
                        networkInterface.getInetAddresses();

                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();

                    if (address.isLoopbackAddress()) {
                        continue;
                    }

                    if (address instanceof Inet4Address) {
                        System.out.println("IPv4: "
                                + address.getHostAddress());
                    } else if (address instanceof Inet6Address) {
                        System.out.println("IPv6: "
                                + address.getHostAddress());
                    }
                }

                System.out.println("-------------------------------");
            }

            System.out.println("Active network interfaces: "
                    + activeInterfaces);

            if (activeInterfaces == 0) {
                System.out.println(
                        "WARNING: No active network interfaces detected.");
            }

        } catch (SocketException e) {
            System.err.println(
                    "Unable to inspect network interfaces: "
                            + e.getMessage());
        }

        System.out.println("Network check complete.");
        System.out.println("===============================");
    }
}
