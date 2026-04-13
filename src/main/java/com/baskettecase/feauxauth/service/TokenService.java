package com.baskettecase.feauxauth.service;

import com.baskettecase.feauxauth.model.AccessToken;
import com.baskettecase.feauxauth.model.OAuthClient;
import com.baskettecase.feauxauth.model.OAuthUser;
import com.baskettecase.feauxauth.model.RefreshToken;
import com.baskettecase.feauxauth.repository.AccessTokenRepository;
import com.baskettecase.feauxauth.repository.RefreshTokenRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
@Slf4j
public class TokenService {

    private final KeyService keyService;
    private final AccessTokenRepository accessTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final String issuer;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenService(KeyService keyService,
                        AccessTokenRepository accessTokenRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        @Value("${feauxauth.issuer}") String issuer) {
        this.keyService = keyService;
        this.accessTokenRepository = accessTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.issuer = issuer;
    }

    public String mintAccessToken(OAuthUser user, OAuthClient client, String scope) {
        String jti = UUID.randomUUID().toString();
        Date now = new Date();
        Date expiry = new Date(now.getTime() + (long) client.getAccessTokenTtl() * 1000);

        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(user.getEmail())
                .audience(client.getClientId())
                .expirationTime(expiry)
                .issueTime(now)
                .jwtID(jti)
                .claim("scope", scope);

        if (scope.contains("email") || scope.contains("openid")) {
            claimsBuilder.claim("email", user.getEmail());
        }
        if (scope.contains("profile")) {
            claimsBuilder.claim("name", user.getDisplayName());
        }

        String jwt = signJwt(claimsBuilder.build());

        AccessToken accessToken = new AccessToken();
        accessToken.setJti(jti);
        accessToken.setClientId(client.getClientId());
        accessToken.setUserId(user.getId());
        accessToken.setScope(scope);
        accessToken.setExpiresAt(LocalDateTime.ofInstant(expiry.toInstant(), ZoneOffset.UTC));
        accessToken.setCreatedAt(LocalDateTime.now());
        accessTokenRepository.save(accessToken);

        return jwt;
    }

    public String mintIdToken(OAuthUser user, OAuthClient client, String scope, String nonce) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + (long) client.getAccessTokenTtl() * 1000);

        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(user.getEmail())
                .audience(client.getClientId())
                .expirationTime(expiry)
                .issueTime(now)
                .claim("email", user.getEmail())
                .claim("name", user.getDisplayName());

        if (nonce != null && !nonce.isBlank()) {
            claimsBuilder.claim("nonce", nonce);
        }

        return signJwt(claimsBuilder.build());
    }

    private String signJwt(JWTClaimsSet claims) {
        try {
            RSAKey rsaKey = keyService.getActiveRSAKey();
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(rsaKey.getKeyID())
                    .type(JOSEObjectType.JWT)
                    .build();
            SignedJWT signedJWT = new SignedJWT(header, claims);
            signedJWT.sign(keyService.getSigner());
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to sign JWT", e);
        }
    }

    public String mintRefreshToken(OAuthUser user, OAuthClient client, String scope) {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String tokenValue = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(tokenValue);
        refreshToken.setClientId(client.getClientId());
        refreshToken.setUserId(user.getId());
        refreshToken.setScope(scope);
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(client.getRefreshTokenTtl()));
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);

        return tokenValue;
    }

    public Optional<JWTClaimsSet> verifyAccessToken(String jwt) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(jwt);
            RSAKey publicKey = keyService.getPublicRSAKey();
            if (!signedJWT.verify(new RSASSAVerifier(publicKey))) {
                return Optional.empty();
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            if (claims.getExpirationTime().before(new Date())) {
                return Optional.empty();
            }

            String jti = claims.getJWTID();
            Optional<AccessToken> tokenRecord = accessTokenRepository.findById(jti);
            if (tokenRecord.isPresent() && tokenRecord.get().isRevoked()) {
                return Optional.empty();
            }

            return Optional.of(claims);
        } catch (Exception e) {
            log.debug("Token verification failed", e);
            return Optional.empty();
        }
    }

    public boolean revokeToken(String tokenValue) {
        Optional<AccessToken> accessToken = accessTokenRepository.findById(tokenValue);
        if (accessToken.isPresent()) {
            accessToken.get().setRevoked(true);
            accessTokenRepository.save(accessToken.get());
            return true;
        }

        try {
            SignedJWT signedJWT = SignedJWT.parse(tokenValue);
            String jti = signedJWT.getJWTClaimsSet().getJWTID();
            Optional<AccessToken> byJti = accessTokenRepository.findById(jti);
            if (byJti.isPresent()) {
                byJti.get().setRevoked(true);
                accessTokenRepository.save(byJti.get());
                return true;
            }
        } catch (Exception ignored) {
        }

        Optional<RefreshToken> refreshToken = refreshTokenRepository.findById(tokenValue);
        if (refreshToken.isPresent()) {
            refreshToken.get().setRevoked(true);
            refreshTokenRepository.save(refreshToken.get());
            return true;
        }

        return false;
    }

    @org.springframework.transaction.annotation.Transactional
    public void revokeAllForUser(UUID userId) {
        accessTokenRepository.revokeAllByUserId(userId);
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}
