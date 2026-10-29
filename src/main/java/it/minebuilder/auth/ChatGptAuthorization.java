package it.minebuilder.auth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Official Sign in with ChatGPT open-source first-registration URL. */
public final class ChatGptAuthorization {
    private static final String AUTHORIZE = "https://auth.openai.com/api/accounts/authorize";
    private static final String SCOPES =
        "openid profile email offline_access resource.invoke chatgpt.tokens.use.direct";
    private static final String RESOURCE = "https://api.openai.com/v1";

    private ChatGptAuthorization() {}

    public static String initialUrl(String hostId, String redirectUri, OAuthPkce.Attempt attempt) {
        if (hostId == null || !hostId.startsWith("urn:uuid:"))
            throw new IllegalArgumentException("Persistent urn:uuid host ID required");
        if (redirectUri == null || !redirectUri.matches("http://127\\.0\\.0\\.1:[0-9]+/auth/callback"))
            throw new IllegalArgumentException("Only the official loopback redirect is allowed");
        StringBuilder url = new StringBuilder(AUTHORIZE);
        append(url, "client_id", "dynamic_agent_client");
        append(url, "agent_name_hint", "Mine Builder");
        append(url, "ext_agent_host_id", hostId);
        append(url, "response_type", "code");
        append(url, "redirect_uri", redirectUri);
        append(url, "scope", SCOPES);
        append(url, "resource", RESOURCE);
        append(url, "state", attempt.state());
        append(url, "nonce", attempt.nonce());
        append(url, "code_challenge_method", "S256");
        append(url, "code_challenge", attempt.challenge());
        return url.toString();
    }

    private static void append(StringBuilder url, String key, String value) {
        url.append(url.indexOf("?") < 0 ? '?' : '&');
        url.append(key).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }
}
