package com.baskettecase.feauxauth.service;

import com.baskettecase.feauxauth.model.AuthCode;
import com.baskettecase.feauxauth.repository.AuthCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthCodeService {

    private final AuthCodeRepository authCodeRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateCode(String clientId, UUID userId, String redirectUri, String scope, String codeChallenge, String nonce) {
        byte[] codeBytes = new byte[32];
        secureRandom.nextBytes(codeBytes);
        String codeValue = Base64.getUrlEncoder().withoutPadding().encodeToString(codeBytes);

        AuthCode authCode = new AuthCode();
        authCode.setCode(codeValue);
        authCode.setClientId(clientId);
        authCode.setUserId(userId);
        authCode.setRedirectUri(redirectUri);
        authCode.setScope(scope);
        authCode.setCodeChallenge(codeChallenge);
        authCode.setNonce(nonce);
        authCode.setExpiresAt(LocalDateTime.now().plusSeconds(120));
        authCode.setUsed(false);

        authCodeRepository.save(authCode);
        return codeValue;
    }

    @org.springframework.transaction.annotation.Transactional
    public Optional<AuthCode> consumeCode(String code) {
        int updated = authCodeRepository.markUsed(code, LocalDateTime.now());
        if (updated == 0) return Optional.empty();

        return authCodeRepository.findById(code);
    }
}
