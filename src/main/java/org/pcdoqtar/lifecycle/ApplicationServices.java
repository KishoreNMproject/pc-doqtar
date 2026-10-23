
package org.pcdoqtar.lifecycle;

import java.util.concurrent.atomic.AtomicBoolean;

import org.pcdoqtar.auth.AuthSession;
import org.pcdoqtar.auth.AuthenticationCoordinator;
import org.pcdoqtar.auth.FirebaseAuthService;
import org.pcdoqtar.connection.ServerHandover;
import org.pcdoqtar.diagnostics.DiagnosticTaskManager;
import org.pcdoqtar.diagnostics.SystemDiagnostics;
import org.pcdoqtar.diagnostics.network.NetworkDiagnostics;

public final class ApplicationServices implements AutoCloseable {

    private final AuthSession authSession;
    private final FirebaseAuthService authService;
    private final AuthenticationCoordinator authenticationCoordinator;
    private final SystemDiagnostics systemDiagnostics;
    private final NetworkDiagnostics networkDiagnostics;
    private final DiagnosticTaskManager diagnosticTaskManager;
    private final ServerHandover serverHandover;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    public ApplicationServices() {
        authSession = new AuthSession();
        authService = new FirebaseAuthService(authSession);
        authenticationCoordinator =
                new AuthenticationCoordinator(authService);

        systemDiagnostics = new SystemDiagnostics();
        networkDiagnostics = new NetworkDiagnostics();

        diagnosticTaskManager = new DiagnosticTaskManager(
                systemDiagnostics,
                networkDiagnostics);

        serverHandover = new ServerHandover(authService);
    }

    public AuthSession getAuthSession() {
        return authSession;
    }

    public FirebaseAuthService getAuthService() {
        return authService;
    }

    public AuthenticationCoordinator getAuthenticationCoordinator() {
        return authenticationCoordinator;
    }

    public SystemDiagnostics getSystemDiagnostics() {
        return systemDiagnostics;
    }

    public NetworkDiagnostics getNetworkDiagnostics() {
        return networkDiagnostics;
    }

    public DiagnosticTaskManager getDiagnosticTaskManager() {
        return diagnosticTaskManager;
    }

    public ServerHandover getServerHandover() {
        return serverHandover;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        diagnosticTaskManager.close();
        authenticationCoordinator.close();
        authService.logout();
    }
}
