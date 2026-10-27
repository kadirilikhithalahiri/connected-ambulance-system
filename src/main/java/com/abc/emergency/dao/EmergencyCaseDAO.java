package com.abc.emergency.dao;

import com.abc.emergency.model.EmergencyCase;
import com.abc.emergency.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EmergencyCaseDAO {

    private final ThreadLocal<String> lastDatabaseError = new ThreadLocal<>();

    public String getLastDatabaseError() {
        return lastDatabaseError.get();
    }

    public void clearDatabaseError() {
        lastDatabaseError.remove();
    }

    public boolean addEmergencyCase(EmergencyCase emergencyCase) {
        lastDatabaseError.remove();
        try (Connection connection = DBConnection.getConnection()) {
            return addEmergencyCase(connection, emergencyCase);
        } catch (Exception e) {
            lastDatabaseError.set(e.getMessage());
            System.err.println("Error adding emergency case: " + e.getMessage());
            return false;
        }
    }

    public boolean addEmergencyCase(Connection connection, EmergencyCase emergencyCase) throws Exception {
        String sql = "INSERT INTO Emergency_Cases (patient_id, incident_location, emergency_type, symptoms, priority_level, case_status) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, emergencyCase.getPatientId());
            statement.setString(2, emergencyCase.getIncidentLocation());
            statement.setString(3, emergencyCase.getEmergencyType());
            statement.setString(4, emergencyCase.getSymptoms());
            String priority = emergencyCase.getPriorityLevel();
            if (priority != null && !priority.trim().isEmpty()) {
                priority = priority.trim().toUpperCase();
            } else {
                priority = "MODERATE";
            }
            statement.setString(5, priority);
            statement.setString(6, emergencyCase.getCaseStatus());

            int affectedRows = statement.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        emergencyCase.setCaseId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    public EmergencyCase getEmergencyCaseById(int caseId) {
        lastDatabaseError.remove();
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
            lastDatabaseError.set(e.getMessage());
            System.err.println("Error retrieving emergency case: " + e.getMessage());
        }

        return null;
    }

    public List<EmergencyCase> getAllEmergencyCases() {
        lastDatabaseError.remove();
        List<EmergencyCase> cases = new ArrayList<>();

        String sql = "SELECT * FROM Emergency_Cases ORDER BY reported_at DESC";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                cases.add(createEmergencyCase(resultSet));
            }

        } catch (Exception e) {
            lastDatabaseError.set(e.getMessage());
            System.err.println("Error retrieving emergency cases: " + e.getMessage());
        }

        return cases;
    }

    public List<EmergencyCase> getCasesByAssignedUserId(int userId) {
        return getCasesByAssignedUserId(userId, "PARAMEDIC");
    }

    public List<EmergencyCase> getCasesByAssignedUserId(int userId, String role) {
        lastDatabaseError.remove();
        List<EmergencyCase> cases = new ArrayList<>();
        if (userId <= 0) {
            return cases;
        }

        boolean isDriver = "AMBULANCE_DRIVER".equalsIgnoreCase(role);

        // For paramedics, scope strictly by paramedic_id and PARAMEDIC crew assignments.
        // Driver foreign keys are evaluated only when the caller is an AMBULANCE_DRIVER.
        // Limitation: Dispatch_Records links cases to vehicles without timestamped crew history.
        String sql = isDriver ? """
                SELECT DISTINCT ec.case_id, ec.patient_id, ec.incident_location,
                                ec.emergency_type, ec.symptoms, ec.priority_level,
                                ec.case_status, ec.reported_at
                FROM Emergency_Cases ec
                JOIN Dispatch_Records dr ON ec.case_id = dr.case_id
                JOIN Ambulances a ON dr.ambulance_id = a.ambulance_id
                LEFT JOIN Ambulance_Crew ac ON a.ambulance_id = ac.ambulance_id
                                             AND ac.user_id = ?
                                             AND ac.crew_role = 'DRIVER'
                                             AND ac.assigned_to IS NULL
                WHERE (
                       a.driver_id = ?
                    OR (
                        ac.user_id = ?
                        AND (a.driver_id IS NULL OR a.driver_id = 0 OR a.driver_id = ?)
                       )
                )
                ORDER BY ec.reported_at DESC
                """ : """
                SELECT DISTINCT ec.case_id, ec.patient_id, ec.incident_location,
                                ec.emergency_type, ec.symptoms, ec.priority_level,
                                ec.case_status, ec.reported_at
                FROM Emergency_Cases ec
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
                ORDER BY ec.reported_at DESC
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, userId);
            statement.setInt(3, userId);
            statement.setInt(4, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    cases.add(createEmergencyCase(resultSet));
                }
            }

        } catch (Exception e) {
            lastDatabaseError.set(e.getMessage());
            System.err.println("Database error retrieving assigned cases for user " + userId + " (" + role + "): " + e.getMessage());
            // Fail-closed: return empty list on authorization query failure
            return Collections.emptyList();
        }

        return cases;
    }

    public boolean isCaseAssignedToUser(int caseId, int userId) {
        return isCaseAssignedToUser(caseId, userId, "PARAMEDIC");
    }

    public boolean isCaseAssignedToUser(int caseId, int userId, String role) {
        lastDatabaseError.remove();
        if (caseId <= 0 || userId <= 0) {
            return false;
        }

        boolean isDriver = "AMBULANCE_DRIVER".equalsIgnoreCase(role);

        String sql = isDriver ? """
                SELECT COUNT(*)
                FROM Dispatch_Records dr
                JOIN Ambulances a ON dr.ambulance_id = a.ambulance_id
                LEFT JOIN Ambulance_Crew ac ON a.ambulance_id = ac.ambulance_id
                                             AND ac.user_id = ?
                                             AND ac.crew_role = 'DRIVER'
                                             AND ac.assigned_to IS NULL
                WHERE dr.case_id = ?
                  AND (
                       a.driver_id = ?
                    OR (
                        ac.user_id = ?
                        AND (a.driver_id IS NULL OR a.driver_id = 0 OR a.driver_id = ?)
                       )
                  )
                """ : """
                SELECT COUNT(*)
                FROM Dispatch_Records dr
                JOIN Ambulances a ON dr.ambulance_id = a.ambulance_id
                LEFT JOIN Ambulance_Crew ac ON a.ambulance_id = ac.ambulance_id
                                             AND ac.user_id = ?
                                             AND ac.crew_role = 'PARAMEDIC'
                                             AND ac.assigned_to IS NULL
                WHERE dr.case_id = ?
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
            statement.setInt(2, caseId);
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
            System.err.println("Database error verifying case assignment for case " + caseId + ", user " + userId + " (" + role + "): " + e.getMessage());
            // Fail-closed: return false on database or query failure
            return false;
        }

        return false;
    }

    public boolean updateEmergencyCase(EmergencyCase emergencyCase) {
        lastDatabaseError.remove();
        try (Connection connection = DBConnection.getConnection()) {
            return updateEmergencyCase(connection, emergencyCase);
        } catch (Exception e) {
            lastDatabaseError.set(e.getMessage());
            System.err.println("Error updating emergency case: " + e.getMessage());
            return false;
        }
    }

    public boolean updateEmergencyCase(Connection connection, EmergencyCase emergencyCase) throws Exception {
        String sql = "UPDATE Emergency_Cases SET patient_id = ?, incident_location = ?, emergency_type = ?, symptoms = ?, priority_level = COALESCE(?, priority_level), case_status = ? WHERE case_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, emergencyCase.getPatientId());
            statement.setString(2, emergencyCase.getIncidentLocation());
            statement.setString(3, emergencyCase.getEmergencyType());
            statement.setString(4, emergencyCase.getSymptoms());
            String priority = emergencyCase.getPriorityLevel();
            if (priority != null && !priority.trim().isEmpty()) {
                statement.setString(5, priority.trim().toUpperCase());
            } else {
                statement.setString(5, null);
            }
            statement.setString(6, emergencyCase.getCaseStatus());
            statement.setInt(7, emergencyCase.getCaseId());

            return statement.executeUpdate() > 0;
        }
    }

    public boolean updateCaseStatus(int caseId, String status) {
        lastDatabaseError.remove();
        try (Connection connection = DBConnection.getConnection()) {
            return updateCaseStatus(connection, caseId, status);
        } catch (Exception e) {
            lastDatabaseError.set(e.getMessage());
            System.err.println("Error updating case status: " + e.getMessage());
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

    public boolean updateCaseStatusAtomic(int caseId, String expectedCurrentStatus, String targetStatus) {
        lastDatabaseError.remove();
        try (Connection connection = DBConnection.getConnection()) {
            return updateCaseStatusAtomic(connection, caseId, expectedCurrentStatus, targetStatus);
        } catch (Exception e) {
            lastDatabaseError.set(e.getMessage());
            System.err.println("Error atomically updating case status: " + e.getMessage());
            return false;
        }
    }

    public boolean updateCaseStatusAtomic(Connection connection, int caseId, String expectedCurrentStatus, String targetStatus) throws Exception {
        String sql = "UPDATE Emergency_Cases SET case_status = ? WHERE case_id = ? AND case_status = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, targetStatus);
            statement.setInt(2, caseId);
            statement.setString(3, expectedCurrentStatus);

            return statement.executeUpdate() > 0;
        }
    }

    public boolean updateCasePriorityAtomic(int caseId, String expectedPriority, String targetPriority) {
        lastDatabaseError.remove();
        try (Connection connection = DBConnection.getConnection()) {
            return updateCasePriorityAtomic(connection, caseId, expectedPriority, targetPriority);
        } catch (Exception e) {
            lastDatabaseError.set(e.getMessage());
            System.err.println("Error atomically updating case priority: " + e.getMessage());
            return false;
        }
    }

    public boolean updateCasePriorityAtomic(Connection connection, int caseId, String expectedPriority, String targetPriority) throws Exception {
        String sql = "UPDATE Emergency_Cases SET priority_level = ? WHERE case_id = ? AND priority_level = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, targetPriority);
            statement.setInt(2, caseId);
            statement.setString(3, expectedPriority);

            return statement.executeUpdate() > 0;
        }
    }

    public boolean updateCasePriority(int caseId, String priorityLevel) {
        lastDatabaseError.remove();
        try (Connection connection = DBConnection.getConnection()) {
            return updateCasePriority(connection, caseId, priorityLevel);
        } catch (Exception e) {
            lastDatabaseError.set(e.getMessage());
            System.err.println("Error updating case priority: " + e.getMessage());
            return false;
        }
    }

    public boolean updateCasePriority(Connection connection, int caseId, String priorityLevel) throws Exception {
        String sql = "UPDATE Emergency_Cases SET priority_level = ? WHERE case_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, priorityLevel);
            statement.setInt(2, caseId);

            return statement.executeUpdate() > 0;
        }
    }

    public boolean deleteEmergencyCase(int caseId) {
        lastDatabaseError.remove();
        String sql = "DELETE FROM Emergency_Cases WHERE case_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, caseId);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            lastDatabaseError.set(e.getMessage());
            System.err.println("Error deleting emergency case: " + e.getMessage());
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
