package it.minebuilder;

import com.google.gson.*;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/** Free public Wikimedia Commons discovery; no paid API or guessed facts. */
public final class ArchitectureResearch {
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10)).build();
    private ArchitectureResearch() {}

    /** One bounded public thumbnail, downloaded only from Wikimedia's own image host. */
    public record VisualReference(String title, byte[] image, String mime) {}

    public static VisualReference visualReference(String request) throws Exception {
        if (request == null || request.isBlank()) return null;
        String clean = request.replaceAll("[\\r\\n\\t]", " ").trim();
        if (clean.length() > 100) clean = clean.substring(0, 100);
        for (String suffix : new String[]{" floor plan architecture", " architectural exterior"}) {
            String query = URLEncoder.encode(clean + suffix, StandardCharsets.UTF_8);
            String url = "https://commons.wikimedia.org/w/api.php?action=query&generator=search"
                + "&gsrsearch=" + query + "&gsrnamespace=6&gsrlimit=6"
                + "&prop=imageinfo&iiprop=url&iiurlwidth=640&format=json";
            HttpRequest requestHttp = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15)).header("User-Agent", "MineBuilder/0.2")
                .GET().build();
            HttpResponse<String> response = HTTP.send(requestHttp, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body().length() > 1024 * 1024) continue;
            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            if (!root.has("query") || !root.getAsJsonObject("query").has("pages")) continue;
            for (JsonElement element : root.getAsJsonObject("query").getAsJsonObject("pages").asMap().values()) {
                JsonObject page = element.getAsJsonObject();
                if (!page.has("imageinfo")) continue;
                JsonObject info = page.getAsJsonArray("imageinfo").get(0).getAsJsonObject();
                if (!info.has("thumburl")) continue;
                URI uri = URI.create(info.get("thumburl").getAsString());
                if (!"https".equals(uri.getScheme()) || !"upload.wikimedia.org".equals(uri.getHost())) continue;
                String path = uri.getPath().toLowerCase(Locale.ROOT);
                String mime = path.endsWith(".png") ? "image/png"
                    : path.endsWith(".jpg") || path.endsWith(".jpeg") ? "image/jpeg" : null;
                if (mime == null) continue;
                HttpRequest imageRequest = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "MineBuilder/0.2").GET().build();
                HttpResponse<byte[]> img = HTTP.send(imageRequest, HttpResponse.BodyHandlers.ofByteArray());
                if (img.statusCode() == 200 && img.body().length > 0 && img.body().length <= 4 * 1024 * 1024)
                    return new VisualReference(page.get("title").getAsString(), img.body(), mime);
            }
        }
        return null;
    }

    /** Free Wikipedia summary leads for factual context; no measurements are treated as verified. */
    public static String encyclopedicReferences(String request) throws Exception {
        if (request == null || request.isBlank()) return "No encyclopedia query.";
        String clean = request.replaceAll("[\\r\\n\\t]", " ").trim();
        if (clean.length() > 100) clean = clean.substring(0, 100);
        String url = "https://en.wikipedia.org/w/api.php?action=query&generator=search"
            + "&gsrsearch=" + URLEncoder.encode(clean + " architecture", StandardCharsets.UTF_8)
            + "&gsrlimit=3&prop=extracts&exintro=1&explaintext=1&exchars=900&format=json";
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(12))
            .header("User-Agent", "MineBuilder/0.3 (public architectural reference research)").GET().build();
        HttpResponse<String> response = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 || response.body().length() > 1024 * 1024)
            throw new IOException("Wikipedia lookup unavailable");
        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        if (!root.has("query") || !root.getAsJsonObject("query").has("pages"))
            return "No encyclopedia references found.";
        StringBuilder out = new StringBuilder("Wikipedia article leads (not independently verified measurements):\n");
        for (JsonElement item : root.getAsJsonObject("query").getAsJsonObject("pages").asMap().values()) {
            JsonObject page = item.getAsJsonObject();
            if (!page.has("title")) continue;
            String title = page.get("title").getAsString();
            out.append(title).append(": ");
            if (page.has("extract")) out.append(page.get("extract").getAsString().replaceAll("[\\r\\n]", " "));
            out.append("\n");
        }
        return out.toString();
    }

    /** Reference discovery for the planning stage. Metadata only: never claim visual analysis. */
    public static String planningReferences(String request) throws Exception {
        if (request == null || request.isBlank()) return "No reference query.";
        String clean = request.replaceAll("[\\r\\n\\t]", " ").trim();
        if (clean.length() > 120) clean = clean.substring(0, 120);
        StringBuilder out = new StringBuilder("Public reference leads (titles and links, NOT inspected drawings):\\n");
        int total = 0;
        for (String suffix : new String[]{" architecture exterior", " floor plan architectural drawing"}) {
            String query = URLEncoder.encode(clean + suffix, StandardCharsets.UTF_8);
            String url = "https://commons.wikimedia.org/w/api.php?action=query&generator=search"
                + "&gsrsearch=" + query + "&gsrnamespace=6&gsrlimit=4&prop=imageinfo"
                + "&iiprop=url&format=json";
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "MineBuilder/0.2 (local architecture research)").GET().build();
            HttpResponse<String> response = HTTP.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body().length() > 1024 * 1024)
                throw new IOException("Wikimedia reference search unavailable: HTTP " + response.statusCode());
            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            if (!root.has("query") || !root.getAsJsonObject("query").has("pages")) continue;
            for (Map.Entry<String, JsonElement> entry :
                    root.getAsJsonObject("query").getAsJsonObject("pages").entrySet()) {
                JsonObject page = entry.getValue().getAsJsonObject();
                if (!page.has("title")) continue;
                String title = page.get("title").getAsString();
                String pageUrl = "https://commons.wikimedia.org/wiki/"
                    + URLEncoder.encode(title.replace(' ', '_'), StandardCharsets.UTF_8);
                out.append(suffix.trim()).append(": ").append(title)
                    .append(" | ").append(pageUrl).append((char) 10);
                total++;
            }
        }
        if (total == 0) return "No verified public architectural references found.";
        return out.append("Do not invent measurements or claim images were visually inspected.").toString();
    }

    public static String search(String name) throws Exception {
        if (name == null || name.isBlank() || name.length() > 120)
            throw new IllegalArgumentException("Invalid building name");
        String query = URLEncoder.encode(name + " building architecture", StandardCharsets.UTF_8);
        String url = "https://commons.wikimedia.org/w/api.php?action=query&generator=search"
            + "&gsrsearch=" + query + "&gsrnamespace=6&gsrlimit=6"
            + "&prop=imageinfo&iiprop=url%7Cextmetadata&iiurlwidth=640&format=json";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(20)).header("User-Agent", "MineBuilder/0.2 (local architecture research)")
            .GET().build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 || response.body().length() > 1024 * 1024)
            throw new IOException("Commons lookup unavailable");
        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        if (!root.has("query")) return "No Wikimedia Commons images found for: " + name;
        JsonObject pages = root.getAsJsonObject("query").getAsJsonObject("pages");
        StringBuilder out = new StringBuilder("Wikimedia Commons results for " + name + ":\n");
        int count = 0;
        for (Map.Entry<String, JsonElement> item : pages.entrySet()) {
            JsonObject page = item.getValue().getAsJsonObject();
            if (!page.has("imageinfo")) continue;
            JsonObject info = page.getAsJsonArray("imageinfo").get(0).getAsJsonObject();
            String title = page.get("title").getAsString();
            String pageUrl = "https://commons.wikimedia.org/wiki/"
                + URLEncoder.encode(title.replace(' ', '_'), StandardCharsets.UTF_8);
            out.append(title).append(" | ").append(pageUrl);
            if (info.has("thumburl")) out.append(" | preview: ").append(info.get("thumburl").getAsString());
            out.append("\n");
            if (++count == 6) break;
        }
        return out.append("Search results are leads, not verified dimensions.").toString();
    }
}
