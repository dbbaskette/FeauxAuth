package com.baskettecase.feauxauth.service;

import com.baskettecase.feauxauth.model.SigningKey;
import com.baskettecase.feauxauth.repository.SigningKeyRepository;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeyService {

    private final SigningKeyRepository signingKeyRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeKeyPair() {
        if (signingKeyRepository.findByActiveTrue().isPresent()) {
            log.info("Active signing key found");
            return;
        }

        log.info("No active signing key found — generating RSA-2048 keypair");
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();

            String kid = UUID.randomUUID().toString().substring(0, 16);

            SigningKey signingKey = new SigningKey();
            signingKey.setKid(kid);
            signingKey.setPrivateKey(encodePem(keyPair.getPrivate().getEncoded(), "PRIVATE KEY"));
            signingKey.setPublicKey(encodePem(keyPair.getPublic().getEncoded(), "PUBLIC KEY"));
            signingKey.setCreatedAt(LocalDateTime.now());
            signingKey.setActive(true);

            signingKeyRepository.save(signingKey);
            log.info("Generated signing key with kid={}", kid);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate RSA keypair", e);
        }
    }

    public SigningKey getActiveKey() {
        return signingKeyRepository.findByActiveTrue()
                .orElseThrow(() -> new IllegalStateException("No active signing key"));
    }

    public RSAKey getActiveRSAKey() {
        SigningKey key = getActiveKey();
        try {
            java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");

            byte[] pubBytes = decodePem(key.getPublicKey());
            RSAPublicKey publicKey = (RSAPublicKey) kf.generatePublic(
                    new java.security.spec.X509EncodedKeySpec(pubBytes));

            byte[] privBytes = decodePem(key.getPrivateKey());
            RSAPrivateKey privateKey = (RSAPrivateKey) kf.generatePrivate(
                    new java.security.spec.PKCS8EncodedKeySpec(privBytes));

            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(key.getKid())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load RSA key", e);
        }
    }

    public RSAKey getPublicRSAKey() {
        SigningKey key = getActiveKey();
        try {
            java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
            byte[] pubBytes = decodePem(key.getPublicKey());
            RSAPublicKey publicKey = (RSAPublicKey) kf.generatePublic(
                    new java.security.spec.X509EncodedKeySpec(pubBytes));

            return new RSAKey.Builder(publicKey)
                    .keyID(key.getKid())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load public RSA key", e);
        }
    }

    private String encodePem(byte[] keyBytes, String type) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(keyBytes);
        return "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----";
    }

    private byte[] decodePem(String pem) {
        String base64 = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }
}
