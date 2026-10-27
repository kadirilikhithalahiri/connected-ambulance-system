package com.abc.emergency.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private DBConnection() {
        // Utility class
    }

    public static Connection getConnection() throws SQLException {
        return getConnection(System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
    }

    public static Connection getConnection(String url, String user, String password) throws SQLException {
        // Fail-closed validation before any connection attempt
        try {
            DatabaseSecurityValidator.validate(url, user, password);
        } catch (DatabaseSecurityException e) {
            throw new SQLException("Database connection blocked by security policy: " + e.getMessage(), e);
        }

        return DriverManager.getConnection(url, user, password);
    }
}
