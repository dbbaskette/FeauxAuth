package com.baskettecase.feauxauth.controller;

import com.baskettecase.feauxauth.config.AppConfig;
import com.baskettecase.feauxauth.model.DeviceCode;
import com.baskettecase.feauxauth.model.OAuthClient;
import com.baskettecase.feauxauth.service.ClientService;
import com.baskettecase.feauxauth.service.DeviceCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class DeviceAuthorizationController {

    private final ClientService clientService;
    private final DeviceCodeService deviceCodeService;
    private final AppConfig appConfig;

    @PostMapping(value = "/oauth/device_authorization", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deviceAuthorization(
            @RequestParam("client_id") String clientId,
            @RequestParam(value = "scope", required = false) String scope) {

        Optional<OAuthClient> clientOpt = clientService.findByClientId(clientId);
        if (clientOpt.isEmpty() || !clientOpt.get().isEnabled()) {
            return errorResponse("invalid_client", "Unknown or disabled client");
        }

        OAuthClient client = clientOpt.get();
        String effectiveScope = (scope != null && !scope.isBlank()) ? scope : client.getAllowedScopes();

        DeviceCode dc = deviceCodeService.issue(client.getClientId(), effectiveScope);

        String issuer = appConfig.getIssuer();
        String verificationUri = issuer + "/device";
        String formattedUserCode = DeviceCodeService.formatUserCode(dc.getUserCode());
        String verificationUriComplete = UriComponentsBuilder.fromUriString(verificationUri)
                .queryParam("user_code", formattedUserCode)
                .toUriString();

        long expiresIn = Duration.between(LocalDateTime.now(), dc.getExpiresAt()).getSeconds();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("device_code", dc.getDeviceCode());
        response.put("user_code", formattedUserCode);
        response.put("verification_uri", verificationUri);
        response.put("verification_uri_complete", verificationUriComplete);
        response.put("expires_in", expiresIn);
        response.put("interval", dc.getIntervalSeconds());

        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, String>> errorResponse(String error, String description) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("error_description", description);
        return ResponseEntity.badRequest().body(body);
    }
}
