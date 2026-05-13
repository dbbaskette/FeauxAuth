package com.baskettecase.feauxauth.oauth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OAuthErrorHintsTest {

    @Test
    void wellKnownCodesReturnHints() {
        assertNotNull(OAuthErrorHints.hint("invalid_request"));
        assertNotNull(OAuthErrorHints.hint("invalid_client"));
        assertNotNull(OAuthErrorHints.hint("invalid_grant"));
        assertNotNull(OAuthErrorHints.hint("access_denied"));
        assertNotNull(OAuthErrorHints.hint("expired_code"));
        assertNotNull(OAuthErrorHints.hint("authorization_pending"));
    }

    @Test
    void unknownCodeReturnsNull() {
        assertNull(OAuthErrorHints.hint("frobnicated_overlord"));
    }

    @Test
    void nullCodeReturnsNull() {
        assertNull(OAuthErrorHints.hint(null));
    }

    @Test
    void caseInsensitive() {
        assertEquals(
            OAuthErrorHints.hint("invalid_grant"),
            OAuthErrorHints.hint("INVALID_GRANT"));
        assertEquals(
            OAuthErrorHints.hint("invalid_grant"),
            OAuthErrorHints.hint("  Invalid_Grant  "));
    }
}
