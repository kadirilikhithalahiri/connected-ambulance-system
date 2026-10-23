package com.abc.emergency.dao;

import com.abc.emergency.model.Hospital;
import com.abc.emergency.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HospitalDAO {

    public boolean addHospital(Hospital hospital) {

        String sql = """
                INSERT INTO Hospitals
                (hospital_name, location, icu_beds, emergency_beds, ventilators,
                 trauma_available, cardio_available, neuro_available, availability)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, hospital.getHospitalName());
            statement.setString(2, hospital.getLocation());
            statement.setInt(3, hospital.getIcuBeds());
            statement.setInt(4, hospital.getEmergencyBeds());
            statement.setInt(5, hospital.getVentilators());
            statement.setBoolean(6, hospital.isTraumaAvailable());
            statement.setBoolean(7, hospital.isCardioAvailable());
            statement.setBoolean(8, hospital.isNeuroAvailable());
            statement.setString(9, hospital.getAvailability());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Error adding hospital: " + e.getMessage());
            return false;
        }
    }

    public Hospital getHospitalById(int hospitalId) {

        String sql = "SELECT * FROM Hospitals WHERE hospital_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, hospitalId);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {

                    Hospital hospital = new Hospital();

                    hospital.setHospitalId(resultSet.getInt("hospital_id"));
                    hospital.setHospitalName(resultSet.getString("hospital_name"));
                    hospital.setLocation(resultSet.getString("location"));
                    hospital.setIcuBeds(resultSet.getInt("icu_beds"));
                    hospital.setEmergencyBeds(resultSet.getInt("emergency_beds"));
                    hospital.setVentilators(resultSet.getInt("ventilators"));
                    hospital.setTraumaAvailable(resultSet.getBoolean("trauma_available"));
                    hospital.setCardioAvailable(resultSet.getBoolean("cardio_available"));
                    hospital.setNeuroAvailable(resultSet.getBoolean("neuro_available"));
                    hospital.setAvailability(resultSet.getString("availability"));

                    return hospital;
                }
            }

        } catch (SQLException e) {
            System.out.println("Error retrieving hospital: " + e.getMessage());
        }

        return null;
    }

    public List<Hospital> getAllHospitals() {

        List<Hospital> hospitals = new ArrayList<>();

        String sql = "SELECT * FROM Hospitals ORDER BY hospital_id";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {

                Hospital hospital = new Hospital();

                hospital.setHospitalId(resultSet.getInt("hospital_id"));
                hospital.setHospitalName(resultSet.getString("hospital_name"));
                hospital.setLocation(resultSet.getString("location"));
                hospital.setIcuBeds(resultSet.getInt("icu_beds"));
                hospital.setEmergencyBeds(resultSet.getInt("emergency_beds"));
                hospital.setVentilators(resultSet.getInt("ventilators"));
                hospital.setTraumaAvailable(resultSet.getBoolean("trauma_available"));
                hospital.setCardioAvailable(resultSet.getBoolean("cardio_available"));
                hospital.setNeuroAvailable(resultSet.getBoolean("neuro_available"));
                hospital.setAvailability(resultSet.getString("availability"));

                hospitals.add(hospital);
            }

        } catch (SQLException e) {
            System.out.println("Error retrieving hospitals: " + e.getMessage());
        }

        return hospitals;
    }

    public boolean updateHospital(Hospital hospital) {

        String sql = """
                UPDATE Hospitals
                SET hospital_name = ?,
                    location = ?,
                    icu_beds = ?,
                    emergency_beds = ?,
                    ventilators = ?,
                    trauma_available = ?,
                    cardio_available = ?,
                    neuro_available = ?,
                    availability = ?
                WHERE hospital_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, hospital.getHospitalName());
            statement.setString(2, hospital.getLocation());
            statement.setInt(3, hospital.getIcuBeds());
            statement.setInt(4, hospital.getEmergencyBeds());
            statement.setInt(5, hospital.getVentilators());
            statement.setBoolean(6, hospital.isTraumaAvailable());
            statement.setBoolean(7, hospital.isCardioAvailable());
            statement.setBoolean(8, hospital.isNeuroAvailable());
            statement.setString(9, hospital.getAvailability());
            statement.setInt(10, hospital.getHospitalId());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Error updating hospital: " + e.getMessage());
            return false;
        }
    }

    public boolean updateAvailability(int hospitalId, String availability) {

        String sql = "UPDATE Hospitals SET availability = ? WHERE hospital_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, availability);
            statement.setInt(2, hospitalId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Error updating hospital availability: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteHospital(int hospitalId) {

        String sql = "DELETE FROM Hospitals WHERE hospital_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, hospitalId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Error deleting hospital: " + e.getMessage());
            return false;
        }
    }
}
