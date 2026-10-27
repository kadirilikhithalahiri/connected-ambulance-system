package com.abc.emergency.dao;

import com.abc.emergency.model.Patient;
import com.abc.emergency.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PatientDAO {

    private final ThreadLocal<String> lastDatabaseError = new ThreadLocal<>();

    public String getLastDatabaseError() {
        return lastDatabaseError.get();
    }

    public void clearDatabaseError() {
        lastDatabaseError.remove();
    }

    public boolean addPatient(Patient patient) {
        lastDatabaseError.remove();

        String sql = """
                INSERT INTO Patients
                (patient_name, age, gender, blood_group,
                 medical_history, allergies, emergency_contact)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, patient.getPatientName());
            statement.setInt(2, patient.getAge());
            statement.setString(3, patient.getGender());
            statement.setString(4, patient.getBloodGroup());
            statement.setString(5, patient.getMedicalHistory());
            statement.setString(6, patient.getAllergies());
            statement.setString(7, patient.getEmergencyContact());

            int affectedRows = statement.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        patient.setPatientId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            return false;

        } catch (SQLException e) {
            lastDatabaseError.set(e.getMessage());
            System.err.println("Error adding patient: " + e.getMessage());
            return false;
        }
    }

    public Patient getPatientById(int patientId) {
        lastDatabaseError.remove();

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
            lastDatabaseError.set(e.getMessage());
            System.err.println("Error retrieving patient: " + e.getMessage());
        }

        return null;
    }

    public List<Patient> getAllPatients() {
        lastDatabaseError.remove();

        List<Patient> patients = new ArrayList<>();

        String sql = "SELECT * FROM Patients ORDER BY patient_id";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                patients.add(mapPatient(resultSet));
            }

        } catch (SQLException e) {
            lastDatabaseError.set(e.getMessage());
            System.err.println("Error retrieving patients: " + e.getMessage());
        }

        return patients;
    }

    public boolean updatePatient(Patient patient) {
        lastDatabaseError.remove();

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
            lastDatabaseError.set(e.getMessage());
            System.err.println("Error updating patient: " + e.getMessage());
            return false;
        }
    }

    public boolean deletePatient(int patientId) {
        lastDatabaseError.remove();

        String sql = "DELETE FROM Patients WHERE patient_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, patientId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            lastDatabaseError.set(e.getMessage());
            System.err.println("Error deleting patient: " + e.getMessage());
            return false;
        }
    }

    public List<Patient> getPatientsByAssignedUserId(int userId) {
        lastDatabaseError.remove();
        List<Patient> patients = new ArrayList<>();
        if (userId <= 0) {
            return patients;
        }

        // Paramedic object-level scoping: matches primary paramedic_id on vehicle OR active PARAMEDIC crew member.
        // All driver foreign keys (a.driver_id) are strictly excluded to prevent cross-role privilege escalation.
        // Limitation: Dispatch_Records does not store shift/crew snapshot at dispatch time; scoping checks currently assigned vehicles.
        String sql = """
                SELECT DISTINCT p.patient_id, p.patient_name, p.age, p.gender,
                                p.blood_group, p.medical_history, p.allergies, p.emergency_contact
                FROM Patients p
                JOIN Emergency_Cases ec ON p.patient_id = ec.patient_id
                JOIN Dispatch_Records dr ON ec.case_id = dr.case_id
                JOIN Ambulances a ON dr.ambulance_id = a.ambulance_id
                LEFT JOIN Ambulance_Crew ac ON a.ambulance_id = ac.ambulance_id
                                             AND ac.user_id = ?
                                             AND ac.crew_role = 'PARAMEDIC'
                                             AND ac.assigned_to IS NULL
                WHERE (
                       a.paramedic_id = ?
                    OR (
                        ac.user_id = ?
                        AND (a.paramedic_id IS NULL OR a.paramedic_id = 0 OR a.paramedic_id = ?)
                       )
                )
                ORDER BY p.patient_id
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, userId);
            statement.setInt(3, userId);
            statement.setInt(4, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    patients.add(mapPatient(resultSet));
                }
            }
        } catch (Exception e) {
            lastDatabaseError.set(e.getMessage());
            System.err.println("Database error retrieving assigned patients for user " + userId + ": " + e.getMessage());
            // Fail-closed: return empty list on authorization query error
            return Collections.emptyList();
        }

        return patients;
    }

    public boolean isPatientAssignedToUser(int patientId, int userId) {
        lastDatabaseError.remove();
        if (patientId <= 0 || userId <= 0) {
            return false;
        }

        // Validates paramedic assignment to patient via primary vehicle slot or active PARAMEDIC shift crew.
        // Driver foreign key comparisons removed to prevent cross-role authorization.
        String sql = """
                SELECT COUNT(*)
                FROM Emergency_Cases ec
                JOIN Dispatch_Records dr ON ec.case_id = dr.case_id
                JOIN Ambulances a ON dr.ambulance_id = a.ambulance_id
                LEFT JOIN Ambulance_Crew ac ON a.ambulance_id = ac.ambulance_id
                                             AND ac.user_id = ?
                                             AND ac.crew_role = 'PARAMEDIC'
                                             AND ac.assigned_to IS NULL
                WHERE ec.patient_id = ?
                  AND (
                       a.paramedic_id = ?
                    OR (
                        ac.user_id = ?
                        AND (a.paramedic_id IS NULL OR a.paramedic_id = 0 OR a.paramedic_id = ?)
                       )
                  )
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, patientId);
            statement.setInt(3, userId);
            statement.setInt(4, userId);
            statement.setInt(5, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0;
                }
            }
        } catch (Exception e) {
            lastDatabaseError.set(e.getMessage());
            System.err.println("Database error verifying patient assignment for patient " + patientId + ", user " + userId + ": " + e.getMessage());
            // Fail-closed: return false on database or query error
            return false;
        }

        return false;
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
