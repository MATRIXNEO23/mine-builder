package it.minebuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * In-process networking for Mine Builder. No external bridge executable is required.
 * Authentication and GPT requests will be integrated only after the supported
 * account-consent flow is verified. No API keys or credentials are embedded here.
 */
public final class LocalGptBridge {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build();

    private LocalGptBridge() {}

    /**
     * Asynchronous HTTPS transport for an authenticated, explicitly approved
     * endpoint. This transport does not initiate OAuth or spend API credits.
     */
    public static CompletableFuture<String> getPublicHttps(URI endpoint) {
        if (endpoint == null || !"https".equalsIgnoreCase(endpoint.getScheme())) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Only HTTPS endpoints are permitted"));
        }
        HttpRequest request = HttpRequest.newBuilder(endpoint)
            .timeout(Duration.ofSeconds(8))
            .GET()
            .build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() != 200) {
                    throw new IllegalStateException("HTTPS request failed: HTTP " + response.statusCode());
                }
                return response.body();
            });
    }
}
