package org.pcdoqtar;

public class Application {

    public static void main(String[] args) {

        String sessionId = null;

        try {

            boolean sessionAvailable = ServerConnection.checkConnection();
            System.out.println("Session available: " + sessionAvailable);

            if (!sessionAvailable) {
                sessionId = ServerConnection.getSessionId();
                System.out.println("Session ID: " + sessionId);
            }




        } catch (Exception e) {

            System.out.println("Unable to contact PC DOQTAR server.");
            System.err.println(e);
        }
    }
}