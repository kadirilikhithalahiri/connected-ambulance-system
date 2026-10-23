package com.abc.emergency.service;

import com.abc.emergency.dao.UserDAO;
import com.abc.emergency.model.User;
import com.abc.emergency.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class LoginService {

    private final UserDAO userDAO;

    public LoginService() {
        userDAO = new UserDAO();
    }

    public User login(String username, String password) {

        if (username == null || username.trim().isEmpty()) {
            System.out.println("Username cannot be empty.");
            return null;
        }

        if (password == null || password.trim().isEmpty()) {
            System.out.println("Password cannot be empty.");
            return null;
        }

        User user = userDAO.getUserByUsername(username);

        if (user == null) {
            System.out.println("Invalid username or password.");
            logLoginAttempt(0, "FAILED");
            return null;
        }

        if (!user.isActive()) {
            System.out.println("User account is inactive.");
            logLoginAttempt(user.getUserId(), "FAILED");
            return null;
        }

        if (!user.getPasswordHash().equals(password)) {
            System.out.println("Invalid username or password.");
            logLoginAttempt(user.getUserId(), "FAILED");
            return null;
        }

        logLoginAttempt(user.getUserId(), "SUCCESS");

        System.out.println("Login successful.");
        System.out.println("Welcome, " + user.getFullName());
        System.out.println("Role: " + user.getRole());

        return user;
    }

    public boolean validateRole(User user, String requiredRole) {

        if (user == null || requiredRole == null) {
            return false;
        }

        return user.getRole().equalsIgnoreCase(requiredRole);
    }

    public boolean logout(int userId) {

        if (userId <= 0) {
            return false;
        }

        String sql = """
                UPDATE Login_Log
                SET logout_time = CURRENT_TIMESTAMP
                WHERE user_id = ?
                  AND login_status = 'SUCCESS'
                  AND logout_time IS NULL
                ORDER BY login_time DESC
                LIMIT 1
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            boolean result = statement.executeUpdate() > 0;

            if (result) {
                System.out.println("Logout successful.");
            } else {
                System.out.println("No active login session found.");
            }

            return result;

        } catch (Exception e) {
            System.out.println("Error during logout: " + e.getMessage());
            return false;
        }
    }

    private void logLoginAttempt(int userId, String status) {

        if (userId <= 0) {
            return;
        }

        String sql = """
                INSERT INTO Login_Log
                (user_id, login_status)
                VALUES (?, ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setString(2, status);
            statement.executeUpdate();

        } catch (Exception e) {
            System.out.println("Error logging login attempt: " + e.getMessage());
        }
    }
}