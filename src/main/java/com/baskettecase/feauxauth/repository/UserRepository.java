package com.baskettecase.feauxauth.repository;

import com.baskettecase.feauxauth.model.OAuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<OAuthUser, UUID> {
    Optional<OAuthUser> findByEmail(String email);
    boolean existsByEmail(String email);
}
