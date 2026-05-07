package com.baskettecase.feauxauth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "device_codes")
@Getter @Setter
public class DeviceCode {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_DENIED = "DENIED";
    public static final String STATUS_CONSUMED = "CONSUMED";

    @Id
    @Column(name = "device_code")
    private String deviceCode;

    @Column(name = "user_code", nullable = false, unique = true, length = 20)
    private String userCode;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(nullable = false, length = 1024)
    private String scope;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "interval_seconds", nullable = false)
    private int intervalSeconds = 5;

    @Column(name = "last_polled_at")
    private LocalDateTime lastPolledAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
