package com.baskettecase.feauxauth.oauth;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScopeRiskTest {

    @Test
    void standardOidcScopesClassifyAsRead() {
        for (String scope : List.of("openid", "profile", "email", "address", "phone")) {
            assertEquals(ScopeRisk.READ, ScopeRisk.classify(scope),
                "expected READ for " + scope);
        }
    }

    @Test
    void offlineAccessClassifiesAsInfo() {
        assertEquals(ScopeRisk.INFO, ScopeRisk.classify("offline_access"));
    }

    @Test
    void writeSuffixesClassifyAsWrite() {
        assertEquals(ScopeRisk.WRITE, ScopeRisk.classify("email.send"));
        assertEquals(ScopeRisk.WRITE, ScopeRisk.classify("calendar.write"));
        assertEquals(ScopeRisk.WRITE, ScopeRisk.classify("files.modify"));
        assertEquals(ScopeRisk.WRITE, ScopeRisk.classify("projects.create"));
        assertEquals(ScopeRisk.WRITE, ScopeRisk.classify("items.delete"));
    }

    @Test
    void adminTokensClassifyAsAdmin() {
        assertEquals(ScopeRisk.ADMIN, ScopeRisk.classify("admin"));
        assertEquals(ScopeRisk.ADMIN, ScopeRisk.classify("users.manage"));
        assertEquals(ScopeRisk.ADMIN, ScopeRisk.classify("tenant.admin"));
        assertEquals(ScopeRisk.ADMIN, ScopeRisk.classify("billing.manage"));
    }

    @Test
    void adminTakesPrecedenceOverWriteSuffix() {
        // ".manage" is admin, even though it would also match no write suffix
        assertEquals(ScopeRisk.ADMIN, ScopeRisk.classify("orgs.manage"));
    }

    @Test
    void unknownScopesDefaultToRead() {
        assertEquals(ScopeRisk.READ, ScopeRisk.classify("groups.read"));
        assertEquals(ScopeRisk.READ, ScopeRisk.classify("calendar.events"));
        assertEquals(ScopeRisk.READ, ScopeRisk.classify("foobar"));
    }

    @Test
    void nullAndBlankDefaultToRead() {
        assertEquals(ScopeRisk.READ, ScopeRisk.classify(null));
        assertEquals(ScopeRisk.READ, ScopeRisk.classify(""));
        assertEquals(ScopeRisk.READ, ScopeRisk.classify("   "));
    }

    @Test
    void caseInsensitive() {
        assertEquals(ScopeRisk.READ, ScopeRisk.classify("OPENID"));
        assertEquals(ScopeRisk.ADMIN, ScopeRisk.classify("Admin"));
        assertEquals(ScopeRisk.WRITE, ScopeRisk.classify("Calendar.WRITE"));
    }

    @Test
    void classifyAllPreservesOrderAndDeduplicates() {
        List<ScopeRisk.ScopedRisk> result = ScopeRisk.classifyAll(
            List.of("openid", "profile", "openid", "email.send", "admin"));
        assertEquals(4, result.size());
        assertEquals("openid", result.get(0).scope());
        assertEquals(ScopeRisk.READ, result.get(0).risk());
        assertEquals("profile", result.get(1).scope());
        assertEquals("email.send", result.get(2).scope());
        assertEquals(ScopeRisk.WRITE, result.get(2).risk());
        assertEquals("admin", result.get(3).scope());
        assertEquals(ScopeRisk.ADMIN, result.get(3).risk());
    }

    @Test
    void classifyAllHandlesSpaceSeparatedString() {
        List<ScopeRisk.ScopedRisk> result = ScopeRisk.classifyAll("openid profile email.send");
        assertEquals(3, result.size());
        assertEquals(ScopeRisk.READ, result.get(0).risk());
        assertEquals(ScopeRisk.READ, result.get(1).risk());
        assertEquals(ScopeRisk.WRITE, result.get(2).risk());
    }

    @Test
    void classifyAllHandlesNullAndEmpty() {
        assertTrue(ScopeRisk.classifyAll((String) null).isEmpty());
        assertTrue(ScopeRisk.classifyAll("").isEmpty());
        assertTrue(ScopeRisk.classifyAll("   ").isEmpty());
    }

    @Test
    void cssTokenIsLowerCase() {
        assertEquals("read", ScopeRisk.READ.cssToken());
        assertEquals("write", ScopeRisk.WRITE.cssToken());
        assertEquals("admin", ScopeRisk.ADMIN.cssToken());
        assertEquals("info", ScopeRisk.INFO.cssToken());
    }
}
