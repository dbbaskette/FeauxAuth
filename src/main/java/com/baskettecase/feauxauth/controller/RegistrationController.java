package com.baskettecase.feauxauth.controller;

import com.baskettecase.feauxauth.model.OAuthClient;
import com.baskettecase.feauxauth.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequiredArgsConstructor
public class RegistrationController {

    private final ClientService clientService;

    @SuppressWarnings("unchecked")
    @PostMapping(value = "/oauth/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> register(@RequestBody Map<String, Object> metadata) {
        String clientName = (String) metadata.get("client_name");
        if (clientName == null || clientName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_client_metadata", "error_description", "client_name is required"));
        }

        Object redirectUrisObj = metadata.get("redirect_uris");
        if (redirectUrisObj == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_client_metadata", "error_description", "redirect_uris is required"));
        }

        String redirectUris;
        if (redirectUrisObj instanceof List<?> list) {
            redirectUris = String.join("\n", list.stream().map(Object::toString).toList());
        } else {
            redirectUris = redirectUrisObj.toString();
        }

        String scope = (String) metadata.getOrDefault("scope", "openid profile email");

        List<String> grantTypes = metadata.containsKey("grant_types")
                ? ((List<String>) metadata.get("grant_types"))
                : List.of("authorization_code");
        boolean requirePkce = grantTypes.contains("authorization_code")
                && "none".equals(metadata.getOrDefault("token_endpoint_auth_method", "client_secret_post"));

        // Generate a URL-safe client_id from the name
        String clientId = clientName.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "")
                + "-" + UUID.randomUUID().toString().substring(0, 8);

        Map<String, Object> result = clientService.create(
                clientName, clientId, redirectUris, scope,
                3600, 2592000, requirePkce, false, "");

        OAuthClient client = (OAuthClient) result.get("client");
        String plainSecret = (String) result.get("plainSecret");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("client_id", client.getClientId());
        response.put("client_secret", plainSecret);
        response.put("client_secret_expires_at", 0);
        response.put("client_name", client.getName());
        response.put("redirect_uris", Arrays.asList(client.getRedirectUris().split("\\n")));
        response.put("grant_types", grantTypes);
        response.put("token_endpoint_auth_method", requirePkce ? "none" : "client_secret_post");
        response.put("scope", client.getAllowedScopes());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
