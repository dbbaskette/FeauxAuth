package com.baskettecase.feauxauth;

import com.baskettecase.feauxauth.model.OAuthClient;
import com.baskettecase.feauxauth.model.OAuthUser;
import com.baskettecase.feauxauth.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ClientRepository clientRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthCodeRepository authCodeRepository;
    @Autowired private AccessTokenRepository accessTokenRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private ObjectMapper objectMapper;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        accessTokenRepository.deleteAll();
        authCodeRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();

        OAuthClient client = new OAuthClient();
        client.setId(UUID.randomUUID());
        client.setClientId("test-app");
        client.setClientSecretHash(encoder.encode("test-secret"));
        client.setName("Test App");
        client.setRedirectUris("http://localhost:3000/callback");
        client.setAllowedScopes("openid profile email");
        clientRepository.save(client);

        OAuthUser user = new OAuthUser();
        user.setId(UUID.randomUUID());
        user.setEmail("user@test.com");
        user.setDisplayName("Test User");
        user.setPasswordHash(encoder.encode("password123"));
        userRepository.save(user);
    }

    @Test
    void discoveryEndpoint_returnsValidConfig() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").exists())
                .andExpect(jsonPath("$.authorization_endpoint").exists())
                .andExpect(jsonPath("$.token_endpoint").exists())
                .andExpect(jsonPath("$.jwks_uri").exists());
    }

    @Test
    void jwksEndpoint_returnsPublicKey() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys").isArray())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"));
    }

    @Test
    void fullAuthCodeFlow() throws Exception {
        // Step 1: GET /oauth/authorize — should render login page
        MvcResult authorizeResult = mockMvc.perform(get("/oauth/authorize")
                        .param("client_id", "test-app")
                        .param("redirect_uri", "http://localhost:3000/callback")
                        .param("response_type", "code")
                        .param("scope", "openid email")
                        .param("state", "xyz"))
                .andExpect(status().isOk())
                .andReturn();

        // Step 2: POST /oauth/authorize — login
        MvcResult loginResult = mockMvc.perform(post("/oauth/authorize")
                        .session((org.springframework.mock.web.MockHttpSession) authorizeResult.getRequest().getSession())
                        .param("email", "user@test.com")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String location = loginResult.getResponse().getRedirectedUrl();
        assertThat(location).startsWith("http://localhost:3000/callback");
        assertThat(location).contains("code=");
        assertThat(location).contains("state=xyz");

        // Extract code
        String code = extractParam(location, "code");

        // Step 3: POST /oauth/token — exchange code
        MvcResult tokenResult = mockMvc.perform(post("/oauth/token")
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("redirect_uri", "http://localhost:3000/callback")
                        .param("client_id", "test-app")
                        .param("client_secret", "test-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.refresh_token").exists())
                .andExpect(jsonPath("$.id_token").exists())
                .andReturn();

        Map<String, Object> tokenResponse = objectMapper.readValue(
                tokenResult.getResponse().getContentAsString(), Map.class);
        String accessToken = (String) tokenResponse.get("access_token");
        String refreshToken = (String) tokenResponse.get("refresh_token");

        // Step 4: GET /oauth/userinfo
        mockMvc.perform(get("/oauth/userinfo")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").value("user@test.com"))
                .andExpect(jsonPath("$.email").value("user@test.com"));

        // Step 5: Refresh token
        mockMvc.perform(post("/oauth/token")
                        .param("grant_type", "refresh_token")
                        .param("refresh_token", refreshToken)
                        .param("client_id", "test-app")
                        .param("client_secret", "test-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists());

        // Step 6: Revoke token
        mockMvc.perform(post("/oauth/revoke")
                        .param("token", accessToken))
                .andExpect(status().isOk());

        // Step 7: Verify revoked token is rejected
        mockMvc.perform(get("/oauth/userinfo")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authCodeFlow_invalidClient_returnsError() throws Exception {
        mockMvc.perform(get("/oauth/authorize")
                        .param("client_id", "nonexistent")
                        .param("redirect_uri", "http://localhost:3000/callback")
                        .param("response_type", "code")
                        .param("scope", "openid"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("invalid_client")));
    }

    @Test
    void authCodeReuse_returnsError() throws Exception {
        // Get auth code
        MvcResult authorizeResult = mockMvc.perform(get("/oauth/authorize")
                        .param("client_id", "test-app")
                        .param("redirect_uri", "http://localhost:3000/callback")
                        .param("response_type", "code")
                        .param("scope", "openid")
                        .param("state", "abc"))
                .andReturn();

        MvcResult loginResult = mockMvc.perform(post("/oauth/authorize")
                        .session((org.springframework.mock.web.MockHttpSession) authorizeResult.getRequest().getSession())
                        .param("email", "user@test.com")
                        .param("password", "password123"))
                .andReturn();

        String code = extractParam(loginResult.getResponse().getRedirectedUrl(), "code");

        // First use — success
        mockMvc.perform(post("/oauth/token")
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("redirect_uri", "http://localhost:3000/callback")
                        .param("client_id", "test-app")
                        .param("client_secret", "test-secret"))
                .andExpect(status().isOk());

        // Second use — fail
        mockMvc.perform(post("/oauth/token")
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("redirect_uri", "http://localhost:3000/callback")
                        .param("client_id", "test-app")
                        .param("client_secret", "test-secret"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
    }

    private String extractParam(String url, String param) {
        String query = url.substring(url.indexOf('?') + 1);
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv[0].equals(param)) return kv[1];
        }
        throw new IllegalArgumentException("Param not found: " + param);
    }
}
