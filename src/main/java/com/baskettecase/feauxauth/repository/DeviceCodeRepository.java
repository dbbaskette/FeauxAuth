package com.baskettecase.feauxauth.repository;

import com.baskettecase.feauxauth.model.DeviceCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceCodeRepository extends JpaRepository<DeviceCode, String> {

    Optional<DeviceCode> findByUserCode(String userCode);
}
