package com.abc.emergency.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Focused automated unit tests for fail-closed database configuration validation
 * enforcing strict allowlist policies on JDBC URLs, hosts, ports, users, and parameters.
 */
public class DatabaseSecurityValidatorTest {

    private static final String VALID_TEST_URL_LOCALHOST = "jdbc:mysql://localhost:3306/connected_ambulance_test";
    private static final String VALID_TEST_URL_LOOPBACK = "jdbc:mysql://127.0.0.1:3306/connected_ambulance_test";
    private static final String VALID_TEST_USER = "amb_test_app";
    private static final String VALID_TEST_PASSWORD = "ValidTestPassword123!";

    // =========================================================================
    // 1. Exact Approved Base URLs, Hosts, and Ports
    // =========================================================================

    @Test
    @DisplayName("Valid test configuration with localhost is accepted")
    void testValidConfig_Localhost_Accepted() {
        assertDoesNotThrow(() ->
                DatabaseSecurityValidator.validate(VALID_TEST_URL_LOCALHOST, VALID_TEST_USER, VALID_TEST_PASSWORD)
        );
    }

    @Test
    @DisplayName("Valid test configuration with 127.0.0.1 is accepted")
    void testValidConfig_LoopbackIp_Accepted() {
        assertDoesNotThrow(() ->
                DatabaseSecurityValidator.validate(VALID_TEST_URL_LOOPBACK, VALID_TEST_USER, VALID_TEST_PASSWORD)
        );
    }

    @Test
    @DisplayName("Case-insensitive protocol and host/database casing is accepted")
    void testValidConfig_CaseInsensitiveBase_Accepted() {
        assertDoesNotThrow(() ->
                DatabaseSecurityValidator.validate(
                        "JDBC:MYSQL://LOCALHOST:3306/CONNECTED_AMBULANCE_TEST",
                        VALID_TEST_USER,
                        VALID_TEST_PASSWORD
                )
        );
    }

    // =========================================================================
    // 2. Production Database Rejection
    // =========================================================================

    @Test
    @DisplayName("Production database URL 'connected_ambulance' is strictly forbidden")
    void testProductionDatabaseUrl_Rejected() {
        String prodUrl = "jdbc:mysql://localhost:3306/connected_ambulance";
        DatabaseSecurityException ex = assertThrows(DatabaseSecurityException.class, () ->
                DatabaseSecurityValidator.validate(prodUrl, VALID_TEST_USER, VALID_TEST_PASSWORD)
        );
        assertTrue(ex.getMessage().contains("production database 'connected_ambulance' is strictly forbidden"));
    }

    @Test
    @DisplayName("Production database URL with query parameters is strictly forbidden")
    void testProductionDatabaseUrlWithParams_Rejected() {
        String prodUrlWithParams = "jdbc:mysql://localhost:3306/connected_ambulance?serverTimezone=UTC";
        DatabaseSecurityException ex = assertThrows(DatabaseSecurityException.class, () ->
                DatabaseSecurityValidator.validate(prodUrlWithParams, VALID_TEST_USER, VALID_TEST_PASSWORD)
        );
        assertTrue(ex.getMessage().contains("production database 'connected_ambulance' is strictly forbidden"));
    }

