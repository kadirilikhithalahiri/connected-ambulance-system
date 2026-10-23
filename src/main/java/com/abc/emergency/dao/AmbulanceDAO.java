package com.abc.emergency.dao;

import com.abc.emergency.model.Ambulance;
import com.abc.emergency.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AmbulanceDAO {

    public boolean addAmbulance(Ambulance ambulance) {

        String sql = """
                INSERT INTO Ambulances
                (vehicle_number, driver_id, paramedic_id, gps_latitude,
                 gps_longitude, equipment, availability)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, ambulance.getVehicleNumber());
            statement.setInt(2, ambulance.getDriverId());
            statement.setInt(3, ambulance.getParamedicId());
            statement.setDouble(4, ambulance.getGpsLatitude());
            statement.setDouble(5, ambulance.getGpsLongitude());
            statement.setString(6, ambulance.getEquipment());
            statement.setString(7, ambulance.getAvailability());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Error adding ambulance: " + e.getMessage());
            return false;
        }
    }

    public Ambulance getAmbulanceById(int ambulanceId) {

        String sql = "SELECT * FROM Ambulances WHERE ambulance_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, ambulanceId);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    Ambulance ambulance = new Ambulance();

                    ambulance.setAmbulanceId(resultSet.getInt("ambulance_id"));
                    ambulance.setVehicleNumber(resultSet.getString("vehicle_number"));
                    ambulance.setDriverId(resultSet.getInt("driver_id"));
                    ambulance.setParamedicId(resultSet.getInt("paramedic_id"));
                    ambulance.setGpsLatitude(resultSet.getDouble("gps_latitude"));
                    ambulance.setGpsLongitude(resultSet.getDouble("gps_longitude"));
                    ambulance.setEquipment(resultSet.getString("equipment"));
                    ambulance.setAvailability(resultSet.getString("availability"));

                    return ambulance;
                }
            }

        } catch (SQLException e) {
            System.out.println("Error retrieving ambulance: " + e.getMessage());
        }

        return null;
    }

    public List<Ambulance> getAllAmbulances() {

        List<Ambulance> ambulances = new ArrayList<>();

        String sql = "SELECT * FROM Ambulances ORDER BY ambulance_id";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {

                Ambulance ambulance = new Ambulance();

                ambulance.setAmbulanceId(resultSet.getInt("ambulance_id"));
                ambulance.setVehicleNumber(resultSet.getString("vehicle_number"));
                ambulance.setDriverId(resultSet.getInt("driver_id"));
                ambulance.setParamedicId(resultSet.getInt("paramedic_id"));
                ambulance.setGpsLatitude(resultSet.getDouble("gps_latitude"));
                ambulance.setGpsLongitude(resultSet.getDouble("gps_longitude"));
                ambulance.setEquipment(resultSet.getString("equipment"));
                ambulance.setAvailability(resultSet.getString("availability"));

                ambulances.add(ambulance);
            }

        } catch (SQLException e) {
            System.out.println("Error retrieving ambulances: " + e.getMessage());
        }

        return ambulances;
    }

    public boolean updateAmbulance(Ambulance ambulance) {

        String sql = """
                UPDATE Ambulances
                SET vehicle_number = ?,
                    driver_id = ?,
                    paramedic_id = ?,
                    gps_latitude = ?,
                    gps_longitude = ?,
                    equipment = ?,
                    availability = ?
                WHERE ambulance_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, ambulance.getVehicleNumber());
            statement.setInt(2, ambulance.getDriverId());
            statement.setInt(3, ambulance.getParamedicId());
            statement.setDouble(4, ambulance.getGpsLatitude());
            statement.setDouble(5, ambulance.getGpsLongitude());
            statement.setString(6, ambulance.getEquipment());
            statement.setString(7, ambulance.getAvailability());
            statement.setInt(8, ambulance.getAmbulanceId());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Error updating ambulance: " + e.getMessage());
            return false;
        }
    }

    public boolean updateAvailability(int ambulanceId, String availability) {

        try (Connection connection = DBConnection.getConnection()) {
            return updateAvailability(connection, ambulanceId, availability);
        } catch (SQLException e) {
            System.out.println("Error updating ambulance availability: " + e.getMessage());
            return false;
        }
    }

    public boolean updateAvailability(Connection connection, int ambulanceId, String availability) throws SQLException {

        String sql = "UPDATE Ambulances SET availability = ? WHERE ambulance_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, availability);
            statement.setInt(2, ambulanceId);

            return statement.executeUpdate() > 0;
        }
    }

    public boolean deleteAmbulance(int ambulanceId) {

        String sql = "DELETE FROM Ambulances WHERE ambulance_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, ambulanceId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Error deleting ambulance: " + e.getMessage());
            return false;
        }
    }
}
