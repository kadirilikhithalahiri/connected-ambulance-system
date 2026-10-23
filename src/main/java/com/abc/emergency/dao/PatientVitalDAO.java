package com.abc.emergency.dao;

import com.abc.emergency.model.PatientVital;
import com.abc.emergency.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class PatientVitalDAO {

    public boolean addVital(PatientVital vital) {
        String sql = "INSERT INTO Patient_Vitals (case_id, heart_rate, systolic_bp, diastolic_bp, spo2, temperature, respiratory_rate, glucose, symptoms) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, vital.getCaseId());
            statement.setInt(2, vital.getHeartRate());
            statement.setInt(3, vital.getSystolicBp());
            statement.setInt(4, vital.getDiastolicBp());
            statement.setDouble(5, vital.getSpo2());
            statement.setDouble(6, vital.getTemperature());
            statement.setInt(7, vital.getRespiratoryRate());
            statement.setDouble(8, vital.getGlucose());
            statement.setString(9, vital.getSymptoms());

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Error adding vital: " + e.getMessage());
            return false;
        }
    }

    public PatientVital getVitalById(int vitalId) {
        String sql = "SELECT * FROM Patient_Vitals WHERE vital_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, vitalId);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return createVital(resultSet);
            }

        } catch (Exception e) {
            System.out.println("Error retrieving vital: " + e.getMessage());
        }

        return null;
    }

    public List<PatientVital> getVitalsByCaseId(int caseId) {
        List<PatientVital> vitals = new ArrayList<>();

        String sql = "SELECT * FROM Patient_Vitals WHERE case_id = ? ORDER BY recorded_at DESC";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, caseId);

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                vitals.add(createVital(resultSet));
            }

        } catch (Exception e) {
            System.out.println("Error retrieving case vitals: " + e.getMessage());
        }

        return vitals;
    }

    public List<PatientVital> getAllVitals() {
        List<PatientVital> vitals = new ArrayList<>();

        String sql = "SELECT * FROM Patient_Vitals ORDER BY recorded_at DESC";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                vitals.add(createVital(resultSet));
            }

        } catch (Exception e) {
            System.out.println("Error retrieving vitals: " + e.getMessage());
        }

        return vitals;
    }

    public boolean updateVital(PatientVital vital) {
        String sql = "UPDATE Patient_Vitals SET case_id = ?, heart_rate = ?, systolic_bp = ?, diastolic_bp = ?, spo2 = ?, temperature = ?, respiratory_rate = ?, glucose = ?, symptoms = ? WHERE vital_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, vital.getCaseId());
            statement.setInt(2, vital.getHeartRate());
            statement.setInt(3, vital.getSystolicBp());
            statement.setInt(4, vital.getDiastolicBp());
            statement.setDouble(5, vital.getSpo2());
            statement.setDouble(6, vital.getTemperature());
            statement.setInt(7, vital.getRespiratoryRate());
            statement.setDouble(8, vital.getGlucose());
            statement.setString(9, vital.getSymptoms());
            statement.setInt(10, vital.getVitalId());

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Error updating vital: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteVital(int vitalId) {
        String sql = "DELETE FROM Patient_Vitals WHERE vital_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, vitalId);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Error deleting vital: " + e.getMessage());
            return false;
        }
    }

    private PatientVital createVital(ResultSet resultSet) throws Exception {
        return new PatientVital(
                resultSet.getInt("vital_id"),
                resultSet.getInt("case_id"),
                resultSet.getInt("heart_rate"),
                resultSet.getInt("systolic_bp"),
                resultSet.getInt("diastolic_bp"),
                resultSet.getDouble("spo2"),
                resultSet.getDouble("temperature"),
                resultSet.getInt("respiratory_rate"),
                resultSet.getDouble("glucose"),
                resultSet.getString("symptoms")
        );
    }
}