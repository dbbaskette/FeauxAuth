package com.baskettecase.feauxauth.controller;

import com.baskettecase.feauxauth.model.RefreshToken;
import com.baskettecase.feauxauth.repository.RefreshTokenRepository;
import com.baskettecase.feauxauth.service.TokenService;
import com.nimbusds.jwt.JWTClaimsSet;
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
public class IntrospectionController {

    private final TokenService tokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    @PostMapping(value = "/oauth/introspect", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> introspect(
            @RequestParam("token") String token,
            @RequestParam(value = "token_type_hint", required = false) String hint) {

        // Try as access token (JWT)
        Optional<JWTClaimsSet> claimsOpt = tokenService.verifyAccessToken(token);
        if (claimsOpt.isPresent()) {
            JWTClaimsSet claims = claimsOpt.get();
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("active", true);
            response.put("sub", claims.getSubject());
            response.put("client_id", claims.getAudience() != null && !claims.getAudience().isEmpty()
                    ? claims.getAudience().get(0) : null);
            response.put("scope", claims.getClaim("scope"));
            response.put("token_type", "Bearer");
            response.put("exp", claims.getExpirationTime().getTime() / 1000);
            response.put("iat", claims.getIssueTime().getTime() / 1000);
            response.put("iss", claims.getIssuer());
            response.put("jti", claims.getJWTID());
            return ResponseEntity.ok(response);
        }

        // Try as refresh token
        Optional<RefreshToken> rtOpt = refreshTokenRepository.findById(token);
        if (rtOpt.isPresent()) {
            RefreshToken rt = rtOpt.get();
            if (!rt.isRevoked() && rt.getExpiresAt().isAfter(LocalDateTime.now())) {
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("active", true);
                response.put("client_id", rt.getClientId());
                response.put("scope", rt.getScope());
                response.put("token_type", "refresh_token");
                return ResponseEntity.ok(response);
            }
        }

        return ResponseEntity.ok(Map.of("active", false));
    }
}
