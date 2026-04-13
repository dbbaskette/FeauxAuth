package com.baskettecase.feauxauth.repository;

import com.baskettecase.feauxauth.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    List<RefreshToken> findByUserIdAndRevokedFalse(UUID userId);
}
