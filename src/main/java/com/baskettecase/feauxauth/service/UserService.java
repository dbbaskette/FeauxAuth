package com.baskettecase.feauxauth.service;

import com.baskettecase.feauxauth.model.OAuthUser;
import com.baskettecase.feauxauth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    public List<OAuthUser> findAll() {
        return userRepository.findAll();
    }

    public Optional<OAuthUser> findById(UUID id) {
        return userRepository.findById(id);
    }

    public Optional<OAuthUser> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public OAuthUser create(String email, String displayName, String password, String roles) {
        OAuthUser user = new OAuthUser();
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRoles(roles != null ? roles : "");
        return userRepository.save(user);
    }

    public OAuthUser update(OAuthUser user) {
        return userRepository.save(user);
    }

    public void delete(UUID id) {
        userRepository.deleteById(id);
    }

    public String resetPassword(UUID id) {
        OAuthUser user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        String newPassword = generatePassword();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return newPassword;
    }

    public boolean verifyPassword(OAuthUser user, String password) {
        return passwordEncoder.matches(password, user.getPasswordHash());
    }

    public void recordLogin(OAuthUser user) {
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private String generatePassword() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
