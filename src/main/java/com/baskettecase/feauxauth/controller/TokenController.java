package com.baskettecase.feauxauth.controller;

import com.baskettecase.feauxauth.model.AuthCode;
import com.baskettecase.feauxauth.model.OAuthClient;
import com.baskettecase.feauxauth.model.OAuthUser;
import com.baskettecase.feauxauth.model.RefreshToken;
import com.baskettecase.feauxauth.repository.RefreshTokenRepository;
import com.baskettecase.feauxauth.repository.UserRepository;
import com.baskettecase.feauxauth.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class TokenController {

    private static final String GRANT_AUTHORIZATION_CODE = "authorization_code";
    private static final String GRANT_REFRESH_TOKEN = "refresh_token";
    private static final String GRANT_CLIENT_CREDENTIALS = "client_credentials";

    private final AuthCodeService authCodeService;
    private final TokenService tokenService;
    private final ClientService clientService;
    private final PkceService pkceService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @PostMapping(value = "/oauth/token", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> token(
            @RequestParam("grant_type") String grantType,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @RequestParam("client_id") String clientId,
            @RequestParam(value = "client_secret", required = false) String clientSecret,
            @RequestParam(value = "code_verifier", required = false) String codeVerifier,
            @RequestParam(value = "refresh_token", required = false) String refreshTokenValue,
            @RequestParam(value = "scope", required = false) String scope) {

        Optional<OAuthClient> clientOpt = clientService.findByClientId(clientId);
        if (clientOpt.isEmpty() || !clientOpt.get().isEnabled()) {
            return errorResponse("invalid_client", "Unknown or disabled client");
        }
        OAuthClient client = clientOpt.get();

        // Client credentials and confidential clients always require client_secret
        boolean secretRequired = GRANT_CLIENT_CREDENTIALS.equals(grantType) || !client.isRequirePkce();
        if (secretRequired) {
            if (clientSecret == null || !clientService.verifySecret(client, clientSecret)) {
                return errorResponse("invalid_client", "Invalid client credentials");
            }
        }

        if (GRANT_AUTHORIZATION_CODE.equals(grantType)) {
            return handleAuthorizationCode(client, code, redirectUri, codeVerifier);
        } else if (GRANT_REFRESH_TOKEN.equals(grantType)) {
            return handleRefreshToken(client, refreshTokenValue);
        } else if (GRANT_CLIENT_CREDENTIALS.equals(grantType)) {
            return handleClientCredentials(client, scope);
        } else {
            return errorResponse("unsupported_grant_type", "Supported: authorization_code, refresh_token, client_credentials");
        }
    }

    private ResponseEntity<?> handleAuthorizationCode(OAuthClient client, String code, String redirectUri, String codeVerifier) {
        if (code == null || code.isBlank()) {
            return errorResponse("invalid_request", "Missing code parameter");
        }

        Optional<AuthCode> authCodeOpt = authCodeService.consumeCode(code);
        if (authCodeOpt.isEmpty()) {
            return errorResponse("invalid_grant", "Invalid, expired, or already-used authorization code");
        }

        AuthCode authCode = authCodeOpt.get();

        if (!authCode.getClientId().equals(client.getClientId())) {
            return errorResponse("invalid_grant", "Code was not issued to this client");
        }

        if (!authCode.getRedirectUri().equals(redirectUri)) {
            return errorResponse("invalid_grant", "redirect_uri mismatch");
        }

        if (authCode.getCodeChallenge() != null) {
            if (codeVerifier == null || !pkceService.verifyChallenge(codeVerifier, authCode.getCodeChallenge())) {
                return errorResponse("invalid_grant", "PKCE verification failed");
            }
        }

        Optional<OAuthUser> userOpt = userRepository.findById(authCode.getUserId());
        if (userOpt.isEmpty() || !userOpt.get().isEnabled()) {
            return errorResponse("invalid_grant", "User not found or disabled");
        }
        OAuthUser user = userOpt.get();

        return ResponseEntity.ok(buildTokenResponse(user, client, authCode.getScope(), authCode.getNonce(), null));
    }

    private ResponseEntity<?> handleRefreshToken(OAuthClient client, String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            return errorResponse("invalid_request", "Missing refresh_token parameter");
        }

        Optional<RefreshToken> rtOpt = refreshTokenRepository.findById(refreshTokenValue);
        if (rtOpt.isEmpty()) {
            return errorResponse("invalid_grant", "Invalid refresh token");
        }

        RefreshToken rt = rtOpt.get();

        if (rt.isRevoked()) {
            return errorResponse("invalid_grant", "Refresh token has been revoked");
        }

        if (rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            return errorResponse("invalid_grant", "Refresh token has expired");
        }

        if (!rt.getClientId().equals(client.getClientId())) {
            return errorResponse("invalid_grant", "Refresh token was not issued to this client");
        }

        Optional<OAuthUser> userOpt = userRepository.findById(rt.getUserId());
        if (userOpt.isEmpty() || !userOpt.get().isEnabled()) {
            return errorResponse("invalid_grant", "User not found or disabled");
        }
        OAuthUser user = userOpt.get();

        Map<String, Object> response = buildTokenResponse(user, client, rt.getScope(), null, refreshTokenValue);

        return ResponseEntity.ok(response);
    }

    private ResponseEntity<?> handleClientCredentials(OAuthClient client, String scope) {
        String effectiveScope = (scope != null && !scope.isBlank()) ? scope : client.getAllowedScopes();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("access_token", tokenService.mintClientAccessToken(client, effectiveScope));
        response.put("token_type", "Bearer");
        response.put("expires_in", client.getAccessTokenTtl());

        return ResponseEntity.ok(response);
    }

    private Map<String, Object> buildTokenResponse(OAuthUser user, OAuthClient client, String scope, String nonce, String existingRefreshToken) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("access_token", tokenService.mintAccessToken(user, client, scope));
        response.put("token_type", "Bearer");
        response.put("expires_in", client.getAccessTokenTtl());

        if (existingRefreshToken != null) {
            response.put("refresh_token", existingRefreshToken);
        } else if (client.getRefreshTokenTtl() > 0) {
            response.put("refresh_token", tokenService.mintRefreshToken(user, client, scope));
        }

        if (scope.contains("openid")) {
            response.put("id_token", tokenService.mintIdToken(user, client, scope, nonce));
        }

        return response;
    }

    private ResponseEntity<Map<String, String>> errorResponse(String error, String description) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("error_description", description);
        return ResponseEntity.badRequest().body(body);
    }
}
