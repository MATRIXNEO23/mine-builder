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
                throw new IOException("Inference HTTP " + response.statusCode());
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
