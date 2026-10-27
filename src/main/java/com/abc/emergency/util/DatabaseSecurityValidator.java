package com.abc.emergency.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enforces strict fail-closed security policies for database connections across
 * both Spring Boot managed datasources and legacy JDBC DBConnection calls.
 *
 * Security Boundaries and Scope:
 * - Protocol: Must strictly be 'jdbc:mysql://'.
 * - Host: Must strictly be 'localhost' or '127.0.0.1'.
 * - Port: Must strictly be '3306'.
 * - Target Database: Must strictly be 'connected_ambulance_test'.
 * - User: Must strictly be 'amb_test_app'.
 * - Password: Must be non-blank (never leaked into exceptions/logs).
 * - Query Parameters: Strict allowlist enforcement. Adhering to the safest
 *   policy, JDBC URLs containing query parameters are strictly forbidden
 *   because no application component requires query parameters.
 * - URL Fragments ('#'), embedded credentials ('user:pass@'), extra path segments,
 *   and URL-encoded characters ('%') are strictly rejected.
 *
 * NOTE: URL validation alone does not prove the actual network destination.
 * Environmental manipulations such as local host-file redirection, DNS overrides,
 * port forwarding, and local proxying remain outside the scope of static URL validation.
 */
public final class DatabaseSecurityValidator {

    public static final String REQUIRED_USER = "amb_test_app";
    public static final String REQUIRED_DB = "connected_ambulance_test";
    public static final String PRODUCTION_DB = "connected_ambulance";

    // Strict Base URL regex: protocol jdbc:mysql://, host localhost or 127.0.0.1, port 3306, path /connected_ambulance_test
    // No extra path segments or path traversal permitted.
    private static final Pattern APPROVED_BASE_URL_PATTERN = Pattern.compile(
            "^jdbc:mysql://(localhost|127\\.0\\.0\\.1):3306/connected_ambulance_test$",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Strict allowlist of permitted query parameters and their allowed value regex patterns.
     * In accordance with the safest policy, this allowlist is empty because no application
     * requirements necessitate query parameters.
     */
    private static final Map<String, Pattern> ALLOWED_QUERY_PARAMETERS = Collections.emptyMap();

    private DatabaseSecurityValidator() {
        // Utility class
    }

    /**
     * Validates database configuration parameters against fail-closed security policies.
     *
     * @param url database JDBC URL
     * @param user database username
     * @param password database password (never logged or included in exceptions)
     * @throws DatabaseSecurityException if any validation check fails
     */
    public static void validate(String url, String user, String password) {
        validateUrl(url);
        validateUser(user);
        validatePassword(password);
    }

    /**
     * Validates that the JDBC URL strictly matches approved test database targets.
     *
     * @param url database JDBC URL
     * @throws DatabaseSecurityException if URL is missing, malformed, or targets forbidden databases/hosts
     */
    public static void validateUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new DatabaseSecurityException(
                    "Database configuration error: Required environment variable 'DB_URL' is missing or blank."
            );
        }

        String trimmedUrl = url.trim();

        // Check for embedded credentials (e.g., user:pass@host)
        if (trimmedUrl.contains("@")) {
            throw new DatabaseSecurityException(
                    "Database security violation: Embedded user credentials in JDBC URL are strictly forbidden: '"
                            + sanitizeUrl(trimmedUrl) + "'"
            );
        }

        // Check for URL fragments ('#')
        if (trimmedUrl.contains("#")) {
            throw new DatabaseSecurityException(
                    "Database security violation: URL fragments ('#') are strictly forbidden in JDBC URLs: '"
                            + sanitizeUrl(trimmedUrl) + "'"
            );
        }

        // Check for percent-encoded characters in URL
        if (trimmedUrl.contains("%")) {
            throw new DatabaseSecurityException(
                    "Database security violation: URL-encoded characters ('%') are strictly forbidden: '"
                            + sanitizeUrl(trimmedUrl) + "'"
            );
        }

