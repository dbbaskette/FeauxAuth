package com.baskettecase.feauxauth.controller;

import com.baskettecase.feauxauth.config.AppConfig;
import com.baskettecase.feauxauth.service.KeyService;
import com.nimbusds.jose.jwk.JWKSet;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class WellKnownController {

    private final AppConfig appConfig;
    private final KeyService keyService;

    @GetMapping(value = "/.well-known/openid-configuration", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> openIdConfiguration() {
        String issuer = appConfig.getIssuer();
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("issuer", issuer);
        config.put("authorization_endpoint", issuer + "/oauth/authorize");
        config.put("token_endpoint", issuer + "/oauth/token");
        config.put("userinfo_endpoint", issuer + "/oauth/userinfo");
        config.put("revocation_endpoint", issuer + "/oauth/revoke");
        config.put("jwks_uri", issuer + "/.well-known/jwks.json");
        config.put("response_types_supported", List.of("code"));
        config.put("end_session_endpoint", issuer + "/oauth/logout");
        config.put("introspection_endpoint", issuer + "/oauth/introspect");
        config.put("registration_endpoint", issuer + "/oauth/register");
        config.put("device_authorization_endpoint", issuer + "/oauth/device_authorization");
        config.put("grant_types_supported", List.of(
                "authorization_code",
                "refresh_token",
                "client_credentials",
                "urn:ietf:params:oauth:grant-type:device_code"));
        config.put("subject_types_supported", List.of("public"));
        config.put("id_token_signing_alg_values_supported", List.of("RS256"));
        config.put("scopes_supported", List.of("openid", "profile", "email", "offline_access"));
        config.put("token_endpoint_auth_methods_supported", List.of("none", "client_secret_post", "client_secret_basic"));
        config.put("code_challenge_methods_supported", List.of("S256"));
        return config;
    }

    @GetMapping(value = "/.well-known/oauth-authorization-server", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> oauthAuthorizationServer() {
        return openIdConfiguration();
    }

    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> jwks() {
        JWKSet jwkSet = new JWKSet(keyService.getPublicRSAKey());
        return jwkSet.toJSONObject();
    }
}
