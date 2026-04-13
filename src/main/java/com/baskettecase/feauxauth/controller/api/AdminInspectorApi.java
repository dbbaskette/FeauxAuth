package com.baskettecase.feauxauth.controller.api;

import com.baskettecase.feauxauth.model.AccessToken;
import com.baskettecase.feauxauth.repository.AccessTokenRepository;
import com.baskettecase.feauxauth.service.KeyService;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/inspector")
@RequiredArgsConstructor
public class AdminInspectorApi {

    private final KeyService keyService;
    private final AccessTokenRepository accessTokenRepository;

    @PostMapping
    public ResponseEntity<?> inspect(@RequestBody Map<String, String> body) {
        String jwt = body.get("token");
        if (jwt == null || jwt.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing token"));
        }

        try {
            SignedJWT signedJWT = SignedJWT.parse(jwt);

            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", signedJWT.getHeader().getAlgorithm().getName());
            header.put("kid", signedJWT.getHeader().getKeyID());
            header.put("typ", signedJWT.getHeader().getType() != null ? signedJWT.getHeader().getType().toString() : null);

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            Map<String, Object> payload = claims.toJSONObject();

            boolean signatureValid;
            try {
                signatureValid = signedJWT.verify(new RSASSAVerifier(keyService.getPublicRSAKey()));
            } catch (Exception e) {
                signatureValid = false;
            }

            boolean expired = claims.getExpirationTime() != null && claims.getExpirationTime().before(new Date());

            String revocationStatus = "unknown";
            String jti = claims.getJWTID();
            if (jti != null) {
                Optional<AccessToken> at = accessTokenRepository.findById(jti);
                if (at.isPresent()) {
                    revocationStatus = at.get().isRevoked() ? "revoked" : "active";
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("header", header);
            result.put("payload", payload);
            result.put("signatureValid", signatureValid);
            result.put("expired", expired);
            result.put("revocationStatus", revocationStatus);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid JWT: " + e.getMessage()));
        }
    }
}
