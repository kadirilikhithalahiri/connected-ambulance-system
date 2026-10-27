package com.abc.emergency;

import com.abc.emergency.util.PasswordUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordUtilTest {

    @Test
    void testBCryptHashingAndVerificationSuccess() {
        String rawPassword = "MedicalSecret#2026";
        String hash = PasswordUtil.hashPassword(rawPassword);

        assertNotNull(hash);
        assertTrue(PasswordUtil.isBCryptHash(hash));
        assertTrue(PasswordUtil.checkPassword(rawPassword, hash));
    }

    @Test
    void testIncorrectPasswordRejection() {
        String rawPassword = "CorrectPassword123";
        String hash = PasswordUtil.hashPassword(rawPassword);

        assertFalse(PasswordUtil.checkPassword("WrongPassword123", hash));
        assertFalse(PasswordUtil.checkPassword("", hash));
        assertFalse(PasswordUtil.checkPassword(null, hash));
    }

    @Test
    void testLegacyPlaintextVerification() {
        String legacyPlaintext = "paramedic123";

        // Plaintext should match when stored value is legacy
        assertTrue(PasswordUtil.checkPassword("paramedic123", legacyPlaintext));

        // Incorrect password should fail against legacy plaintext
        assertFalse(PasswordUtil.checkPassword("wrongPassword", legacyPlaintext));
    }

    @Test
    void testBCryptFormatDetection() {
        // Valid BCrypt hash generated with cost 10
        String validHash = PasswordUtil.hashPassword("testPassword", 10);
        assertTrue(PasswordUtil.isBCryptHash(validHash));

        // Plaintext strings should return false
        assertFalse(PasswordUtil.isBCryptHash("paramedic123"));
        assertFalse(PasswordUtil.isBCryptHash("admin123"));
        assertFalse(PasswordUtil.isBCryptHash("short"));

        // Null and empty checks
        assertFalse(PasswordUtil.isBCryptHash(null));
        assertFalse(PasswordUtil.isBCryptHash(""));

        // 60-character string but not matching BCrypt pattern
        String fake60Char = "A".repeat(60);
        assertFalse(PasswordUtil.isBCryptHash(fake60Char));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "$2a$12$short",
            "$2z$12$e808v1y.n1oGv4n8iG6e7uQe51n0x5.0x0m7l7e2i7n9e3u7m8y6q",
            "$2a$12$has spaces in string e808v1y n1oGv4n8iG6e7uQe51n0x50x0m7l7e2",
            "",
            "   "
    })
    void testMalformedInputSafety(String invalidInput) {
        assertFalse(PasswordUtil.isBCryptHash(invalidInput));
        assertFalse(PasswordUtil.checkPassword("somePassword", invalidInput));
    }

    @Test
    void testMalformedBCryptCandidateDoesNotFallBackToPlaintext() {
        String malformedHash = "$2a$12$malformed";
        // Even if raw password string equals malformedHash, checkPassword must fail safely
        assertFalse(PasswordUtil.checkPassword(malformedHash, malformedHash));
    }

    @Test
    void testInvalidBCryptSaltHandledSafely() {
        // 60 chars matching format pattern but with invalid salt data
        String badSaltHash = "$2a$12$.....................................................";
        // Must safely return false without throwing uncaught exceptions
        assertFalse(PasswordUtil.checkPassword("somePassword", badSaltHash));
    }

    @Test
    void testCustomWorkFactor() {
        String hashCost10 = PasswordUtil.hashPassword("quickPass", 10);
        assertTrue(PasswordUtil.isBCryptHash(hashCost10));
        assertTrue(hashCost10.startsWith("$2a$10$"));
        assertTrue(PasswordUtil.checkPassword("quickPass", hashCost10));
    }

    @Test
    void testEmptyPasswordHashingThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hashPassword(""));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hashPassword(null));
    }
}
