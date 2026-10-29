package it.minebuilder.auth;

public final class OAuthPkceTest {
    public static void main(String[] args) {
        OAuthPkce.Attempt first = OAuthPkce.newAttempt();
        OAuthPkce.Attempt second = OAuthPkce.newAttempt();
        if (first.verifier().length() < 43 || first.verifier().length() > 128) {
            throw new AssertionError("Invalid PKCE verifier length");
        }
        if (!first.challenge().equals(OAuthPkce.challenge(first.verifier()))) {
            throw new AssertionError("PKCE S256 mismatch");
        }
        if (first.state().equals(second.state()) || first.nonce().equals(second.nonce())) {
            throw new AssertionError("OAuth attempts must be independent");
        }
        if (!"E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM".equals(
                OAuthPkce.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))) {
            throw new AssertionError("RFC 7636 test vector mismatch");
        }
        System.out.println("OAuth PKCE checks passed");
    }
}
