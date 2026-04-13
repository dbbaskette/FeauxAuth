package com.baskettecase.feauxauth.controller.api;

import com.baskettecase.feauxauth.model.OAuthUser;
import com.baskettecase.feauxauth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserApi {

    private final UserService userService;

    @GetMapping
    public List<OAuthUser> list() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OAuthUser> get(@PathVariable UUID id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<OAuthUser> create(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String displayName = body.get("displayName");
        String password = body.get("password");
        OAuthUser user = userService.create(email, displayName, password);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        Optional<OAuthUser> existing = userService.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        OAuthUser user = existing.get();
        if (body.containsKey("email")) user.setEmail((String) body.get("email"));
        if (body.containsKey("displayName")) user.setDisplayName((String) body.get("displayName"));
        if (body.containsKey("enabled")) user.setEnabled((Boolean) body.get("enabled"));

        return ResponseEntity.ok(userService.update(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable UUID id) {
        String newPassword = userService.resetPassword(id);
        return ResponseEntity.ok(Map.of("password", newPassword));
    }
}
