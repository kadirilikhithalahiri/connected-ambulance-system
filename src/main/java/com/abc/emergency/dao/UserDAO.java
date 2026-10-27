package com.abc.emergency.dao;

import com.abc.emergency.model.User;
import com.abc.emergency.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    public boolean addUser(User user) {

        String sql = """
                INSERT INTO Users
                (username, password_hash, full_name, role, phone, active)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPasswordHash());
            statement.setString(3, user.getFullName());
            statement.setString(4, user.getRole());
            statement.setString(5, user.getPhone());
            statement.setBoolean(6, user.isActive());

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Error adding user: " + e.getMessage());
            return false;
        }
    }

    public User getUserByUsername(String username) {

        String sql = """
                SELECT user_id, username, password_hash,
                       full_name, role, phone, active
                FROM Users
                WHERE username = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {

                    return new User(
                            resultSet.getInt("user_id"),
                            resultSet.getString("username"),
                            resultSet.getString("password_hash"),
                            resultSet.getString("full_name"),
                            resultSet.getString("role"),
                            resultSet.getString("phone"),
                            resultSet.getBoolean("active")
                    );
                }
            }

        } catch (Exception e) {
            System.out.println("Error retrieving user: " + e.getMessage());
        }

        return null;
    }

    public User getUserById(int userId) {

        String sql = "SELECT * FROM Users WHERE user_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {

                    User user = new User();

                    user.setUserId(resultSet.getInt("user_id"));
                    user.setUsername(resultSet.getString("username"));
                    user.setPasswordHash(resultSet.getString("password_hash"));
                    user.setFullName(resultSet.getString("full_name"));
                    user.setRole(resultSet.getString("role"));
                    user.setPhone(resultSet.getString("phone"));
                    user.setActive(resultSet.getBoolean("active"));

                    return user;
                }
            }

        } catch (SQLException e) {
            System.out.println("Error retrieving user by ID: " + e.getMessage());
        }

        return null;
    }

    public User getUserByRole(String role) {

        if (role == null || role.trim().isEmpty()) {
            return null;
        }

        String sql = """
                SELECT user_id, username, password_hash,
                       full_name, role, phone, active
                FROM Users
                WHERE role = ?
                  AND active = TRUE
                LIMIT 1
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, role.toUpperCase());

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {

                    return new User(
                            resultSet.getInt("user_id"),
                            resultSet.getString("username"),
                            resultSet.getString("password_hash"),
                            resultSet.getString("full_name"),
                            resultSet.getString("role"),
                            resultSet.getString("phone"),
                            resultSet.getBoolean("active")
                    );
                }
            }

        } catch (SQLException e) {
            System.out.println("Error retrieving user by role: " + e.getMessage());
        }

        return null;
    }

    public boolean updatePasswordHash(int userId, String newHash) {
        if (userId <= 0 || newHash == null || newHash.trim().isEmpty()) {
            return false;
        }

        String sql = "UPDATE Users SET password_hash = ? WHERE user_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, newHash);
            statement.setInt(2, userId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Error updating password: " + e.getMessage());
            return false;
        }
    }

    public boolean updatePasswordHashConditional(int userId, String expectedOldHashOrPlain, String newHash) {
        if (userId <= 0 || newHash == null || newHash.trim().isEmpty() || expectedOldHashOrPlain == null) {
            return false;
        }

        String sql = "UPDATE Users SET password_hash = ? WHERE user_id = ? AND password_hash = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, newHash);
            statement.setInt(2, userId);
            statement.setString(3, expectedOldHashOrPlain);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Error conditionally updating password: " + e.getMessage());
            return false;
        }
    }
}
