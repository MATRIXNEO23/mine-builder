package it.minebuilder.auth;

import com.google.gson.*;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** ChatGPT plan Responses API client; only completed streams count as success. */
public final class ChatGptInference {
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(12)).build();
    private ChatGptInference() {}

    public static String firstModel(String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/models"))
            .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + token).GET().build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("Model list HTTP " + response.statusCode());
        JsonArray models = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("models");
        for (JsonElement item : models) {
            JsonObject model = item.getAsJsonObject();
            if (model.has("visibility") && "list".equals(model.get("visibility").getAsString())
                && model.has("slug")) return model.get("slug").getAsString();
        }
        throw new IOException("No ChatGPT plan models available");
    }

    private static String readableError(InputStream stream) throws IOException {
        String raw = new String(stream.readNBytes(4096), StandardCharsets.UTF_8);
        try {
            JsonElement root = JsonParser.parseString(raw);
            if (root.isJsonObject()) {
                JsonObject json = root.getAsJsonObject();
                JsonElement detail = json.has("error") ? json.get("error") : root;
                if (detail.isJsonObject()) {
                    JsonObject err = detail.getAsJsonObject();
                    String type = err.has("type") && !err.get("type").isJsonNull()
                        ? err.get("type").getAsString() : "unknown";
                    String code = err.has("code") && !err.get("code").isJsonNull()
                        ? err.get("code").getAsString() : "unknown";
                    String msg = err.has("message") && !err.get("message").isJsonNull()
                        ? err.get("message").getAsString() : "";
                    if (!msg.isBlank()) return compact("type=" + type + " code=" + code + " message=" + msg);
                }
                return compact("JSON fields: " + json.keySet() + " body=" + json);
            }
            return compact("Server response: " + root);
        } catch (RuntimeException ignored) {
            return compact("Non-JSON server response: " + raw);
        }
    }

    private static String compact(String value) {
        String clean = value.replaceAll("[\\r\\n\\t]", " ");
        clean = clean.replaceAll("(?i)Bearer\\s+[^\\s\\\"]+", "Bearer [redacted]");
        clean = clean.replaceAll("(?i)(access_token|refresh_token|id_token|authorization)\\\"?\\s*[:=]\\s*\\\"?[^\\\"\\s,}]+", "$1=[redacted]");
        return clean.substring(0, Math.min(600, clean.length()));
    }

    public static String ask(String token, String model, String message) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("input", message);
        body.addProperty("store", false);
        body.addProperty("stream", true);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/responses"))
            .timeout(Duration.ofMinutes(2))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8)).build();
        HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream stream = response.body()) {
            if (response.statusCode() != 200)
                throw new IOException("Inference HTTP " + response.statusCode() + ": " + readableError(stream));
            StringBuilder text = new StringBuilder();
            boolean completed = false;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data: ")) continue;
                    String data = line.substring(6);
                    if ("[DONE]".equals(data)) break;
                    JsonObject event = JsonParser.parseString(data).getAsJsonObject();
                    if (!event.has("type")) continue;
                    String type = event.get("type").getAsString();
                    if ("response.output_text.delta".equals(type) && event.has("delta"))
                        text.append(event.get("delta").getAsString());
                    if ("response.completed".equals(type)) completed = true;
                    if ("response.failed".equals(type) || "response.incomplete".equals(type))
                        throw new IOException("Inference did not complete: " + type);
                }
            }
            if (!completed || text.toString().isBlank())
                throw new IOException("No completed text response");
            return text.toString();
        }
    }
}
