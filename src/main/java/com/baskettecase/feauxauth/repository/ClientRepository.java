package com.baskettecase.feauxauth.repository;

import com.baskettecase.feauxauth.model.OAuthClient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<OAuthClient, UUID> {
    Optional<OAuthClient> findByClientId(String clientId);
    boolean existsByClientId(String clientId);
}
