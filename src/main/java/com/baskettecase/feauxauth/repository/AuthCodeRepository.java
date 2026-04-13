package com.baskettecase.feauxauth.repository;

import com.baskettecase.feauxauth.model.AuthCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthCodeRepository extends JpaRepository<AuthCode, String> {
}
