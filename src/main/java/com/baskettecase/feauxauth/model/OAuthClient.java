package com.baskettecase.feauxauth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "oauth_clients")
@Getter @Setter
public class OAuthClient {
    @Id
    private UUID id;

    @Column(name = "client_id", nullable = false, unique = true)
    private String clientId;

    @Column(name = "client_secret_hash", nullable = false)
    private String clientSecretHash;

    @Column(nullable = false)
    private String name;

    @Column(name = "redirect_uris", nullable = false, columnDefinition = "TEXT")
    private String redirectUris;

    @Column(name = "allowed_scopes", nullable = false)
    private String allowedScopes = "openid profile email";

    @Column(name = "access_token_ttl", nullable = false)
    private int accessTokenTtl = 3600;

    @Column(name = "refresh_token_ttl", nullable = false)
    private int refreshTokenTtl = 2592000;

    @Column(name = "require_pkce", nullable = false)
    private boolean requirePkce = false;

    @Column(name = "require_consent", nullable = false)
    private boolean requireConsent = false;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(length = 1024)
    private String roles = "";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
