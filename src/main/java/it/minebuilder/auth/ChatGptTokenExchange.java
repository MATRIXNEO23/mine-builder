package it.minebuilder.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Exchanges a browser authorization code. Tokens must be validated before use. */
public final class ChatGptTokenExchange {
    private static final URI ENDPOINT = URI.create("https://auth.openai.com/api/accounts/oauth/token");
    private ChatGptTokenExchange() {}

    public static JsonObject exchange(LoopbackRedirect.Callback callback,
                                      OAuthPkce.Attempt attempt, String redirectUri)
            throws IOException, InterruptedException {
        String clientId = callback.clientId();
        if (clientId == null || !clientId.matches("oaiapp_[A-Za-z0-9_-]+"))
            throw new IOException("Missing or invalid issued OAuth client ID");
        String body = form("grant_type", "authorization_code")
            + "&" + form("client_id", clientId)
            + "&" + form("code", callback.code())
            + "&" + form("code_verifier", attempt.verifier())
            + "&" + form("redirect_uri", redirectUri)
            + "&" + form("resource", "https://api.openai.com/v1");
        HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
            .timeout(Duration.ofSeconds(25))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> response = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build()
            .send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200)
            throw new IOException("OAuth token exchange failed (HTTP " + response.statusCode() + ")");
        JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
        if (!result.has("id_token") || !result.has("access_token"))
            throw new IOException("OAuth token response incomplete");
        if (!result.has("scope") || !java.util.Arrays.asList(result.get("scope").getAsString().split(" ")).contains("chatgpt.tokens.use.direct"))
            throw new IOException("ChatGPT plan inference permission was not granted");
        return result;
    }

    private static String form(String name, String value) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8) + "="
            + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
