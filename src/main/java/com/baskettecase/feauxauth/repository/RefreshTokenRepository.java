package com.baskettecase.feauxauth.repository;

import com.baskettecase.feauxauth.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    List<RefreshToken> findByUserIdAndRevokedFalse(UUID userId);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.userId = :userId AND r.revoked = false")
    int revokeAllByUserId(UUID userId);
}
