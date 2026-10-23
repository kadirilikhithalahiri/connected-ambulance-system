package com.abc.emergency.dao;

import com.abc.emergency.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AmbulanceTrackingDAO {

    public boolean addTracking(int ambulanceId, double latitude,
                               double longitude, double speed, String status) {

        String sql = "INSERT INTO Ambulance_Tracking " +
                "(ambulance_id, latitude, longitude, speed, status) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, ambulanceId);
            statement.setDouble(2, latitude);
            statement.setDouble(3, longitude);
            statement.setDouble(4, speed);
            statement.setString(5, status);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Error adding tracking data: " + e.getMessage());
            return false;
        }
    }

    public void getLatestLocation(int ambulanceId) {

        String sql = "SELECT latitude, longitude, speed, status, recorded_at " +
                "FROM Ambulance_Tracking " +
                "WHERE ambulance_id = ? " +
                "ORDER BY recorded_at DESC LIMIT 1";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, ambulanceId);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                System.out.println("Ambulance ID: " + ambulanceId);
                System.out.println("Latitude: " + resultSet.getDouble("latitude"));
                System.out.println("Longitude: " + resultSet.getDouble("longitude"));
                System.out.println("Speed: " + resultSet.getDouble("speed"));
                System.out.println("Status: " + resultSet.getString("status"));
                System.out.println("Recorded At: " + resultSet.getTimestamp("recorded_at"));
            } else {
                System.out.println("No tracking data found.");
            }

        } catch (Exception e) {
            System.out.println("Error getting latest location: " + e.getMessage());
        }
    }

    public void getTrackingHistory(int ambulanceId) {

        String sql = "SELECT latitude, longitude, speed, status, recorded_at " +
                "FROM Ambulance_Tracking " +
                "WHERE ambulance_id = ? " +
                "ORDER BY recorded_at DESC";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, ambulanceId);

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                System.out.println(
                        resultSet.getDouble("latitude") + " | " +
                        resultSet.getDouble("longitude") + " | " +
                        resultSet.getDouble("speed") + " | " +
                        resultSet.getString("status") + " | " +
                        resultSet.getTimestamp("recorded_at")
                );
            }

        } catch (Exception e) {
            System.out.println("Error getting tracking history: " + e.getMessage());
        }
    }

    public boolean updateAmbulanceLocation(int ambulanceId,
                                           double latitude,
                                           double longitude) {

        String sql = "UPDATE Ambulances " +
                "SET gps_latitude = ?, gps_longitude = ? " +
                "WHERE ambulance_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setDouble(1, latitude);
            statement.setDouble(2, longitude);
            statement.setInt(3, ambulanceId);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Error updating ambulance location: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteTracking(int trackingId) {

        String sql = "DELETE FROM Ambulance_Tracking WHERE tracking_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, trackingId);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Error deleting tracking record: " + e.getMessage());
            return false;
        }
    }
}