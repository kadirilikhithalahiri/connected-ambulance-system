package com.abc.emergency.dao;

import com.abc.emergency.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AmbulanceCrewDAO {

    public boolean addCrewMember(int ambulanceId, int userId, String crewRole) {
        String sql = "INSERT INTO Ambulance_Crew (ambulance_id, user_id, crew_role) VALUES (?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, ambulanceId);
            statement.setInt(2, userId);
            statement.setString(3, crewRole);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Error adding crew member: " + e.getMessage());
            return false;
        }
    }

    public List<String> getCrewMembersByAmbulance(int ambulanceId) {
        List<String> crewMembers = new ArrayList<>();

        String sql = """
                SELECT u.user_id, u.full_name, u.role, ac.crew_role
                FROM Ambulance_Crew ac
                JOIN Users u ON ac.user_id = u.user_id
                WHERE ac.ambulance_id = ? AND ac.assigned_to IS NULL
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, ambulanceId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String member = resultSet.getInt("user_id") + " - "
                            + resultSet.getString("full_name") + " - "
                            + resultSet.getString("role") + " - "
                            + resultSet.getString("crew_role");

                    crewMembers.add(member);
                }
            }

        } catch (SQLException e) {
            System.out.println("Error retrieving crew members: " + e.getMessage());
        }

        return crewMembers;
    }

    public boolean removeCrewMember(int crewId) {
        String sql = "UPDATE Ambulance_Crew SET assigned_to = CURRENT_TIMESTAMP WHERE crew_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, crewId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Error removing crew member: " + e.getMessage());
            return false;
        }
    }

    public List<String> getAllCrewAssignments() {
        List<String> assignments = new ArrayList<>();

        String sql = """
                SELECT ac.crew_id, a.vehicle_number, u.full_name, ac.crew_role
                FROM Ambulance_Crew ac
                JOIN Ambulances a ON ac.ambulance_id = a.ambulance_id
                JOIN Users u ON ac.user_id = u.user_id
                WHERE ac.assigned_to IS NULL
                ORDER BY a.vehicle_number
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                String assignment = resultSet.getInt("crew_id") + " - "
                        + resultSet.getString("vehicle_number") + " - "
                        + resultSet.getString("full_name") + " - "
                        + resultSet.getString("crew_role");

                assignments.add(assignment);
            }

        } catch (SQLException e) {
            System.out.println("Error retrieving crew assignments: " + e.getMessage());
        }

        return assignments;
    }
}