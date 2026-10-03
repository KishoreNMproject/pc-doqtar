package org.pcdoqtar;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;	

public class ServerConnection {

    private static final String SERVER_URL = "https://pc-doqtar-connection-service.vercel.app";

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public static boolean checkConnection()
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + "/sessions"))
                .GET()
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        return response.statusCode() == 200
                && Boolean.parseBoolean(response.body().trim());
    }

    public static String getSessionId()
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + "/getsession"))
                .GET()
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return response.body().trim();
        }

        throw new IOException(
                "Failed to get session ID. HTTP status: "
                        + response.statusCode());
    }
}