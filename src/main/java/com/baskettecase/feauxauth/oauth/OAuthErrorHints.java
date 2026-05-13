package com.baskettecase.feauxauth.oauth;

import java.util.Locale;
import java.util.Map;

/**
 * Maps well-known OAuth 2.0 / OIDC error codes to plain-English hints
 * a developer or end user can act on. Returns {@code null} for unknown
 * codes — callers should fall back to whatever error description the
 * upstream sent.
 */
public final class OAuthErrorHints {

    private OAuthErrorHints() {}

    private static final Map<String, String> HINTS = Map.ofEntries(
        Map.entry("invalid_request",
            "The request is missing a required parameter or contains an invalid value."),
        Map.entry("invalid_client",
            "The client_id is unknown or disabled. Check the client registration in the admin console."),
        Map.entry("invalid_grant",
            "The authorization code, refresh token, or device code is expired, revoked, or already used."),
        Map.entry("unauthorized_client",
            "The client isn't allowed to use this grant type. Check the client's allowed grant types."),
        Map.entry("unsupported_response_type",
            "Only response_type=code is supported. Switch the authorization request to use the authorization code flow."),
        Map.entry("unsupported_grant_type",
            "This grant type isn't enabled on this server. Check the OIDC discovery document for supported grant types."),
        Map.entry("invalid_scope",
            "One of the requested scopes isn't allowed for this client. Check allowed_scopes in the admin console."),
        Map.entry("access_denied",
            "The user declined the authorization request. Have them retry and approve."),
        Map.entry("expired_token",
            "The token has expired. Mint a new one via the token endpoint."),
        Map.entry("server_error",
            "FeauxAuth encountered an unexpected error. Check the server logs for the stack trace."),
        Map.entry("authorization_pending",
            "The user hasn't approved the device code yet. Keep polling at the prescribed interval."),
        Map.entry("slow_down",
            "Polling too aggressively. Increase the polling interval by at least 5 seconds before retrying."),
        Map.entry("expired_code",
            "The device code has expired. Return to the device and request a new code.")
    );

    /**
     * @return a plain-English hint, or {@code null} when the error code isn't recognized.
     */
    public static String hint(String errorCode) {
        if (errorCode == null) return null;
        return HINTS.get(errorCode.toLowerCase(Locale.ROOT).trim());
    }
}
