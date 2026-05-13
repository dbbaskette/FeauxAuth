package com.baskettecase.feauxauth.controller.api;

import com.baskettecase.feauxauth.model.AccessToken;
import com.baskettecase.feauxauth.repository.AccessTokenRepository;
import com.baskettecase.feauxauth.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/tokens")
@RequiredArgsConstructor
public class AdminTokenApi {

    private final AccessTokenRepository accessTokenRepository;
    private final TokenService tokenService;

    @GetMapping
    public Page<AccessToken> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return accessTokenRepository.findByExpiresAtAfterOrderByCreatedAtDesc(
                LocalDateTime.now(), PageRequest.of(page, size));
    }

    @GetMapping("/{jti}")
    public ResponseEntity<AccessToken> findOne(@PathVariable String jti) {
        return accessTokenRepository.findById(jti)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{jti}/revoke")
    public ResponseEntity<Void> revoke(@PathVariable String jti) {
        tokenService.revokeToken(jti);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/revoke-user/{userId}")
    public ResponseEntity<Void> revokeAllForUser(@PathVariable UUID userId) {
        tokenService.revokeAllForUser(userId);
        return ResponseEntity.ok().build();
    }
}
