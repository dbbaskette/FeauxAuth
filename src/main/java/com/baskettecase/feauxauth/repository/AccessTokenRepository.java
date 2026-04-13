package com.baskettecase.feauxauth.repository;

import com.baskettecase.feauxauth.model.AccessToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AccessTokenRepository extends JpaRepository<AccessToken, String> {
    Page<AccessToken> findByExpiresAtAfterOrderByCreatedAtDesc(LocalDateTime now, Pageable pageable);
    List<AccessToken> findByUserIdAndRevokedFalse(UUID userId);
    long countByExpiresAtAfterAndRevokedFalse(LocalDateTime now);

    @Modifying
    @Query("UPDATE AccessToken a SET a.revoked = true WHERE a.userId = :userId AND a.revoked = false")
    int revokeAllByUserId(UUID userId);
}
