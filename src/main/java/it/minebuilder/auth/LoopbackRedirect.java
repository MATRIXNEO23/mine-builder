package it.minebuilder.auth;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;

/** Receives a single browser redirect on the local computer. */
public final class LoopbackRedirect implements AutoCloseable {
    private final HttpServer server;
    private final String expectedState;
    private final CompletableFuture<String> code = new CompletableFuture<>();

    private LoopbackRedirect(HttpServer server, String state) {
        this.server = server;
        this.expectedState = state;
    }

    public static LoopbackRedirect start(String state) throws IOException {
        if (state == null || state.isBlank()) throw new IllegalArgumentException("state required");
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 1);
        LoopbackRedirect receiver = new LoopbackRedirect(server, state);
        server.createContext("/callback", exchange -> {
            String query = exchange.getRequestURI().getRawQuery();
            boolean accepted = false;
            if ("GET".equals(exchange.getRequestMethod()) && query != null) {
                String foundState = null;
                String foundCode = null;
                for (String entry : query.split("&")) {
                    String[] parts = entry.split("=", 2);
                    if (parts.length != 2) continue;
                    if ("state".equals(parts[0])) foundState = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                    if ("code".equals(parts[0])) foundCode = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                }
                if (receiver.expectedState.equals(foundState) && foundCode != null && !foundCode.isBlank()) {
                    receiver.code.complete(foundCode);
                    accepted = true;
                }
            }
            byte[] response = (accepted ? "Return to Minecraft." : "Invalid authorization response.")
                .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(accepted ? 200 : 400, response.length);
            try (var output = exchange.getResponseBody()) { output.write(response); }
        });
        server.start();
        return receiver;
    }

    public String redirectUri() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/callback";
    }

    public CompletableFuture<String> authorizationCode() {
        return code;
    }

    public CompletableFuture<String> awaitCode(Duration timeout) {
        return code.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override public void close() {
        server.stop(0);
    }
}
