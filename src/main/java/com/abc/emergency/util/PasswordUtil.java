package com.abc.emergency.util;

import org.mindrot.jbcrypt.BCrypt;

import java.util.regex.Pattern;

public class PasswordUtil {

    public static final int DEFAULT_LOG_ROUNDS = 12;

    // Standard BCrypt pattern: $2a$, $2b$, or $2y$ followed by two-digit cost factor, $, and 53 chars of salt+hash (total 60 chars)
    private static final Pattern BCRYPT_PATTERN =
            Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    private PasswordUtil() {
        // Utility class
    }

    public static boolean isBCryptHash(String stored) {
        if (stored == null || stored.length() != 60) {
            return false;
        }
        return BCRYPT_PATTERN.matcher(stored).matches();
    }

    public static String hashPassword(String rawPassword) {
        return hashPassword(rawPassword, DEFAULT_LOG_ROUNDS);
    }

    public static String hashPassword(String rawPassword, int logRounds) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }
        if (logRounds < 4 || logRounds > 31) {
            logRounds = DEFAULT_LOG_ROUNDS;
        }
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(logRounds));
    }

    public static boolean checkPassword(String rawPassword, String storedHashOrPlain) {
        if (rawPassword == null || storedHashOrPlain == null) {
            return false;
        }
        if (rawPassword.isEmpty() || storedHashOrPlain.isEmpty()) {
            return false;
        }

        // If the stored value is an attempted or malformed BCrypt hash ($2...), do not treat as plaintext
        if (storedHashOrPlain.startsWith("$2")) {
            if (isBCryptHash(storedHashOrPlain)) {
                try {
                    return BCrypt.checkpw(rawPassword, storedHashOrPlain);
                } catch (Exception e) {
                    // Malformed or invalid BCrypt structure/salt
                    return false;
                }
            }
            return false;
        }

        // Temporary legacy plaintext verification
        return rawPassword.equals(storedHashOrPlain);
    }
}
