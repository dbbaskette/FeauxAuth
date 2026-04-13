package com.baskettecase.feauxauth.service;

import com.baskettecase.feauxauth.model.AccessToken;
import com.baskettecase.feauxauth.model.OAuthClient;
import com.baskettecase.feauxauth.model.OAuthUser;
import com.baskettecase.feauxauth.model.RefreshToken;
import com.baskettecase.feauxauth.repository.AccessTokenRepository;
import com.baskettecase.feauxauth.repository.RefreshTokenRepository;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock private KeyService keyService;
    @Mock private AccessTokenRepository accessTokenRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    private TokenService tokenService;
    private RSAKey rsaKey;

    @BeforeEach
    void setUp() throws Exception {
        rsaKey = new RSAKeyGenerator(2048).keyID("test-kid").generate();
        tokenService = new TokenService(keyService, accessTokenRepository, refreshTokenRepository, "http://localhost:8080");
    }

    @Test
    void mintAccessToken_returnsValidJwt() throws Exception {
        when(keyService.getActiveRSAKey()).thenReturn(rsaKey);
        when(accessTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        OAuthUser user = new OAuthUser();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setDisplayName("Test User");

        OAuthClient client = new OAuthClient();
        client.setClientId("my-app");
        client.setAccessTokenTtl(3600);

        String jwt = tokenService.mintAccessToken(user, client, "openid email");

        SignedJWT parsed = SignedJWT.parse(jwt);
        assertThat(parsed.getJWTClaimsSet().getIssuer()).isEqualTo("http://localhost:8080");
        assertThat(parsed.getJWTClaimsSet().getSubject()).isEqualTo("test@example.com");
        assertThat(parsed.getJWTClaimsSet().getAudience()).contains("my-app");
        assertThat(parsed.getJWTClaimsSet().getClaim("email")).isEqualTo("test@example.com");
        assertThat(parsed.getJWTClaimsSet().getClaim("scope")).isEqualTo("openid email");
    }

    @Test
    void mintRefreshToken_returns256BitToken() throws Exception {
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        OAuthUser user = new OAuthUser();
        user.setId(UUID.randomUUID());

        OAuthClient client = new OAuthClient();
        client.setClientId("my-app");
        client.setRefreshTokenTtl(2592000);

        String token = tokenService.mintRefreshToken(user, client, "openid");

        assertThat(token).isNotBlank();
        assertThat(token.length()).isGreaterThanOrEqualTo(32);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getScope()).isEqualTo("openid");
    }

    @Test
    void verifyAccessToken_validToken_returnsClaims() throws Exception {
        when(keyService.getActiveRSAKey()).thenReturn(rsaKey);
        when(keyService.getPublicRSAKey()).thenReturn(rsaKey.toPublicJWK());
        when(accessTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(accessTokenRepository.findById(any())).thenAnswer(i -> {
            AccessToken at = new AccessToken();
            at.setJti(i.getArgument(0));
            at.setRevoked(false);
            return Optional.of(at);
        });

        OAuthUser user = new OAuthUser();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setDisplayName("Test User");

        OAuthClient client = new OAuthClient();
        client.setClientId("my-app");
        client.setAccessTokenTtl(3600);

        String jwt = tokenService.mintAccessToken(user, client, "openid");
        var claims = tokenService.verifyAccessToken(jwt);

        assertThat(claims).isPresent();
        assertThat(claims.get().getSubject()).isEqualTo("test@example.com");
    }

    @Test
    void verifyAccessToken_revokedToken_returnsEmpty() throws Exception {
        when(keyService.getActiveRSAKey()).thenReturn(rsaKey);
        when(keyService.getPublicRSAKey()).thenReturn(rsaKey.toPublicJWK());
        when(accessTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(accessTokenRepository.findById(any())).thenAnswer(i -> {
            AccessToken at = new AccessToken();
            at.setJti(i.getArgument(0));
            at.setRevoked(true);
            return Optional.of(at);
        });

        OAuthUser user = new OAuthUser();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setDisplayName("Test User");

        OAuthClient client = new OAuthClient();
        client.setClientId("my-app");
        client.setAccessTokenTtl(3600);

        String jwt = tokenService.mintAccessToken(user, client, "openid");
        var claims = tokenService.verifyAccessToken(jwt);

        assertThat(claims).isEmpty();
    }
}
