package com.baskettecase.feauxauth.controller.api;

import com.baskettecase.feauxauth.model.OAuthClient;
import com.baskettecase.feauxauth.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/clients")
@RequiredArgsConstructor
public class AdminClientApi {

    private final ClientService clientService;

    @GetMapping
    public List<OAuthClient> list() {
        return clientService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OAuthClient> get(@PathVariable UUID id) {
        return clientService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String clientId = (String) body.get("clientId");
        String redirectUris = (String) body.get("redirectUris");
        String allowedScopes = (String) body.getOrDefault("allowedScopes", "openid profile email");
        int accessTokenTtl = ((Number) body.getOrDefault("accessTokenTtl", 3600)).intValue();
        int refreshTokenTtl = ((Number) body.getOrDefault("refreshTokenTtl", 2592000)).intValue();
        boolean requirePkce = (Boolean) body.getOrDefault("requirePkce", false);
        boolean requireConsent = (Boolean) body.getOrDefault("requireConsent", false);
        String roles = (String) body.getOrDefault("roles", "");

        Map<String, Object> result = clientService.create(name, clientId, redirectUris,
                allowedScopes, accessTokenTtl, refreshTokenTtl, requirePkce, requireConsent, roles);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("client", result.get("client"));
        response.put("clientSecret", result.get("plainSecret"));

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        Optional<OAuthClient> existing = clientService.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        OAuthClient client = existing.get();
        if (body.containsKey("name")) client.setName((String) body.get("name"));
        if (body.containsKey("redirectUris")) client.setRedirectUris((String) body.get("redirectUris"));
        if (body.containsKey("allowedScopes")) client.setAllowedScopes((String) body.get("allowedScopes"));
        if (body.containsKey("accessTokenTtl")) client.setAccessTokenTtl(((Number) body.get("accessTokenTtl")).intValue());
        if (body.containsKey("refreshTokenTtl")) client.setRefreshTokenTtl(((Number) body.get("refreshTokenTtl")).intValue());
        if (body.containsKey("requirePkce")) client.setRequirePkce((Boolean) body.get("requirePkce"));
        if (body.containsKey("requireConsent")) client.setRequireConsent((Boolean) body.get("requireConsent"));
        if (body.containsKey("enabled")) client.setEnabled((Boolean) body.get("enabled"));
        if (body.containsKey("roles")) client.setRoles((String) body.get("roles"));

        return ResponseEntity.ok(clientService.update(client));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reset-secret")
    public ResponseEntity<?> resetSecret(@PathVariable UUID id) {
        String newSecret = clientService.resetSecret(id);
        return ResponseEntity.ok(Map.of("clientSecret", newSecret));
    }
}
