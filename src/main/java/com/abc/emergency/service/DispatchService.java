package com.abc.emergency.service;

import com.abc.emergency.dao.AmbulanceDAO;
import com.abc.emergency.dao.DispatchRecordDAO;
import com.abc.emergency.dao.EmergencyCaseDAO;
import com.abc.emergency.model.Ambulance;
import com.abc.emergency.model.DispatchRecord;
import com.abc.emergency.model.EmergencyCase;
import com.abc.emergency.util.DBConnection;
import com.abc.emergency.util.ValidationUtil;

import java.sql.Connection;
import java.util.List;

public class DispatchService {

    private final AmbulanceDAO ambulanceDAO;
    private final DispatchRecordDAO dispatchRecordDAO;
    private final EmergencyCaseDAO emergencyCaseDAO;

    private String lastErrorMessage;
    private int lastGeneratedDispatchId = -1;

    public DispatchService() {
        this.ambulanceDAO = new AmbulanceDAO();
        this.dispatchRecordDAO = new DispatchRecordDAO();
        this.emergencyCaseDAO = new EmergencyCaseDAO();
    }

    public boolean dispatchAmbulance(int caseId, int ambulanceId, int estimatedArrivalMinutes) {
        lastErrorMessage = null;
        lastGeneratedDispatchId = -1;

        if (!ValidationUtil.isValidId(caseId) || !ValidationUtil.isValidId(ambulanceId)) {
            lastErrorMessage = "Invalid Case ID or Ambulance ID. Both must be positive integers.";
            System.out.println(lastErrorMessage);
            return false;
        }

        if (estimatedArrivalMinutes < 0) {
            lastErrorMessage = "Invalid estimated arrival time. ETA cannot be negative.";
            System.out.println(lastErrorMessage);
            return false;
        }

        EmergencyCase emergencyCase = emergencyCaseDAO.getEmergencyCaseById(caseId);
        if (emergencyCase == null) {
            lastErrorMessage = "Emergency Case ID " + caseId + " does not exist.";
            System.out.println(lastErrorMessage);
            return false;
        }

        String caseStatus = emergencyCase.getCaseStatus();
        if ("COMPLETED".equalsIgnoreCase(caseStatus)) {
            lastErrorMessage = "Emergency Case ID " + caseId + " is already completed.";
            System.out.println(lastErrorMessage);
            return false;
        }
        if ("CANCELLED".equalsIgnoreCase(caseStatus)) {
            lastErrorMessage = "Emergency Case ID " + caseId + " has been cancelled.";
            System.out.println(lastErrorMessage);
            return false;
        }

        if (dispatchRecordDAO.hasActiveDispatchForCase(caseId)) {
            lastErrorMessage = "Emergency Case ID " + caseId + " already has an active dispatch. Multiple active dispatches are not permitted.";
            System.out.println(lastErrorMessage);
            return false;
        }

        Ambulance ambulance = ambulanceDAO.getAmbulanceById(ambulanceId);
        if (ambulance == null) {
            lastErrorMessage = "Ambulance ID " + ambulanceId + " does not exist.";
            System.out.println(lastErrorMessage);
            return false;
        }

        if (!"AVAILABLE".equalsIgnoreCase(ambulance.getAvailability())) {
            lastErrorMessage = "Ambulance ID " + ambulanceId + " is not available (Current status: " + ambulance.getAvailability() + ").";
            System.out.println(lastErrorMessage);
            return false;
        }

        DispatchRecord record = new DispatchRecord(
                0,
                caseId,
                ambulanceId,
                estimatedArrivalMinutes,
                "DISPATCHED"
        );

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                boolean saved = dispatchRecordDAO.addDispatchRecord(connection, record);
                if (!saved) {
                    connection.rollback();
                    lastErrorMessage = "Failed to create dispatch record.";
                    return false;
                }

                boolean availabilityUpdated = ambulanceDAO.updateAvailability(connection, ambulanceId, "DISPATCHED");
                if (!availabilityUpdated) {
                    connection.rollback();
                    lastErrorMessage = "Failed to update ambulance availability.";
                    return false;
                }

                boolean caseStatusUpdated = emergencyCaseDAO.updateCaseStatus(connection, caseId, "DISPATCHED");
                if (!caseStatusUpdated) {
                    connection.rollback();
                    lastErrorMessage = "Failed to update emergency case status to DISPATCHED.";
                    return false;
                }

                connection.commit();
                lastGeneratedDispatchId = record.getDispatchId();
                System.out.println("Ambulance dispatched successfully. Dispatch ID: " + lastGeneratedDispatchId);
                return true;

            } catch (Exception ex) {
                connection.rollback();
                lastErrorMessage = "Database transaction failed during dispatch: " + ex.getMessage();
                System.out.println(lastErrorMessage);
                return false;
            }
        } catch (Exception ex) {
            lastErrorMessage = "Database connection error: " + ex.getMessage();
            System.out.println(lastErrorMessage);
            return false;
        }
    }

    public boolean updateDispatchStatus(int dispatchId, String status) {
        lastErrorMessage = null;

        if (!ValidationUtil.isValidId(dispatchId)) {
            lastErrorMessage = "Invalid Dispatch ID. Must be a positive integer.";
            System.out.println(lastErrorMessage);
            return false;
        }

        if (!isValidStatus(status)) {
            lastErrorMessage = "Invalid dispatch status: " + status;
            System.out.println(lastErrorMessage);
            return false;
        }

        DispatchRecord record = dispatchRecordDAO.getDispatchById(dispatchId);
        if (record == null) {
            lastErrorMessage = "Dispatch record ID " + dispatchId + " does not exist.";
            System.out.println(lastErrorMessage);
            return false;
        }

        String currentStatus = record.getDispatchStatus();
        String targetStatus = status.toUpperCase();

        if (!isAllowedTransition(currentStatus, targetStatus)) {
            lastErrorMessage = "Invalid dispatch status transition from " + currentStatus + " to " + targetStatus + ".";
            System.out.println(lastErrorMessage);
            return false;
        }

        String targetCaseStatus = switch (targetStatus) {
            case "EN_ROUTE" -> "IN_TRANSIT";
            case "ARRIVED" -> "ARRIVED";
            case "COMPLETED" -> "COMPLETED";
            case "CANCELLED" -> "CANCELLED";
            default -> null;
        };

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                boolean dispatchUpdated;
                if ("ARRIVED".equals(targetStatus)) {
                    dispatchUpdated = dispatchRecordDAO.markAsArrived(connection, dispatchId);
                } else {
                    dispatchUpdated = dispatchRecordDAO.updateDispatchStatus(connection, dispatchId, targetStatus);
                }

                if (!dispatchUpdated) {
                    connection.rollback();
                    lastErrorMessage = "Failed to update dispatch status.";
                    return false;
                }

                if (targetCaseStatus != null) {
                    boolean caseUpdated = emergencyCaseDAO.updateCaseStatus(connection, record.getCaseId(), targetCaseStatus);
                    if (!caseUpdated) {
                        connection.rollback();
                        lastErrorMessage = "Failed to synchronize emergency case status to " + targetCaseStatus + ".";
                        return false;
                    }
                }

                if ("COMPLETED".equals(targetStatus) || "CANCELLED".equals(targetStatus)) {
                    int ambulanceId = record.getAmbulanceId();
                    boolean otherActive = dispatchRecordDAO.hasOtherActiveDispatchForAmbulance(connection, ambulanceId, dispatchId);
                    if (!otherActive) {
                        boolean ambulanceReleased = ambulanceDAO.updateAvailability(connection, ambulanceId, "AVAILABLE");
                        if (!ambulanceReleased) {
                            connection.rollback();
                            lastErrorMessage = "Failed to release ambulance " + ambulanceId + " to AVAILABLE.";
                            return false;
                        }
                    }
                }

                connection.commit();
                System.out.println("Dispatch status updated successfully to " + targetStatus + ".");
                return true;

            } catch (Exception ex) {
                connection.rollback();
                lastErrorMessage = "Database transaction failed during status update: " + ex.getMessage();
                System.out.println(lastErrorMessage);
                return false;
            }
        } catch (Exception ex) {
            lastErrorMessage = "Database connection error: " + ex.getMessage();
            System.out.println(lastErrorMessage);
            return false;
        }
    }

    public boolean markAsArrived(int dispatchId) {
        lastErrorMessage = null;

        if (!ValidationUtil.isValidId(dispatchId)) {
            lastErrorMessage = "Invalid Dispatch ID. Must be a positive integer.";
            System.out.println(lastErrorMessage);
            return false;
        }

        DispatchRecord record = dispatchRecordDAO.getDispatchById(dispatchId);
        if (record == null) {
            lastErrorMessage = "Dispatch record ID " + dispatchId + " does not exist.";
            System.out.println(lastErrorMessage);
            return false;
        }

        if (!isAllowedTransition(record.getDispatchStatus(), "ARRIVED")) {
            lastErrorMessage = "Cannot mark as ARRIVED: Invalid transition from " + record.getDispatchStatus() + " to ARRIVED.";
            System.out.println(lastErrorMessage);
            return false;
        }

        return updateDispatchStatus(dispatchId, "ARRIVED");
    }

    public boolean completeDispatch(int dispatchId, int ambulanceId) {
        lastErrorMessage = null;

        if (!ValidationUtil.isValidId(dispatchId)) {
            lastErrorMessage = "Invalid Dispatch ID. Must be a positive integer.";
            System.out.println(lastErrorMessage);
            return false;
        }

        DispatchRecord record = dispatchRecordDAO.getDispatchById(dispatchId);
        if (record == null) {
            lastErrorMessage = "Dispatch record ID " + dispatchId + " does not exist.";
            System.out.println(lastErrorMessage);
            return false;
        }

        if (ambulanceId > 0 && ambulanceId != record.getAmbulanceId()) {
            lastErrorMessage = "Provided Ambulance ID " + ambulanceId + " does not match dispatched Ambulance ID " + record.getAmbulanceId() + ".";
            System.out.println(lastErrorMessage);
            return false;
        }

        if (!isAllowedTransition(record.getDispatchStatus(), "COMPLETED")) {
            lastErrorMessage = "Cannot complete dispatch: Invalid transition from " + record.getDispatchStatus() + " to COMPLETED.";
            System.out.println(lastErrorMessage);
            return false;
        }

        return updateDispatchStatus(dispatchId, "COMPLETED");
    }

    public boolean completeDispatch(int dispatchId) {
        return completeDispatch(dispatchId, -1);
    }

    public DispatchRecord getDispatchById(int dispatchId) {
        if (!ValidationUtil.isValidId(dispatchId)) {
            lastErrorMessage = "Invalid dispatch ID.";
            System.out.println(lastErrorMessage);
            return null;
        }

        return dispatchRecordDAO.getDispatchById(dispatchId);
    }

    public List<DispatchRecord> getDispatchesByCaseId(int caseId) {
        if (!ValidationUtil.isValidId(caseId)) {
            lastErrorMessage = "Invalid case ID.";
            System.out.println(lastErrorMessage);
            return List.of();
        }

        return dispatchRecordDAO.getDispatchesByCaseId(caseId);
    }

    public List<DispatchRecord> getAllDispatchRecords() {
        return dispatchRecordDAO.getAllDispatchRecords();
    }

    public boolean isAllowedTransition(String currentStatus, String targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }

        String curr = currentStatus.toUpperCase();
        String target = targetStatus.toUpperCase();

        if (curr.equals(target)) {
            return false;
        }

        return switch (curr) {
            case "DISPATCHED" -> target.equals("EN_ROUTE") || target.equals("CANCELLED");
            case "EN_ROUTE" -> target.equals("ARRIVED") || target.equals("CANCELLED");
            case "ARRIVED" -> target.equals("COMPLETED") || target.equals("CANCELLED");
            case "COMPLETED", "CANCELLED" -> false;
            default -> false;
        };
    }

    public boolean isValidStatus(String status) {
        if (status == null) {
            return false;
        }

        String value = status.toUpperCase();

        return value.equals("DISPATCHED")
                || value.equals("EN_ROUTE")
                || value.equals("ARRIVED")
                || value.equals("COMPLETED")
                || value.equals("CANCELLED");
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public int getLastGeneratedDispatchId() {
        return lastGeneratedDispatchId;
    }
}
