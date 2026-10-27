package com.abc.emergency.dao;

import com.abc.emergency.model.DispatchRecord;
import com.abc.emergency.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DispatchRecordDAO {

    private final ThreadLocal<String> lastDatabaseError = new ThreadLocal<>();

    public String getLastDatabaseError() {
        return lastDatabaseError.get();
    }

    public void clearDatabaseError() {
        lastDatabaseError.remove();
    }

    public boolean addDispatchRecord(DispatchRecord dispatch) {
        lastDatabaseError.remove();
        try (Connection connection = DBConnection.getConnection()) {
            return addDispatchRecord(connection, dispatch);
        } catch (Exception e) {
            lastDatabaseError.set(e.getMessage());
            System.err.println("Error adding dispatch record: " + e.getMessage());
            return false;
        }
    }

    public boolean addDispatchRecord(Connection connection, DispatchRecord dispatch) throws Exception {
        String sql = "INSERT INTO Dispatch_Records (case_id, ambulance_id, estimated_arrival_minutes, dispatch_status) VALUES (?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, dispatch.getCaseId());
            statement.setInt(2, dispatch.getAmbulanceId());
            statement.setInt(3, dispatch.getEstimatedArrivalMinutes());
            statement.setString(4, dispatch.getDispatchStatus());

            int affectedRows = statement.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        dispatch.setDispatchId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    public DispatchRecord getDispatchById(int dispatchId) {
        lastDatabaseError.remove();
        String sql = "SELECT * FROM Dispatch_Records WHERE dispatch_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, dispatchId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return createDispatchRecord(resultSet);
                }
            }

        } catch (Exception e) {
            lastDatabaseError.set(e.getMessage());
            System.err.println("Error getting dispatch record: " + e.getMessage());
        }

        return null;
    }

    public List<DispatchRecord> getDispatchesByCaseId(int caseId) {
        lastDatabaseError.remove();
        List<DispatchRecord> dispatches = new ArrayList<>();

        String sql = "SELECT * FROM Dispatch_Records WHERE case_id = ? ORDER BY dispatch_time DESC";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, caseId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    dispatches.add(createDispatchRecord(resultSet));
                }
            }

        } catch (Exception e) {
            lastDatabaseError.set(e.getMessage());
            System.err.println("Error getting case dispatches: " + e.getMessage());
        }

        return dispatches;
    }

    public List<DispatchRecord> getAllDispatchRecords() {
        lastDatabaseError.remove();
        List<DispatchRecord> dispatches = new ArrayList<>();

        String sql = "SELECT * FROM Dispatch_Records ORDER BY dispatch_time DESC";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                dispatches.add(createDispatchRecord(resultSet));
            }

        } catch (Exception e) {
            lastDatabaseError.set(e.getMessage());
            System.err.println("Error getting dispatch records: " + e.getMessage());
        }

        return dispatches;
    }

    public boolean hasActiveDispatchForCase(int caseId) {
        lastDatabaseError.remove();
        try (Connection connection = DBConnection.getConnection()) {
            return hasActiveDispatchForCase(connection, caseId);
        } catch (Exception e) {
            lastDatabaseError.set(e.getMessage());
            System.err.println("Error checking active dispatch for case: " + e.getMessage());
            return false;
        }
    }

    public boolean hasActiveDispatchForCase(Connection connection, int caseId) throws Exception {
        String sql = "SELECT COUNT(*) FROM Dispatch_Records WHERE case_id = ? AND dispatch_status IN ('DISPATCHED', 'EN_ROUTE', 'ARRIVED')";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, caseId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0;
                }
            }
        }

        return false;
    }

    public boolean hasOtherActiveDispatchForAmbulance(Connection connection, int ambulanceId, int excludeDispatchId) throws Exception {
        String sql = "SELECT COUNT(*) FROM Dispatch_Records WHERE ambulance_id = ? AND dispatch_id != ? AND dispatch_status IN ('DISPATCHED', 'EN_ROUTE', 'ARRIVED')";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, ambulanceId);
            statement.setInt(2, excludeDispatchId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0;
                }
            }
        }

        return false;
    }

    public boolean hasOtherActiveDispatchForAmbulance(int ambulanceId, int excludeDispatchId) {
        lastDatabaseError.remove();
        try (Connection connection = DBConnection.getConnection()) {
            return hasOtherActiveDispatchForAmbulance(connection, ambulanceId, excludeDispatchId);
        } catch (Exception e) {
            lastDatabaseError.set(e.getMessage());
            System.err.println("Error checking other active dispatch for ambulance: " + e.getMessage());
            return false;
        }
    }

    public boolean updateDispatchStatus(int dispatchId, String status) {
        lastDatabaseError.remove();
        try (Connection connection = DBConnection.getConnection()) {
            return updateDispatchStatus(connection, dispatchId, status);
        } catch (Exception e) {
            lastDatabaseError.set(e.getMessage());
            System.err.println("Error updating dispatch status: " + e.getMessage());
            return false;
        }
    }

    public boolean updateDispatchStatus(Connection connection, int dispatchId, String status) throws Exception {
        String sql = "UPDATE Dispatch_Records SET dispatch_status = ? WHERE dispatch_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setInt(2, dispatchId);

            return statement.executeUpdate() > 0;
        }
    }

    public boolean updateDispatchStatusAtomic(Connection connection, int dispatchId, String expectedStatus, String targetStatus) throws Exception {
        String sql = "UPDATE Dispatch_Records SET dispatch_status = ? WHERE dispatch_id = ? AND dispatch_status = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, targetStatus);
            statement.setInt(2, dispatchId);
            statement.setString(3, expectedStatus);

            return statement.executeUpdate() > 0;
        }
    }

    public boolean markAsArrived(int dispatchId) {
        lastDatabaseError.remove();
        try (Connection connection = DBConnection.getConnection()) {
            return markAsArrived(connection, dispatchId);
        } catch (Exception e) {
            lastDatabaseError.set(e.getMessage());
            System.err.println("Error marking dispatch as arrived: " + e.getMessage());
            return false;
        }
    }

    public boolean markAsArrived(Connection connection, int dispatchId) throws Exception {
        String sql = "UPDATE Dispatch_Records SET dispatch_status = 'ARRIVED', actual_arrival_time = CURRENT_TIMESTAMP WHERE dispatch_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, dispatchId);

            return statement.executeUpdate() > 0;
        }
    }

    public boolean markAsArrivedAtomic(Connection connection, int dispatchId, String expectedStatus) throws Exception {
        String sql = "UPDATE Dispatch_Records SET dispatch_status = 'ARRIVED', actual_arrival_time = CURRENT_TIMESTAMP WHERE dispatch_id = ? AND dispatch_status = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, dispatchId);
            statement.setString(2, expectedStatus);

            return statement.executeUpdate() > 0;
        }
    }

    public List<DispatchRecord> getActiveDispatchesForAmbulance(int ambulanceId) {
        lastDatabaseError.remove();
        List<DispatchRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM Dispatch_Records WHERE ambulance_id = ? AND dispatch_status IN ('DISPATCHED', 'EN_ROUTE', 'ARRIVED') ORDER BY dispatch_id DESC";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, ambulanceId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    list.add(createDispatchRecord(resultSet));
                }
            }

        } catch (Exception e) {
            lastDatabaseError.set(e.getMessage());
            System.err.println("Error getting active dispatches for ambulance: " + e.getMessage());
        }

        return list;
    }

    public boolean deleteDispatchRecord(int dispatchId) {
        lastDatabaseError.remove();
        String sql = "DELETE FROM Dispatch_Records WHERE dispatch_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, dispatchId);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            lastDatabaseError.set(e.getMessage());
            System.err.println("Error deleting dispatch record: " + e.getMessage());
            return false;
        }
    }

    private DispatchRecord createDispatchRecord(ResultSet resultSet) throws Exception {
        return new DispatchRecord(
                resultSet.getInt("dispatch_id"),
                resultSet.getInt("case_id"),
                resultSet.getInt("ambulance_id"),
                resultSet.getInt("estimated_arrival_minutes"),
                resultSet.getString("dispatch_status")
        );
    }
}
