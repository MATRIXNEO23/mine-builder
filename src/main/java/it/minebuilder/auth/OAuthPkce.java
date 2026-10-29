package it.minebuilder.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * RFC 7636 PKCE parameters for a public, locally installed OAuth client.
 * Does not authenticate by itself or store credentials.
 */
public final class OAuthPkce {
    private static final SecureRandom RANDOM = new SecureRandom();

    private OAuthPkce() {}

    public static Attempt newAttempt() {
        String verifier = randomUrlSafe(32);
        String state = randomUrlSafe(32);
        String nonce = randomUrlSafe(32);
        return new Attempt(verifier, challenge(verifier), state, nonce);
    }

    public static String challenge(String verifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by Java 17", e);
        }
    }

    private static String randomUrlSafe(int bytes) {
        byte[] buffer = new byte[bytes];
        RANDOM.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    public record Attempt(String verifier, String challenge, String state, String nonce) {}
}