    // =========================================================================
    // 3. Remote Host and Non-Approved Port Rejection
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:mysql://remote-db.production.internal:3306/connected_ambulance_test",
            "jdbc:mysql://192.168.1.50:3306/connected_ambulance_test",
            "jdbc:mysql://10.0.0.1:3306/connected_ambulance_test",
            "jdbc:mysql://mysql-prod:3306/connected_ambulance_test",
            "jdbc:mysql://example.com:3306/connected_ambulance_test"
    })
    @DisplayName("Unexpected or remote hosts are strictly rejected")
    void testUnexpectedHost_Rejected(String remoteUrl) {
        DatabaseSecurityException ex = assertThrows(DatabaseSecurityException.class, () ->
                DatabaseSecurityValidator.validate(remoteUrl, VALID_TEST_USER, VALID_TEST_PASSWORD)
        );
        assertTrue(ex.getMessage().contains("Host and port must strictly be 'localhost:3306' or '127.0.0.1:3306'"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:mysql://localhost:3307/connected_ambulance_test",
            "jdbc:mysql://127.0.0.1:3308/connected_ambulance_test",
            "jdbc:mysql://localhost:8080/connected_ambulance_test",
            "jdbc:mysql://localhost/connected_ambulance_test"
    })
    @DisplayName("Non-standard ports or missing ports are rejected")
    void testUnexpectedPort_Rejected(String nonStandardPortUrl) {
        DatabaseSecurityException ex = assertThrows(DatabaseSecurityException.class, () ->
                DatabaseSecurityValidator.validate(nonStandardPortUrl, VALID_TEST_USER, VALID_TEST_PASSWORD)
        );
        assertTrue(ex.getMessage().contains("Database security violation"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "postgres://localhost:3306/connected_ambulance_test",
            "http://localhost:3306/connected_ambulance_test",
            "mysql://localhost:3306/connected_ambulance_test",
            "not_a_valid_url"
    })
    @DisplayName("Malformed protocols or non-MySQL JDBC URLs are rejected")
    void testMalformedProtocol_Rejected(String malformedUrl) {
        DatabaseSecurityException ex = assertThrows(DatabaseSecurityException.class, () ->
                DatabaseSecurityValidator.validate(malformedUrl, VALID_TEST_USER, VALID_TEST_PASSWORD)
        );
        assertTrue(ex.getMessage().contains("must use 'jdbc:mysql://' protocol"));
    }

    // =========================================================================
    // 4. Embedded User-Info and Percent-Encoded Path Rejection
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:mysql://user:pass@localhost:3306/connected_ambulance_test",
            "jdbc:mysql://amb_test_app@localhost:3306/connected_ambulance_test",
            "jdbc:mysql://root:secret@127.0.0.1:3306/connected_ambulance_test"
    })
    @DisplayName("Embedded user credentials in JDBC URL are strictly forbidden")
    void testEmbeddedUserInfo_Rejected(String embeddedCredsUrl) {
        DatabaseSecurityException ex = assertThrows(DatabaseSecurityException.class, () ->
                DatabaseSecurityValidator.validate(embeddedCredsUrl, VALID_TEST_USER, VALID_TEST_PASSWORD)
        );
        assertTrue(ex.getMessage().contains("Embedded user credentials in JDBC URL are strictly forbidden"));
        assertFalse(ex.getMessage().contains("pass"));
        assertFalse(ex.getMessage().contains("secret"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:mysql://localhost:3306/%63onnected_ambulance_test",
            "jdbc:mysql://%6c%6f%63%61%6c%68%6f%73%74:3306/connected_ambulance_test",
            "jdbc:mysql://localhost:3306/connected_ambulance_test%20",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?param%3Dval"
    })
    @DisplayName("Percent-encoded characters in URL are strictly forbidden")
    void testPercentEncodedPath_Rejected(String encodedUrl) {
        DatabaseSecurityException ex = assertThrows(DatabaseSecurityException.class, () ->
                DatabaseSecurityValidator.validate(encodedUrl, VALID_TEST_USER, VALID_TEST_PASSWORD)
        );
        assertTrue(ex.getMessage().contains("URL-encoded characters ('%') are strictly forbidden"));
    }

    // =========================================================================
    // 5. Query Parameter Allowlist Policy: Rejection of Unknown Parameters
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:mysql://localhost:3306/connected_ambulance_test?sessionVariables=auto_commit=0",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?sessionVariables=wait_timeout=600",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?allowMultiQueries=true",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?allowLoadLocalInfile=true",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?autoDeserialize=true",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?connectionAttributes=foo:bar",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?serverTimezone=UTC",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?useSSL=false",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?characterEncoding=UTF-8",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?allowPublicKeyRetrieval=true",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?unknownParam=123"
    })
    @DisplayName("Unknown or unauthorized query parameters are strictly forbidden under allowlist policy")
    void testUnknownQueryParams_Rejected(String queryUrl) {
        DatabaseSecurityException ex = assertThrows(DatabaseSecurityException.class, () ->
                DatabaseSecurityValidator.validate(queryUrl, VALID_TEST_USER, VALID_TEST_PASSWORD)
        );
        assertTrue(ex.getMessage().contains("Unauthorized query parameter")
                || ex.getMessage().contains("strictly forbidden by allowlist policy"));
    }

    // =========================================================================
    // 6. Duplicate Query Parameters and Malformed Query Strings
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:mysql://localhost:3306/connected_ambulance_test?param=1&param=2",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?sessionVariables=1&sessionVariables=2",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?foo=abc&bar=123&foo=def",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?serverTimezone=UTC&serverTimezone=GMT"
    })
    @DisplayName("Duplicate query parameters are strictly rejected")
    void testDuplicateQueryParams_Rejected(String duplicateParamUrl) {
        DatabaseSecurityException ex = assertThrows(DatabaseSecurityException.class, () ->
                DatabaseSecurityValidator.validate(duplicateParamUrl, VALID_TEST_USER, VALID_TEST_PASSWORD)
        );
        assertTrue(ex.getMessage().contains("Duplicate query parameter detected"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:mysql://localhost:3306/connected_ambulance_test?",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?&&",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?&param=1",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?param=1&",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?param=1&&other=2",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?param",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?=value",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?   =value",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?param=",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?param=   ",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?param1=val1?param2=val2"
    })
    @DisplayName("Malformed query strings and separators are strictly rejected")
    void testMalformedQueryStrings_Rejected(String malformedQueryUrl) {
        DatabaseSecurityException ex = assertThrows(DatabaseSecurityException.class, () ->
                DatabaseSecurityValidator.validate(malformedQueryUrl, VALID_TEST_USER, VALID_TEST_PASSWORD)
        );
        assertTrue(ex.getMessage().contains("Malformed") || ex.getMessage().contains("Empty query parameter"));
    }

    // =========================================================================
    // 7. Query Parameter Case Variations
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:mysql://localhost:3306/connected_ambulance_test?SessionVariables=1",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?SESSIONVARIABLES=1",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?ServerTimezone=UTC",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?SERVERTIMEZONE=UTC",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?Usessl=false",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?param=1&PARAM=2"
    })
    @DisplayName("Case variations of query parameters are strictly detected and rejected")
    void testQueryParamCaseVariations_Rejected(String caseVariantUrl) {
        DatabaseSecurityException ex = assertThrows(DatabaseSecurityException.class, () ->
                DatabaseSecurityValidator.validate(caseVariantUrl, VALID_TEST_USER, VALID_TEST_PASSWORD)
        );
        assertTrue(ex.getMessage().contains("Duplicate query parameter")
                || ex.getMessage().contains("Unauthorized query parameter")
                || ex.getMessage().contains("strictly forbidden by allowlist policy"));
    }

    // =========================================================================
    // 8. URL Fragments and Extra Path Segments Rejection
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:mysql://localhost:3306/connected_ambulance_test#fragment",
            "jdbc:mysql://localhost:3306/connected_ambulance_test#section1",
            "jdbc:mysql://localhost:3306/connected_ambulance_test?param=val#fragment"
    })
    @DisplayName("URL fragments ('#') are strictly forbidden")
    void testUrlFragments_Rejected(String fragmentUrl) {
        DatabaseSecurityException ex = assertThrows(DatabaseSecurityException.class, () ->
                DatabaseSecurityValidator.validate(fragmentUrl, VALID_TEST_USER, VALID_TEST_PASSWORD)
        );
        assertTrue(ex.getMessage().contains("URL fragments ('#') are strictly forbidden"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:mysql://localhost:3306/connected_ambulance_test/extra",
            "jdbc:mysql://localhost:3306/connected_ambulance_test/",
            "jdbc:mysql://localhost:3306/connected_ambulance_test/subpath/endpoint",
            "jdbc:mysql://localhost:3306/connected_ambulance_test/../connected_ambulance"
    })
    @DisplayName("Extra path segments or path traversal are strictly forbidden")
    void testExtraPathSegments_Rejected(String extraPathUrl) {
        DatabaseSecurityException ex = assertThrows(DatabaseSecurityException.class, () ->
                DatabaseSecurityValidator.validate(extraPathUrl, VALID_TEST_USER, VALID_TEST_PASSWORD)
        );
        assertTrue(ex.getMessage().contains("Extra path segments or path traversal are strictly forbidden")
                || ex.getMessage().contains("Malformed or unapproved JDBC URL")
                || ex.getMessage().contains("production database 'connected_ambulance' is strictly forbidden"));
    }

    @Test
    @DisplayName("Semicolon separators and path parameters are strictly forbidden")
    void testSemicolonPathParameters_Rejected() {
        String semiColonUrl = "jdbc:mysql://localhost:3306/connected_ambulance_test;param=value";
        DatabaseSecurityException ex = assertThrows(DatabaseSecurityException.class, () ->
                DatabaseSecurityValidator.validate(semiColonUrl, VALID_TEST_USER, VALID_TEST_PASSWORD)
        );
        assertTrue(ex.getMessage().contains("Semicolon separators or path parameters are strictly forbidden"));
    }

    // =========================================================================
    // 9. Missing URL, Username, or Password Validation
    // =========================================================================

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Missing or blank DB_URL is strictly rejected")
    void testMissingUrl_Rejected(String missingUrl) {
        DatabaseSecurityException ex = assertThrows(DatabaseSecurityException.class, () ->
                DatabaseSecurityValidator.validate(missingUrl, VALID_TEST_USER, VALID_TEST_PASSWORD)
        );
        assertTrue(ex.getMessage().contains("'DB_URL' is missing or blank"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Missing or blank DB_USER is strictly rejected")
    void testMissingUser_Rejected(String missingUser) {
        DatabaseSecurityException ex = assertThrows(DatabaseSecurityException.class, () ->
                DatabaseSecurityValidator.validate(VALID_TEST_URL_LOCALHOST, missingUser, VALID_TEST_PASSWORD)
        );
        assertTrue(ex.getMessage().contains("'DB_USER' is missing or blank"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Missing or blank DB_PASSWORD is strictly rejected")
    void testMissingPassword_Rejected(String missingPassword) {
        DatabaseSecurityException ex = assertThrows(DatabaseSecurityException.class, () ->
                DatabaseSecurityValidator.validate(VALID_TEST_URL_LOCALHOST, VALID_TEST_USER, missingPassword)
        );
        assertTrue(ex.getMessage().contains("'DB_PASSWORD' is missing or blank"));
    }

    // =========================================================================
    // 10. Root and Incorrect Usernames Rejection
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {"root", "ROOT", "Root", " root ", "rOoT"})
    @DisplayName("Privileged user 'root' in any casing or whitespace variant is strictly forbidden")
    void testRootUser_Rejected(String rootVariant) {
        DatabaseSecurityException ex = assertThrows(DatabaseSecurityException.class, () ->
                DatabaseSecurityValidator.validate(VALID_TEST_URL_LOCALHOST, rootVariant, VALID_TEST_PASSWORD)
        );
        assertTrue(ex.getMessage().contains("Privileged user 'root' is strictly forbidden"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin", "administrator", "amb_test_setup", "dispatcher", "unknown_user"})
    @DisplayName("Any username other than 'amb_test_app' is rejected")
    void testUnauthorizedUser_Rejected(String unauthorizedUser) {
        DatabaseSecurityException ex = assertThrows(DatabaseSecurityException.class, () ->
                DatabaseSecurityValidator.validate(VALID_TEST_URL_LOCALHOST, unauthorizedUser, VALID_TEST_PASSWORD)
        );
        assertTrue(ex.getMessage().contains("Unauthorized database user"));
        assertTrue(ex.getMessage().contains("Only 'amb_test_app' is permitted"));
    }

    // =========================================================================
    // 11. Safe Exception Messages Without Passwords or Secrets
    // =========================================================================

    @Test
    @DisplayName("Passwords and secrets are never leaked in exception messages")
    void testPasswordNeverLeakedInExceptionMessages() {
        String sensitiveCanaryPassword = "SUPER_SECRET_CANARY_VALUE_NEVER_LOG_98765";

        // Test with invalid URL
        DatabaseSecurityException ex1 = assertThrows(DatabaseSecurityException.class, () ->
                DatabaseSecurityValidator.validate("invalid_url", VALID_TEST_USER, sensitiveCanaryPassword)
        );
        assertFalse(ex1.getMessage().contains(sensitiveCanaryPassword), "Password leaked in invalid URL exception!");

        // Test with invalid user
        DatabaseSecurityException ex2 = assertThrows(DatabaseSecurityException.class, () ->
                DatabaseSecurityValidator.validate(VALID_TEST_URL_LOCALHOST, "root", sensitiveCanaryPassword)
        );
        assertFalse(ex2.getMessage().contains(sensitiveCanaryPassword), "Password leaked in invalid user exception!");

        // Test with embedded credentials
        DatabaseSecurityException ex3 = assertThrows(DatabaseSecurityException.class, () ->
                DatabaseSecurityValidator.validate(
                        "jdbc:mysql://canary_user:" + sensitiveCanaryPassword + "@localhost:3306/connected_ambulance_test",
                        VALID_TEST_USER,
                        VALID_TEST_PASSWORD
                )
        );
        assertFalse(ex3.getMessage().contains(sensitiveCanaryPassword), "Password leaked in embedded credentials exception!");
        assertTrue(ex3.getMessage().contains("***@localhost:3306"), "Credentials not masked in URL exception!");
    }

    // =========================================================================
    // 12. Legacy DBConnection Fail-Closed Behavior
    // =========================================================================

    @Test
    @DisplayName("DBConnection.getConnection() fails closed with SQLException when configuration is missing")
    void testDBConnectionGetConnection_FailsClosed() {
        // Explicit configuration injection ensures test is resilient regardless of ambient test process environment
        SQLException ex = assertThrows(SQLException.class, () -> DBConnection.getConnection(null, null, null));
        assertTrue(ex.getMessage().contains("Database connection blocked by security policy"));
        assertNotNull(ex.getCause());
        assertInstanceOf(DatabaseSecurityException.class, ex.getCause());
    }
}
