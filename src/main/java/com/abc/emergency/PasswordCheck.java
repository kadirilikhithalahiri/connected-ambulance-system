package com.abc.emergency;

import com.abc.emergency.util.DBConnection;
import com.abc.emergency.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PasswordCheck {

    public static void main(String[] args) {

        String targetUser = args.length > 0 ? args[0] : "synthetic_user";

        String sql = "SELECT username, password_hash FROM Users WHERE username = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, targetUser);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    String stored = resultSet.getString("password_hash");
                    boolean isBcrypt = PasswordUtil.isBCryptHash(stored);

                    System.out.println("Username: " + resultSet.getString("username"));
                    System.out.println("Format: " + (isBcrypt ? "BCrypt Hash" : "Legacy Plaintext"));
                    System.out.println("Length: " + (stored != null ? stored.length() : 0));
                } else {
                    System.out.println("User not found: " + targetUser);
                }
            }

        } catch (Exception e) {
            System.out.println("Password check failed: " + e.getMessage());
        }
    }
}
