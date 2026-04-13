package com.baskettecase.feauxauth.controller;

import com.baskettecase.feauxauth.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RevocationController {

    private final TokenService tokenService;

    @PostMapping("/oauth/revoke")
    public ResponseEntity<Void> revoke(@RequestParam("token") String token) {
        tokenService.revokeToken(token);
        return ResponseEntity.ok().build();
    }
}
