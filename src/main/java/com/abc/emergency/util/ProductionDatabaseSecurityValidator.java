
package com.abc.emergency.util;

import java.net.URI;
import java.util.Locale;

public final class ProductionDatabaseSecurityValidator {

    private static final String REQUIRED_DATABASE = "connected_ambulance";
    private static final String REQUIRED_HOST = "mysql-1412b881-kadirilikhithalahiri-c087.h.aivencloud.com";
    private static final int REQUIRED_PORT = 28268;
    private static final String REQUIRED_USER = "avnadmin";

    private ProductionDatabaseSecurityValidator() {
    }

    public static void validate(String url, String user, String password) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalStateException("DB_URL is missing.");
        }

        if (user == null || !REQUIRED_USER.equals(user.trim())) {
            throw new IllegalStateException("DB_USER is not approved for production.");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new IllegalStateException("DB_PASSWORD is missing.");
        }

        String trimmedUrl = url.trim();

        if (!trimmedUrl.startsWith("jdbc:mysql://")) {
            throw new IllegalStateException("Production requires a MySQL JDBC URL.");
        }

        try {
            URI uri = URI.create(trimmedUrl.substring("jdbc:".length()));

            if (!"mysql".equalsIgnoreCase(uri.getScheme())
                    || !REQUIRED_HOST.equalsIgnoreCase(uri.getHost())
                    || uri.getPort() != REQUIRED_PORT
                    || !("/" + REQUIRED_DATABASE).equals(uri.getPath())
                    || uri.getUserInfo() != null
                    || uri.getFragment() != null) {
                throw new IllegalStateException(
                        "Production database URL does not match the approved Aiven endpoint.");
            }

            String query = uri.getQuery();
            if (query == null || query.isBlank()
                    || !query.equalsIgnoreCase("sslMode=REQUIRED")) {
                throw new IllegalStateException(
                        "Production database connection must require SSL.");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Production database URL is malformed.");
        }
    }
}
