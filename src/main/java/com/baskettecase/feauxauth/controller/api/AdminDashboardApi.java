package com.baskettecase.feauxauth.controller.api;

import com.baskettecase.feauxauth.repository.AccessTokenRepository;
import com.baskettecase.feauxauth.repository.ClientRepository;
import com.baskettecase.feauxauth.repository.UserRepository;
import com.baskettecase.feauxauth.service.KeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardApi {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final AccessTokenRepository accessTokenRepository;
    private final KeyService keyService;

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalClients", clientRepository.count());
        stats.put("totalUsers", userRepository.count());
        stats.put("activeTokens", accessTokenRepository.countByExpiresAtAfterAndRevokedFalse(LocalDateTime.now()));
        stats.put("signingKeyId", keyService.getActiveKey().getKid());
        stats.put("recentTokens", accessTokenRepository.findByExpiresAtAfterOrderByCreatedAtDesc(
                LocalDateTime.now().minusDays(7), PageRequest.of(0, 10)).getContent());
        return stats;
    }
}