        // Check for semicolon separators / path parameters
        if (trimmedUrl.contains(";")) {
            throw new DatabaseSecurityException(
                    "Database security violation: Semicolon separators or path parameters are strictly forbidden: '"
                            + sanitizeUrl(trimmedUrl) + "'"
            );
        }

        // Protocol check
        if (!trimmedUrl.toLowerCase(Locale.ROOT).startsWith("jdbc:mysql://")) {
            throw new DatabaseSecurityException(
                    "Database security violation: URL must use 'jdbc:mysql://' protocol. URL rejected: '"
                            + sanitizeUrl(trimmedUrl) + "'"
            );
        }

        // Explicit check against the production database name
        if (trimmedUrl.toLowerCase(Locale.ROOT).contains("/" + PRODUCTION_DB)
                && !trimmedUrl.toLowerCase(Locale.ROOT).contains("/" + REQUIRED_DB)) {
            throw new DatabaseSecurityException(
                    "Database security violation: Connection to production database '" + PRODUCTION_DB
                            + "' is strictly forbidden."
            );
        }

        // Separate base URL from query string if '?' is present
        int queryIndex = trimmedUrl.indexOf('?');
        String baseUrl = (queryIndex != -1) ? trimmedUrl.substring(0, queryIndex) : trimmedUrl;
        String queryString = (queryIndex != -1) ? trimmedUrl.substring(queryIndex + 1) : null;

        // Validate base URL against strict approved base URL pattern
        Matcher matcher = APPROVED_BASE_URL_PATTERN.matcher(baseUrl);
        if (!matcher.matches()) {
            String lowerBase = baseUrl.toLowerCase(Locale.ROOT);
            if (!lowerBase.contains("/" + REQUIRED_DB)) {
                throw new DatabaseSecurityException(
                        "Database security violation: Target database must be '" + REQUIRED_DB
                                + "'. URL rejected: '" + sanitizeUrl(trimmedUrl) + "'"
                );
            }
            if (!lowerBase.contains("localhost:3306") && !lowerBase.contains("127.0.0.1:3306")) {
                throw new DatabaseSecurityException(
                        "Database security violation: Host and port must strictly be 'localhost:3306' or '127.0.0.1:3306'. "
                                + "Remote hosts and non-standard ports are rejected: '" + sanitizeUrl(trimmedUrl) + "'"
                );
            }
            if (lowerBase.startsWith("jdbc:mysql://localhost:3306/" + REQUIRED_DB + "/")
                    || lowerBase.startsWith("jdbc:mysql://127.0.0.1:3306/" + REQUIRED_DB + "/")) {
                throw new DatabaseSecurityException(
                        "Database security violation: Extra path segments or path traversal are strictly forbidden: '"
                                + sanitizeUrl(trimmedUrl) + "'"
                );
            }
            throw new DatabaseSecurityException(
                    "Database security violation: Malformed or unapproved JDBC URL: '"
                            + sanitizeUrl(trimmedUrl) + "'"
            );
        }

