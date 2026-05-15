package com.baskettecase.feauxauth.oauth;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Classification of OAuth scopes by risk. Mirrored in
 * {@code frontend/src/lib/scopes.js} — both implementations must stay in sync.
 *
 * <p>See spec: {@code docs/superpowers/specs/2026-05-13-aurora-design-system-design.md}
 */
public enum ScopeRisk {
    READ,
    WRITE,
    ADMIN,
    INFO;

    private static final Set<String> STANDARD_READ_SCOPES = Set.of(
        "openid", "profile", "email", "address", "phone"
    );

    private static final List<String> WRITE_SUFFIXES = List.of(
        ".write", ".send", ".modify", ".create", ".delete"
    );

    /**
     * Classify a single scope. Null/blank scopes default to {@link #READ}.
     */
    public static ScopeRisk classify(String scope) {
        if (scope == null) return READ;
        String s = scope.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return READ;

        if ("offline_access".equals(s)) return INFO;
        if (STANDARD_READ_SCOPES.contains(s)) return READ;

        if (s.contains("admin") || s.contains("manage") || s.endsWith(".manage")) return ADMIN;
        for (String suffix : WRITE_SUFFIXES) {
            if (s.endsWith(suffix)) return WRITE;
        }
        return READ;
    }

    /**
     * Classify a collection of scopes, preserving insertion order and de-duplicating.
     */
    public static List<ScopedRisk> classifyAll(Collection<String> scopes) {
        if (scopes == null || scopes.isEmpty()) return Collections.emptyList();
        Set<String> seen = new LinkedHashSet<>();
        return scopes.stream()
            .filter(s -> s != null && !s.isBlank())
            .map(String::trim)
            .filter(seen::add)
            .map(s -> new ScopedRisk(s, classify(s)))
            .collect(Collectors.toList());
    }

    /**
     * Classify a space-separated scope string (the standard OAuth wire format).
     */
    public static List<ScopedRisk> classifyAll(String scopeString) {
        if (scopeString == null || scopeString.isBlank()) return Collections.emptyList();
        return classifyAll(Arrays.asList(scopeString.trim().split("\\s+")));
    }

    /** Lower-case token used in Tailwind class names (e.g., {@code badge-read}). */
    public String cssToken() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * A single scope paired with its classified risk. Designed for direct use
     * in Thymeleaf templates ({@code scope.scope}, {@code scope.risk.cssToken()}).
     */
    public record ScopedRisk(String scope, ScopeRisk risk) {
        public String cssToken() { return risk.cssToken(); }
    }
}
