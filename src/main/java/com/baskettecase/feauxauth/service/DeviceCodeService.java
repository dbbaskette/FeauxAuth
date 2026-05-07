package com.baskettecase.feauxauth.service;

import com.baskettecase.feauxauth.model.DeviceCode;
import com.baskettecase.feauxauth.repository.DeviceCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceCodeService {

    static final String USER_CODE_ALPHABET = "BCDFGHJKLMNPQRSTVWXZ";
    static final int USER_CODE_LENGTH = 8;
    static final int DEFAULT_TTL_SECONDS = 600;
    static final int DEFAULT_INTERVAL_SECONDS = 5;

    private final DeviceCodeRepository deviceCodeRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public DeviceCode issue(String clientId, String scope) {
        byte[] codeBytes = new byte[32];
        secureRandom.nextBytes(codeBytes);
        String deviceCodeValue = Base64.getUrlEncoder().withoutPadding().encodeToString(codeBytes);

        DeviceCode dc = new DeviceCode();
        dc.setDeviceCode(deviceCodeValue);
        dc.setUserCode(generateUniqueUserCode());
        dc.setClientId(clientId);
        dc.setScope(scope);
        dc.setStatus(DeviceCode.STATUS_PENDING);
        dc.setExpiresAt(LocalDateTime.now().plusSeconds(DEFAULT_TTL_SECONDS));
        dc.setIntervalSeconds(DEFAULT_INTERVAL_SECONDS);
        dc.setCreatedAt(LocalDateTime.now());

        return deviceCodeRepository.save(dc);
    }

    public Optional<DeviceCode> findByDeviceCode(String deviceCode) {
        return deviceCodeRepository.findById(deviceCode);
    }

    public Optional<DeviceCode> findByUserCode(String userCode) {
        return deviceCodeRepository.findByUserCode(normalizeUserCode(userCode));
    }

    @Transactional
    public DeviceCode approve(DeviceCode dc, UUID userId) {
        dc.setStatus(DeviceCode.STATUS_APPROVED);
        dc.setUserId(userId);
        return deviceCodeRepository.save(dc);
    }

    @Transactional
    public DeviceCode deny(DeviceCode dc) {
        dc.setStatus(DeviceCode.STATUS_DENIED);
        return deviceCodeRepository.save(dc);
    }

    @Transactional
    public DeviceCode markConsumed(DeviceCode dc) {
        dc.setStatus(DeviceCode.STATUS_CONSUMED);
        return deviceCodeRepository.save(dc);
    }

    @Transactional
    public DeviceCode recordPoll(DeviceCode dc) {
        dc.setLastPolledAt(LocalDateTime.now());
        return deviceCodeRepository.save(dc);
    }

    public static String normalizeUserCode(String userCode) {
        if (userCode == null) return "";
        return userCode.replace("-", "").replace(" ", "").toUpperCase();
    }

    public static String formatUserCode(String userCode) {
        String normalized = normalizeUserCode(userCode);
        if (normalized.length() == USER_CODE_LENGTH) {
            return normalized.substring(0, 4) + "-" + normalized.substring(4);
        }
        return normalized;
    }

    private String generateUniqueUserCode() {
        // Collisions are vanishingly rare (~37 bits of entropy in an 8-char code over 20 letters),
        // but a bounded retry loop costs nothing and makes the code race-safe under load.
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = randomUserCode();
            if (deviceCodeRepository.findByUserCode(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique user_code after 5 attempts");
    }

    private String randomUserCode() {
        StringBuilder sb = new StringBuilder(USER_CODE_LENGTH);
        for (int i = 0; i < USER_CODE_LENGTH; i++) {
            sb.append(USER_CODE_ALPHABET.charAt(secureRandom.nextInt(USER_CODE_ALPHABET.length())));
        }
        return sb.toString();
    }
}
