package com.abc.emergency.dao;

import com.abc.emergency.model.EmergencyCase;
import com.abc.emergency.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class EmergencyCaseDAO {

    public boolean addEmergencyCase(EmergencyCase emergencyCase) {
        String sql = "INSERT INTO Emergency_Cases (patient_id, incident_location, emergency_type, symptoms, priority_level, case_status) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, emergencyCase.getPatientId());
            statement.setString(2, emergencyCase.getIncidentLocation());
            statement.setString(3, emergencyCase.getEmergencyType());
            statement.setString(4, emergencyCase.getSymptoms());
            statement.setString(5, emergencyCase.getPriorityLevel());
            statement.setString(6, emergencyCase.getCaseStatus());

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Error adding emergency case: " + e.getMessage());
            return false;
        }
    }

    public EmergencyCase getEmergencyCaseById(int caseId) {
        String sql = "SELECT * FROM Emergency_Cases WHERE case_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, caseId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return createEmergencyCase(resultSet);
                }
            }

        } catch (Exception e) {
            System.out.println("Error retrieving emergency case: " + e.getMessage());
        }

        return null;
    }

    public List<EmergencyCase> getAllEmergencyCases() {
        List<EmergencyCase> cases = new ArrayList<>();

        String sql = "SELECT * FROM Emergency_Cases ORDER BY reported_at DESC";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                cases.add(createEmergencyCase(resultSet));
            }

        } catch (Exception e) {
            System.out.println("Error retrieving emergency cases: " + e.getMessage());
        }

        return cases;
    }

    public boolean updateEmergencyCase(EmergencyCase emergencyCase) {
        String sql = "UPDATE Emergency_Cases SET patient_id = ?, incident_location = ?, emergency_type = ?, symptoms = ?, priority_level = ?, case_status = ? WHERE case_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, emergencyCase.getPatientId());
            statement.setString(2, emergencyCase.getIncidentLocation());
            statement.setString(3, emergencyCase.getEmergencyType());
            statement.setString(4, emergencyCase.getSymptoms());
            statement.setString(5, emergencyCase.getPriorityLevel());
            statement.setString(6, emergencyCase.getCaseStatus());
            statement.setInt(7, emergencyCase.getCaseId());

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Error updating emergency case: " + e.getMessage());
            return false;
        }
    }

    public boolean updateCaseStatus(int caseId, String status) {
        try (Connection connection = DBConnection.getConnection()) {
            return updateCaseStatus(connection, caseId, status);
        } catch (Exception e) {
            System.out.println("Error updating case status: " + e.getMessage());
            return false;
        }
    }

    public boolean updateCaseStatus(Connection connection, int caseId, String status) throws Exception {
        String sql = "UPDATE Emergency_Cases SET case_status = ? WHERE case_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setInt(2, caseId);

            return statement.executeUpdate() > 0;
        }
    }

    public boolean updateCasePriority(int caseId, String priorityLevel) {
        String sql = "UPDATE Emergency_Cases SET priority_level = ? WHERE case_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, priorityLevel);
            statement.setInt(2, caseId);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Error updating case priority: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteEmergencyCase(int caseId) {
        String sql = "DELETE FROM Emergency_Cases WHERE case_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, caseId);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Error deleting emergency case: " + e.getMessage());
            return false;
        }
    }

    private EmergencyCase createEmergencyCase(ResultSet resultSet) throws Exception {
        return new EmergencyCase(
                resultSet.getInt("case_id"),
                resultSet.getInt("patient_id"),
                resultSet.getString("incident_location"),
                resultSet.getString("emergency_type"),
                resultSet.getString("symptoms"),
                resultSet.getString("priority_level"),
                resultSet.getString("case_status")
        );
    }
}
