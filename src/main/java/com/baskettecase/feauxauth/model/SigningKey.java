package com.baskettecase.feauxauth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "signing_keys")
@Getter @Setter
public class SigningKey {
    @Id
    private String kid;

    @Column(name = "private_key", nullable = false, columnDefinition = "TEXT")
    private String privateKey;

    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
    private String publicKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private boolean active = true;
}
