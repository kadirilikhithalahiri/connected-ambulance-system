package com.abc.emergency;

import com.abc.emergency.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PasswordCheck {

    public static void main(String[] args) {

        String sql =
                "SELECT username, password_hash " +
                "FROM Users WHERE username='paramedic01'";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet resultSet =
                     statement.executeQuery()) {

            if (resultSet.next()) {

                String storedPassword =
                        resultSet.getString("password_hash");

                String expectedPassword =
                        "paramedic123";

                System.out.println(
                        "Username: "
                                + resultSet.getString("username")
                );

                System.out.println(
                        "Password matches paramedic123: "
                                + expectedPassword.equals(storedPassword)
                );

                System.out.println(
                        "Stored password has leading/trailing spaces: "
                                + !storedPassword.equals(
                                        storedPassword.trim()
                                )
                );

            } else {
                System.out.println("User not found.");
            }

        } catch (Exception e) {
            System.out.println(
                    "Password check failed: "
                            + e.getMessage()
            );
        }
    }
}
