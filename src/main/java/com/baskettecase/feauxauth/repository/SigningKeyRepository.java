package com.baskettecase.feauxauth.repository;

import com.baskettecase.feauxauth.model.SigningKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SigningKeyRepository extends JpaRepository<SigningKey, String> {
    Optional<SigningKey> findByActiveTrue();
}
