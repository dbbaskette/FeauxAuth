package com.baskettecase.feauxauth.service;

import com.baskettecase.feauxauth.model.OAuthClient;
import com.baskettecase.feauxauth.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    public List<OAuthClient> findAll() {
        return clientRepository.findAll();
    }

    public Optional<OAuthClient> findById(UUID id) {
        return clientRepository.findById(id);
    }

    public Optional<OAuthClient> findByClientId(String clientId) {
        return clientRepository.findByClientId(clientId);
    }

    public Map<String, Object> create(String name, String clientId, String redirectUris,
                                       String allowedScopes, int accessTokenTtl, int refreshTokenTtl,
                                       boolean requirePkce, boolean requireConsent, String roles) {
        String plainSecret = generateSecret();

        OAuthClient client = new OAuthClient();
        client.setName(name);
        client.setClientId(clientId);
        client.setClientSecretHash(passwordEncoder.encode(plainSecret));
        client.setRedirectUris(redirectUris);
        client.setAllowedScopes(allowedScopes);
        client.setAccessTokenTtl(accessTokenTtl);
        client.setRefreshTokenTtl(refreshTokenTtl);
        client.setRequirePkce(requirePkce);
        client.setRequireConsent(requireConsent);
        client.setRoles(roles == null ? "" : roles);

        clientRepository.save(client);

        Map<String, Object> result = new HashMap<>();
        result.put("client", client);
        result.put("plainSecret", plainSecret);
        return result;
    }

    public OAuthClient update(OAuthClient client) {
        return clientRepository.save(client);
    }

    public void delete(UUID id) {
        clientRepository.deleteById(id);
    }

    public String resetSecret(UUID id) {
        OAuthClient client = clientRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Client not found"));
        String plainSecret = generateSecret();
        client.setClientSecretHash(passwordEncoder.encode(plainSecret));
        clientRepository.save(client);
        return plainSecret;
    }

    public boolean verifySecret(OAuthClient client, String plainSecret) {
        return passwordEncoder.matches(plainSecret, client.getClientSecretHash());
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
