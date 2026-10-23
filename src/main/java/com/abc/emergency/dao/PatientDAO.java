package com.abc.emergency.dao;

import com.abc.emergency.model.Patient;
import com.abc.emergency.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PatientDAO {

    public boolean addPatient(Patient patient) {

        String sql = """
                INSERT INTO Patients
                (patient_name, age, gender, blood_group,
                 medical_history, allergies, emergency_contact)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, patient.getPatientName());
            statement.setInt(2, patient.getAge());
            statement.setString(3, patient.getGender());
            statement.setString(4, patient.getBloodGroup());
            statement.setString(5, patient.getMedicalHistory());
            statement.setString(6, patient.getAllergies());
            statement.setString(7, patient.getEmergencyContact());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Error adding patient: " + e.getMessage());
            return false;
        }
    }

    public Patient getPatientById(int patientId) {

        String sql = "SELECT * FROM Patients WHERE patient_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, patientId);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapPatient(resultSet);
                }
            }

        } catch (SQLException e) {
            System.out.println("Error retrieving patient: " + e.getMessage());
        }

        return null;
    }

    public List<Patient> getAllPatients() {

        List<Patient> patients = new ArrayList<>();

        String sql = "SELECT * FROM Patients ORDER BY patient_id";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                patients.add(mapPatient(resultSet));
            }

        } catch (SQLException e) {
            System.out.println("Error retrieving patients: " + e.getMessage());
        }

        return patients;
    }

    public boolean updatePatient(Patient patient) {

        String sql = """
                UPDATE Patients
                SET patient_name = ?,
                    age = ?,
                    gender = ?,
                    blood_group = ?,
                    medical_history = ?,
                    allergies = ?,
                    emergency_contact = ?
                WHERE patient_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, patient.getPatientName());
            statement.setInt(2, patient.getAge());
            statement.setString(3, patient.getGender());
            statement.setString(4, patient.getBloodGroup());
            statement.setString(5, patient.getMedicalHistory());
            statement.setString(6, patient.getAllergies());
            statement.setString(7, patient.getEmergencyContact());
            statement.setInt(8, patient.getPatientId());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Error updating patient: " + e.getMessage());
            return false;
        }
    }

    public boolean deletePatient(int patientId) {

        String sql = "DELETE FROM Patients WHERE patient_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, patientId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Error deleting patient: " + e.getMessage());
            return false;
        }
    }

    private Patient mapPatient(ResultSet resultSet) throws SQLException {

        Patient patient = new Patient();

        patient.setPatientId(resultSet.getInt("patient_id"));
        patient.setPatientName(resultSet.getString("patient_name"));
        patient.setAge(resultSet.getInt("age"));
        patient.setGender(resultSet.getString("gender"));
        patient.setBloodGroup(resultSet.getString("blood_group"));
        patient.setMedicalHistory(resultSet.getString("medical_history"));
        patient.setAllergies(resultSet.getString("allergies"));
        patient.setEmergencyContact(resultSet.getString("emergency_contact"));

        return patient;
    }
}