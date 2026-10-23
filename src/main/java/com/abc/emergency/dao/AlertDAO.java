package com.abc.emergency.dao;

import com.abc.emergency.model.Alert;
import com.abc.emergency.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AlertDAO {

    public boolean addAlert(Alert alert) {
        String sql = "INSERT INTO Alerts (case_id, alert_type, alert_message, severity, recipient_role, is_read) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, alert.getCaseId());
            statement.setString(2, alert.getAlertType());
            statement.setString(3, alert.getAlertMessage());
            statement.setString(4, alert.getSeverity());
            statement.setString(5, alert.getRecipientRole());
            statement.setBoolean(6, alert.isRead());

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Error adding alert: " + e.getMessage());
            return false;
        }
    }

    public Alert getAlertById(int alertId) {
        String sql = "SELECT * FROM Alerts WHERE alert_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, alertId);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return createAlert(resultSet);
            }

        } catch (Exception e) {
            System.out.println("Error retrieving alert: " + e.getMessage());
        }

        return null;
    }

    public List<Alert> getAlertsByCaseId(int caseId) {
        List<Alert> alerts = new ArrayList<>();

        String sql = "SELECT * FROM Alerts WHERE case_id = ? ORDER BY created_at DESC";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, caseId);

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                alerts.add(createAlert(resultSet));
            }

        } catch (Exception e) {
            System.out.println("Error retrieving case alerts: " + e.getMessage());
        }

        return alerts;
    }

    public List<Alert> getUnreadAlerts() {
        List<Alert> alerts = new ArrayList<>();

        String sql = "SELECT * FROM Alerts WHERE is_read = FALSE ORDER BY created_at DESC";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                alerts.add(createAlert(resultSet));
            }

        } catch (Exception e) {
            System.out.println("Error retrieving unread alerts: " + e.getMessage());
        }

        return alerts;
    }

    public List<Alert> getAlertsByRecipientRole(String recipientRole) {
        List<Alert> alerts = new ArrayList<>();

        String sql = "SELECT * FROM Alerts WHERE recipient_role = ? ORDER BY created_at DESC";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, recipientRole);

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                alerts.add(createAlert(resultSet));
            }

        } catch (Exception e) {
            System.out.println("Error retrieving recipient alerts: " + e.getMessage());
        }

        return alerts;
    }

    public boolean markAlertAsRead(int alertId) {
        String sql = "UPDATE Alerts SET is_read = TRUE WHERE alert_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, alertId);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Error marking alert as read: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteAlert(int alertId) {
        String sql = "DELETE FROM Alerts WHERE alert_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, alertId);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Error deleting alert: " + e.getMessage());
            return false;
        }
    }

    private Alert createAlert(ResultSet resultSet) throws Exception {
        return new Alert(
                resultSet.getInt("alert_id"),
                resultSet.getInt("case_id"),
                resultSet.getString("alert_type"),
                resultSet.getString("alert_message"),
                resultSet.getString("severity"),
                resultSet.getString("recipient_role"),
                resultSet.getBoolean("is_read")
        );
    }
}
