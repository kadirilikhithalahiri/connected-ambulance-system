package com.abc.emergency.dao;

import com.abc.emergency.model.AITriage;
import com.abc.emergency.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AITriageDAO {

    public boolean addTriage(AITriage triage) {
        String sql = "INSERT INTO AI_Triage (case_id, priority_level, suggested_specialty, suggested_hospital_id, estimated_arrival_minutes, priority_code, recommendation) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, triage.getCaseId());
            statement.setString(2, triage.getPriorityLevel());
            statement.setString(3, triage.getSuggestedSpecialty());
            statement.setInt(4, triage.getSuggestedHospitalId());
            statement.setInt(5, triage.getEstimatedArrivalMinutes());
            statement.setString(6, triage.getPriorityCode());
            statement.setString(7, triage.getRecommendation());

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Error adding triage: " + e.getMessage());
            return false;
        }
    }

    public AITriage getTriageById(int triageId) {
        String sql = "SELECT * FROM AI_Triage WHERE triage_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, triageId);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return createTriage(resultSet);
            }

        } catch (Exception e) {
            System.out.println("Error retrieving triage: " + e.getMessage());
        }

        return null;
    }

    public AITriage getTriageByCaseId(int caseId) {
        String sql = "SELECT * FROM AI_Triage WHERE case_id = ? ORDER BY created_at DESC LIMIT 1";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, caseId);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return createTriage(resultSet);
            }

        } catch (Exception e) {
            System.out.println("Error retrieving case triage: " + e.getMessage());
        }

        return null;
    }

    public List<AITriage> getAllTriageResults() {
        List<AITriage> triageResults = new ArrayList<>();

        String sql = "SELECT * FROM AI_Triage ORDER BY created_at DESC";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                triageResults.add(createTriage(resultSet));
            }

        } catch (Exception e) {
            System.out.println("Error retrieving triage results: " + e.getMessage());
        }

        return triageResults;
    }

    public boolean updateTriage(AITriage triage) {
        String sql = "UPDATE AI_Triage SET case_id = ?, priority_level = ?, suggested_specialty = ?, suggested_hospital_id = ?, estimated_arrival_minutes = ?, priority_code = ?, recommendation = ? WHERE triage_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, triage.getCaseId());
            statement.setString(2, triage.getPriorityLevel());
            statement.setString(3, triage.getSuggestedSpecialty());
            statement.setInt(4, triage.getSuggestedHospitalId());
            statement.setInt(5, triage.getEstimatedArrivalMinutes());
            statement.setString(6, triage.getPriorityCode());
            statement.setString(7, triage.getRecommendation());
            statement.setInt(8, triage.getTriageId());

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Error updating triage: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteTriage(int triageId) {
        String sql = "DELETE FROM AI_Triage WHERE triage_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, triageId);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Error deleting triage: " + e.getMessage());
            return false;
        }
    }

    private AITriage createTriage(ResultSet resultSet) throws Exception {
        return new AITriage(
                resultSet.getInt("triage_id"),
                resultSet.getInt("case_id"),
                resultSet.getString("priority_level"),
                resultSet.getString("suggested_specialty"),
                resultSet.getInt("suggested_hospital_id"),
                resultSet.getInt("estimated_arrival_minutes"),
                resultSet.getString("priority_code"),
                resultSet.getString("recommendation")
        );
    }
}