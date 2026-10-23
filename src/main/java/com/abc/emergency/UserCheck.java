package com.abc.emergency;

import com.abc.emergency.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserCheck {

    public static void main(String[] args) {

        String sql =
                "SELECT user_id, username, full_name, role " +
                "FROM Users ORDER BY user_id";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet resultSet =
                     statement.executeQuery()) {

            System.out.println("===== USERS IN DATABASE =====");

            while (resultSet.next()) {

                System.out.println(
                        "ID: " + resultSet.getInt("user_id")
                                + " | Username: "
                                + resultSet.getString("username")
                                + " | Name: "
                                + resultSet.getString("full_name")
                                + " | Role: "
                                + resultSet.getString("role")
                );
            }

            System.out.println("============================");

        } catch (Exception e) {
            System.out.println(
                    "Database check failed: "
                            + e.getMessage()
            );
        }
    }
}
