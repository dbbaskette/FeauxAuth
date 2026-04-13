package com.baskettecase.feauxauth.repository;

import com.baskettecase.feauxauth.model.AuthCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface AuthCodeRepository extends JpaRepository<AuthCode, String> {

    @Modifying
    @Query("UPDATE AuthCode a SET a.used = true WHERE a.code = :code AND a.used = false AND a.expiresAt > :now")
    int markUsed(String code, LocalDateTime now);
}
