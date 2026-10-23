package com.abc.emergency.dao;

import com.abc.emergency.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HospitalFacilityDAO {

    public boolean addFacility(
            int hospitalId,
            String facilityName,
            int totalUnits,
            int availableUnits,
            String status) {

        String sql = """
                INSERT INTO Hospital_Facilities
                (hospital_id, facility_name, total_units, available_units, status)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, hospitalId);
            statement.setString(2, facilityName);
            statement.setInt(3, totalUnits);
            statement.setInt(4, availableUnits);
            statement.setString(5, status);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Error adding hospital facility: " + e.getMessage());
            return false;
        }
    }

    public List<String> getFacilitiesByHospital(int hospitalId) {

        List<String> facilities = new ArrayList<>();

        String sql = """
                SELECT facility_id, facility_name, total_units,
                       available_units, status
                FROM Hospital_Facilities
                WHERE hospital_id = ?
                ORDER BY facility_id
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, hospitalId);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {

                    String facility =
                            resultSet.getInt("facility_id") + " - "
                            + resultSet.getString("facility_name") + " - "
                            + resultSet.getInt("total_units") + " total - "
                            + resultSet.getInt("available_units") + " available - "
                            + resultSet.getString("status");

                    facilities.add(facility);
                }
            }

        } catch (SQLException e) {
            System.out.println("Error retrieving hospital facilities: " + e.getMessage());
        }

        return facilities;
    }

    public boolean updateFacility(
            int facilityId,
            String facilityName,
            int totalUnits,
            int availableUnits,
            String status) {

        String sql = """
                UPDATE Hospital_Facilities
                SET facility_name = ?,
                    total_units = ?,
                    available_units = ?,
                    status = ?
                WHERE facility_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, facilityName);
            statement.setInt(2, totalUnits);
            statement.setInt(3, availableUnits);
            statement.setString(4, status);
            statement.setInt(5, facilityId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Error updating hospital facility: " + e.getMessage());
            return false;
        }
    }

    public boolean updateFacilityStatus(int facilityId, String status) {

        String sql = """
                UPDATE Hospital_Facilities
                SET status = ?
                WHERE facility_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, status);
            statement.setInt(2, facilityId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Error updating facility status: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteFacility(int facilityId) {

        String sql = "DELETE FROM Hospital_Facilities WHERE facility_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, facilityId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Error deleting hospital facility: " + e.getMessage());
            return false;
        }
    }
}
