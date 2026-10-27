package com.abc.emergency.config;

import com.abc.emergency.util.DatabaseSecurityException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DatabaseSecurityConfigTest {

    @Test
    @DisplayName("DatabaseSecurityConfig passes with valid test configuration properties")
    void testConfigWithValidProperties_Passes() {
        DatabaseSecurityConfig config = new DatabaseSecurityConfig(
                "jdbc:mysql://localhost:3306/connected_ambulance_test",
                "amb_test_app",
                "SecretPass123!"
        );

        assertDoesNotThrow(config::validateDatabaseConfiguration);
    }

    @Test
    @DisplayName("DatabaseSecurityConfig rejects production database URL property")
    void testConfigWithProductionUrl_FailsClosed() {
        DatabaseSecurityConfig config = new DatabaseSecurityConfig(
                "jdbc:mysql://localhost:3306/connected_ambulance",
                "amb_test_app",
                "SecretPass123!"
        );

        assertThrows(DatabaseSecurityException.class, config::validateDatabaseConfiguration);
    }

    @Test
    @DisplayName("DatabaseSecurityConfig rejects root user property")
    void testConfigWithRootUser_FailsClosed() {
        DatabaseSecurityConfig config = new DatabaseSecurityConfig(
                "jdbc:mysql://localhost:3306/connected_ambulance_test",
                "root",
                "SecretPass123!"
        );

        assertThrows(DatabaseSecurityException.class, config::validateDatabaseConfiguration);
    }

    @Test
    @DisplayName("DatabaseSecurityConfig rejects missing or empty URL property when fallback is absent")
    void testConfigWithMissingUrl_FailsClosed() {
        // Tests explicitly unconfigured state (both Spring property and environment fallback are absent)
        DatabaseSecurityConfig config = new DatabaseSecurityConfig(
                "",
                "amb_test_app",
                "SecretPass123!",
                varName -> null
        );

        assertThrows(DatabaseSecurityException.class, config::validateDatabaseConfiguration);
    }

    @Test
    @DisplayName("DatabaseSecurityConfig successfully falls back to valid environment variables when properties are null")
    void testConfigWithEnvironmentFallback_Passes() {
        DatabaseSecurityConfig config = new DatabaseSecurityConfig(
                null,
                null,
                null,
                varName -> {
                    switch (varName) {
                        case "DB_URL": return "jdbc:mysql://localhost:3306/connected_ambulance_test";
                        case "DB_USER": return "amb_test_app";
                        case "DB_PASSWORD": return "SecretPass123!";
                        default: return null;
                    }
                }
        );

        assertDoesNotThrow(config::validateDatabaseConfiguration);
    }
}
