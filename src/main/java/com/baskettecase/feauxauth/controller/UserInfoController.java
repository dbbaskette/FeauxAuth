package com.baskettecase.feauxauth.controller;

import com.baskettecase.feauxauth.model.OAuthUser;
import com.baskettecase.feauxauth.service.TokenService;
import com.baskettecase.feauxauth.service.UserService;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class UserInfoController {

    private final TokenService tokenService;
    private final UserService userService;

    @GetMapping(value = "/oauth/userinfo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> userInfo(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_token", "error_description", "Missing or invalid Bearer token"));
        }

        String token = authHeader.substring(7);
        Optional<JWTClaimsSet> claimsOpt = tokenService.verifyAccessToken(token);
        if (claimsOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_token", "error_description", "Token is invalid, expired, or revoked"));
        }

        JWTClaimsSet claims = claimsOpt.get();
        String email = claims.getSubject();
        String scope = (String) claims.getClaim("scope");

        Optional<OAuthUser> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty() || !userOpt.get().isEnabled()) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_token", "error_description", "User not found or disabled"));
        }

        OAuthUser user = userOpt.get();

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("sub", user.getEmail());

        if (scope != null && (scope.contains("email") || scope.contains("openid"))) {
            profile.put("email", user.getEmail());
        }
        if (scope != null && scope.contains("profile")) {
            profile.put("name", user.getDisplayName());
        }

        if (user.getRoles() != null && !user.getRoles().isBlank()) {
            profile.put("roles", java.util.Arrays.stream(user.getRoles().split(","))
                    .map(String::trim).filter(r -> !r.isEmpty()).toList());
        }

        return ResponseEntity.ok(profile);
    }
}
