package it.minebuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Minecraft-side client for a separate local GPT bridge.
 * Never holds an OAuth token, API key, or remote service credentials.
 * The local bridge must handle account consent and inference independently.
 */
public final class LocalGptBridge {
    private static final URI ENDPOINT = URI.create("http://127.0.0.1:38471/health");
    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build();

    private LocalGptBridge() {}

    /** Non-blocking check: a running bridge is not proof of GPT authentication. */
    public static CompletableFuture<Boolean> isBridgeRunning() {
        HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
            .timeout(Duration.ofSeconds(3))
            .GET()
            .build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding())
            .thenApply(response -> response.statusCode() == 200)
            .exceptionally(error -> false);
    }
}
