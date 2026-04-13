package com.baskettecase.feauxauth.service;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import static org.assertj.core.api.Assertions.*;

class PkceServiceTest {

    private final PkceService pkceService = new PkceService();

    @Test
    void verifyChallenge_validS256_returnsTrue() throws Exception {
        String verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

        assertThat(pkceService.verifyChallenge(verifier, challenge)).isTrue();
    }

    @Test
    void verifyChallenge_wrongVerifier_returnsFalse() throws Exception {
        String verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

        assertThat(pkceService.verifyChallenge("wrong-verifier", challenge)).isFalse();
    }

    @Test
    void generateChallenge_producesValidChallenge() {
        String verifier = "test-verifier-string-with-enough-entropy-1234567890";
        String challenge = pkceService.generateChallenge(verifier);

        assertThat(challenge).isNotBlank();
        assertThat(pkceService.verifyChallenge(verifier, challenge)).isTrue();
    }
}
