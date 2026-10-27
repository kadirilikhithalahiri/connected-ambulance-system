package com.abc.emergency.util;

/**
 * Thrown when database configuration violates fail-closed security policies
 * or when required database connection environment variables are missing.
 */
public class DatabaseSecurityException extends IllegalStateException {

    public DatabaseSecurityException(String message) {
        super(message);
    }

    public DatabaseSecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}
