package com.abc.emergency.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DBConnectionTest {

    public static void main(String[] args) {

        String[] requiredTables = {
                "Users",
                "Patients",
                "Ambulances",
                "Ambulance_Crew",
                "Emergency_Cases",
                "Patient_Vitals",
                "AI_Triage",
                "Hospitals",
                "Hospital_Facilities",
                "Ambulance_Tracking",
                "Dispatch_Records",
                "Alerts",
                "Reports",
                "Login_Log",
                "Audit_Log",
                "System_Settings"
        };

        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement()) {

            System.out.println("Database connection successful!");
            System.out.println("Database: " + connection.getCatalog());
            System.out.println();

            int count = 0;

            for (String table : requiredTables) {

                String sql = "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_name = '"
                        + table + "'";

                try (ResultSet resultSet = statement.executeQuery(sql)) {

                    if (resultSet.next() && resultSet.getInt(1) == 1) {
                        System.out.println("[OK] " + table);
                        count++;
                    } else {
                        System.out.println("[MISSING] " + table);
                    }
                }
            }

            System.out.println();
            System.out.println("Required tables: " + requiredTables.length);
            System.out.println("Tables found: " + count);

            if (count == requiredTables.length) {
                System.out.println("Database structure verification successful!");
            } else {
                System.out.println("Database structure verification failed!");
            }

        } catch (Exception e) {
            System.out.println("Database verification failed!");
            System.out.println(e.getMessage());
        }
    }
}