        // Validate query string if '?' was present
        if (queryIndex != -1) {
            validateQueryString(queryString, trimmedUrl);
        }
    }

    /**
     * Strictly validates the query string against the defined allowlist and malformation rules.
     */
    private static void validateQueryString(String queryString, String fullUrl) {
        if (queryString == null || queryString.isEmpty()) {
            throw new DatabaseSecurityException(
                    "Database security violation: Malformed JDBC URL with empty query string: '"
                            + sanitizeUrl(fullUrl) + "'"
            );
        }

        if (queryString.contains("?")) {
            throw new DatabaseSecurityException(
                    "Database security violation: Malformed query string with multiple '?' characters: '"
                            + sanitizeUrl(fullUrl) + "'"
            );
        }

        if (queryString.startsWith("&") || queryString.endsWith("&") || queryString.contains("&&")) {
            throw new DatabaseSecurityException(
                    "Database security violation: Malformed query string with empty or consecutive separators: '"
                            + sanitizeUrl(fullUrl) + "'"
            );
        }

        String[] pairs = queryString.split("&", -1);
        List<String[]> parsedPairs = new ArrayList<>(pairs.length);
        Set<String> seenParameters = new HashSet<>();

        // Pass 1: Parse and validate syntax, structure, empty elements, and duplicates
        for (String pair : pairs) {
            if (pair.isEmpty()) {
                throw new DatabaseSecurityException(
                        "Database security violation: Malformed query string contains empty parameter: '"
                                + sanitizeUrl(fullUrl) + "'"
                );
            }

            int eqIndex = pair.indexOf('=');
            if (eqIndex == -1) {
                throw new DatabaseSecurityException(
                        "Database security violation: Malformed query parameter without '=': '"
                                + pair + "'"
                );
            }

            String key = pair.substring(0, eqIndex);
            String value = pair.substring(eqIndex + 1);

            if (key.trim().isEmpty()) {
                throw new DatabaseSecurityException(
                        "Database security violation: Empty query parameter name: '" + pair + "'"
                );
            }

            if (value.trim().isEmpty()) {
                throw new DatabaseSecurityException(
                        "Database security violation: Empty query parameter value for parameter '" + key + "'"
                );
            }

            // Check for duplicate parameters (case-insensitive)
            String canonicalKey = key.toLowerCase(Locale.ROOT);
            if (!seenParameters.add(canonicalKey)) {
                throw new DatabaseSecurityException(
                        "Database security violation: Duplicate query parameter detected: '" + key + "'"
                );
            }

            parsedPairs.add(new String[]{key, value, canonicalKey});
        }

        // Pass 2: Validate against allowlist and canonical constraints
        for (String[] entry : parsedPairs) {
            String key = entry[0];
            String value = entry[1];
            String canonicalKey = entry[2];

            // Allowlist verification: check if parameter is allowed
            if (!ALLOWED_QUERY_PARAMETERS.containsKey(canonicalKey)) {
                throw new DatabaseSecurityException(
                        "Database security violation: Unauthorized query parameter '" + key
                                + "'. Query parameters are strictly forbidden by allowlist policy."
                );
            }

            // If allowed, ensure exact canonical key casing matches
            if (!ALLOWED_QUERY_PARAMETERS.containsKey(key)) {
                throw new DatabaseSecurityException(
                        "Database security violation: Query parameter case variation detected for '" + key
                                + "'. Must match exact canonical casing."
                );
            }

            // Validate value against allowed pattern
            Pattern valuePattern = ALLOWED_QUERY_PARAMETERS.get(key);
            if (valuePattern != null && !valuePattern.matcher(value).matches()) {
                throw new DatabaseSecurityException(
                        "Database security violation: Value '" + value + "' for query parameter '" + key
                                + "' violates allowlist constraints."
                );
            }
        }
    }

    /**
     * Validates that the database user strictly equals the approved test application account.
     *
     * @param user database username
     * @throws DatabaseSecurityException if username is missing, root, or unapproved
     */
    public static void validateUser(String user) {
        if (user == null || user.trim().isEmpty()) {
            throw new DatabaseSecurityException(
                    "Database configuration error: Required environment variable 'DB_USER' is missing or blank."
            );
        }

        String trimmedUser = user.trim();

        if ("root".equalsIgnoreCase(trimmedUser)) {
            throw new DatabaseSecurityException(
                    "Database security violation: Privileged user 'root' is strictly forbidden."
            );
        }

        if (!REQUIRED_USER.equals(trimmedUser)) {
            throw new DatabaseSecurityException(
                    "Database security violation: Unauthorized database user '" + trimmedUser
                            + "'. Only '" + REQUIRED_USER + "' is permitted."
            );
        }
    }

    /**
     * Validates that a non-empty password is provided.
     * Note: Password value is strictly never included in exception messages or logs.
     *
     * @param password database password
     * @throws DatabaseSecurityException if password is missing or blank
     */
    public static void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new DatabaseSecurityException(
                    "Database configuration error: Required environment variable 'DB_PASSWORD' is missing or blank."
            );
        }
    }

    /**
     * Strips credentials from URL strings if present before formatting error messages.
     */
    private static String sanitizeUrl(String url) {
        if (url == null) return "null";
        return url.replaceAll("(?<=://)[^@]+@", "***@");
    }
}
