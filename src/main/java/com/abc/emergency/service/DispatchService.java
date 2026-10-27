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
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class DispatchService {

    private final AmbulanceDAO ambulanceDAO;
    private final DispatchRecordDAO dispatchRecordDAO;
    private final EmergencyCaseDAO emergencyCaseDAO;
    private final AmbulanceService ambulanceService;

    private final ThreadLocal<String> lastErrorMessage = new ThreadLocal<>();
    private final ThreadLocal<Integer> lastGeneratedDispatchId = ThreadLocal.withInitial(() -> -1);

    public DispatchService() {
        this(new AmbulanceDAO(), new DispatchRecordDAO(), new EmergencyCaseDAO(), new AmbulanceService());
    }

    public DispatchService(AmbulanceDAO ambulanceDAO, DispatchRecordDAO dispatchRecordDAO, EmergencyCaseDAO emergencyCaseDAO) {
        this(ambulanceDAO, dispatchRecordDAO, emergencyCaseDAO, new AmbulanceService(ambulanceDAO, new com.abc.emergency.dao.UserDAO()));
    }

    public DispatchService(AmbulanceDAO ambulanceDAO, DispatchRecordDAO dispatchRecordDAO,
                           EmergencyCaseDAO emergencyCaseDAO, AmbulanceService ambulanceService) {
        this.ambulanceDAO = ambulanceDAO;
        this.dispatchRecordDAO = dispatchRecordDAO;
        this.emergencyCaseDAO = emergencyCaseDAO;
        this.ambulanceService = ambulanceService;
    }

    public ServiceResult<DispatchRecord> dispatchAmbulanceResult(int caseId, int ambulanceId, int estimatedArrivalMinutes) {
        lastErrorMessage.remove();
        lastGeneratedDispatchId.set(-1);

        if (!ValidationUtil.isValidId(caseId) || !ValidationUtil.isValidId(ambulanceId)) {
            String err = "Invalid Case ID or Ambulance ID. Both must be positive integers.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        if (estimatedArrivalMinutes < 0 || estimatedArrivalMinutes > 300) {
            String err = "Invalid estimated arrival time. ETA cannot be negative and must be between 0 and 300 minutes.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        EmergencyCase emergencyCase = emergencyCaseDAO.getEmergencyCaseById(caseId);
        if (emergencyCaseDAO.getLastDatabaseError() != null) {
            String err = "An internal database error occurred while retrieving the emergency case.";
            lastErrorMessage.set(err);
            return ServiceResult.databaseError(err);
        }
        if (emergencyCase == null) {
            String err = "Emergency Case ID " + caseId + " does not exist.";
            lastErrorMessage.set(err);
            return ServiceResult.notFound(err);
        }

        String caseStatus = emergencyCase.getCaseStatus();
        if ("COMPLETED".equalsIgnoreCase(caseStatus)) {
            String err = "Emergency Case ID " + caseId + " is already completed.";
            lastErrorMessage.set(err);
            return ServiceResult.conflict(err);
        }
        if ("CANCELLED".equalsIgnoreCase(caseStatus)) {
            String err = "Emergency Case ID " + caseId + " has been cancelled.";
            lastErrorMessage.set(err);
            return ServiceResult.conflict(err);
        }
        if (!"REGISTERED".equalsIgnoreCase(caseStatus)) {
            String err = "Emergency Case ID " + caseId + " is not in REGISTERED state (current status: " + caseStatus + ").";
            lastErrorMessage.set(err);
            return ServiceResult.conflict(err);
        }

        boolean hasActive = dispatchRecordDAO.hasActiveDispatchForCase(caseId);
        if (dispatchRecordDAO.getLastDatabaseError() != null) {
            String err = "An internal database error occurred while checking active dispatches.";
            lastErrorMessage.set(err);
            return ServiceResult.databaseError(err);
        }
        if (hasActive) {
            String err = "Emergency Case ID " + caseId + " already has an active dispatch. Multiple active dispatches are not permitted.";
            lastErrorMessage.set(err);
            return ServiceResult.conflict(err);
        }

        Ambulance ambulance = ambulanceDAO.getAmbulanceById(ambulanceId);
        if (ambulance == null) {
            String err = "Ambulance ID " + ambulanceId + " does not exist.";
            lastErrorMessage.set(err);
            return ServiceResult.notFound(err);
        }

        if (!"AVAILABLE".equalsIgnoreCase(ambulance.getAvailability())) {
            String err = "Ambulance ID " + ambulanceId + " is not available (Current status: " + ambulance.getAvailability() + ").";
            lastErrorMessage.set(err);
            return ServiceResult.conflict(err);
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
                // Step 1: Conditionally update case status from REGISTERED to DISPATCHED
                boolean caseStatusUpdated = emergencyCaseDAO.updateCaseStatusAtomic(connection, caseId, "REGISTERED", "DISPATCHED");
                if (!caseStatusUpdated) {
                    connection.rollback();
                    String err = "Concurrent dispatch conflict: emergency case status was modified by another request.";
                    lastErrorMessage.set(err);
                    return ServiceResult.conflict(err);
                }

                // Step 2: Conditionally update ambulance availability from AVAILABLE to DISPATCHED
                boolean availabilityUpdated = ambulanceDAO.updateAvailabilityAtomic(connection, ambulanceId, "AVAILABLE", "DISPATCHED");
                if (!availabilityUpdated) {
                    connection.rollback();
                    String err = "Concurrent dispatch conflict: ambulance availability was modified by another request.";
                    lastErrorMessage.set(err);
                    return ServiceResult.conflict(err);
                }

                // Step 3: Transactional check for active dispatch
                if (dispatchRecordDAO.hasActiveDispatchForCase(connection, caseId)) {
                    connection.rollback();
                    String err = "Emergency Case ID " + caseId + " already has an active dispatch.";
                    lastErrorMessage.set(err);
                    return ServiceResult.conflict(err);
                }

                // Step 4: Insert dispatch record
                boolean saved = dispatchRecordDAO.addDispatchRecord(connection, record);
                if (!saved || record.getDispatchId() <= 0) {
                    connection.rollback();
                    String err = "DATABASE_ERROR: Failed to create dispatch record.";
                    lastErrorMessage.set(err);
                    return ServiceResult.databaseError(err);
                }

                connection.commit();
                lastGeneratedDispatchId.set(record.getDispatchId());
                lastErrorMessage.remove();
                return ServiceResult.success("Ambulance dispatched successfully.", record);

            } catch (Exception ex) {
                connection.rollback();
                String err = "DATABASE_ERROR: Transaction failed during dispatch: " + ex.getMessage();
                lastErrorMessage.set(err);
                return ServiceResult.databaseError(err);
            }
        } catch (SQLException ex) {
            String err = "DATABASE_ERROR: Database connection error: " + ex.getMessage();
            lastErrorMessage.set(err);
            return ServiceResult.databaseError(err);
        }
    }

    public boolean dispatchAmbulance(int caseId, int ambulanceId, int estimatedArrivalMinutes) {
        return dispatchAmbulanceResult(caseId, ambulanceId, estimatedArrivalMinutes).isSuccess();
    }

    public ServiceResult<DispatchRecord> updateDispatchStatusResult(int dispatchId,
                                                                   String targetStatus,
                                                                   int callerUserId,
                                                                   String callerRole) {
        lastErrorMessage.remove();

        if (!ValidationUtil.isValidId(dispatchId)) {
            String err = "Invalid Dispatch ID. Must be a positive integer.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        if (targetStatus == null || targetStatus.trim().isEmpty()) {
            String err = "Dispatch status cannot be empty.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        String normalizedTarget = targetStatus.toUpperCase();
        if (!isValidStatus(normalizedTarget)) {
            String err = "Invalid dispatch status: " + targetStatus;
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        DispatchRecord record = dispatchRecordDAO.getDispatchById(dispatchId);
        if (dispatchRecordDAO.getLastDatabaseError() != null) {
            String err = "An internal database error occurred while retrieving the dispatch record.";
            lastErrorMessage.set(err);
            return ServiceResult.databaseError(err);
        }
        if (record == null) {
            String err = "Dispatch record not found with ID: " + dispatchId;
            lastErrorMessage.set(err);
            return ServiceResult.notFound(err);
        }

        // Driver scoping: drivers may only update dispatches assigned to their vehicle
        if ("AMBULANCE_DRIVER".equalsIgnoreCase(callerRole)) {
            ServiceResult<Ambulance> driverAmbResult = ambulanceService.getAmbulanceForDriver(callerUserId);
            if (driverAmbResult.getStatus() == ServiceResult.Status.DATABASE_ERROR) {
                return ServiceResult.databaseError(driverAmbResult.getMessage());
            }
            if (!driverAmbResult.isSuccess() || driverAmbResult.getData().getAmbulanceId() != record.getAmbulanceId()) {
                String err = "Dispatch record not found with ID: " + dispatchId;
                lastErrorMessage.set(err);
                return ServiceResult.notFound(err);
            }
        }

        String currentDispatchStatus = record.getDispatchStatus().toUpperCase();
        if (!isAllowedTransition(currentDispatchStatus, normalizedTarget)) {
            String err = "Invalid dispatch status transition from " + currentDispatchStatus + " to " + normalizedTarget + ".";
            lastErrorMessage.set(err);
            return ServiceResult.conflict(err);
        }

        int caseId = record.getCaseId();
        int ambulanceId = record.getAmbulanceId();

        EmergencyCase emergencyCase = emergencyCaseDAO.getEmergencyCaseById(caseId);
        if (emergencyCaseDAO.getLastDatabaseError() != null) {
            String err = "An internal database error occurred while retrieving the emergency case.";
            lastErrorMessage.set(err);
            return ServiceResult.databaseError(err);
        }
        if (emergencyCase == null) {
            String err = "Emergency case not found for dispatch ID: " + dispatchId;
            lastErrorMessage.set(err);
            return ServiceResult.notFound(err);
        }
        String currentCaseStatus = emergencyCase.getCaseStatus();

        // Determine transition expectations
        String expectedCaseStatus;
        String targetCaseStatus;
        boolean releaseAmbulance = false;

        switch (normalizedTarget) {
            case "EN_ROUTE" -> {
                expectedCaseStatus = "DISPATCHED";
                targetCaseStatus = "IN_TRANSIT";
            }
            case "ARRIVED" -> {
                expectedCaseStatus = "IN_TRANSIT";
                targetCaseStatus = "ARRIVED";
            }
            case "COMPLETED" -> {
                expectedCaseStatus = "ARRIVED";
                targetCaseStatus = "COMPLETED";
                releaseAmbulance = true;
            }
            case "CANCELLED" -> {
                releaseAmbulance = true;
                if ("DISPATCHED".equalsIgnoreCase(currentDispatchStatus)) {
                    // Pre-departure cancellation: case returns to REGISTERED so it can be redispatched
                    expectedCaseStatus = "DISPATCHED";
                    targetCaseStatus = "REGISTERED";
                } else {
                    // Post-departure cancellation: emergency is aborted
                    expectedCaseStatus = currentCaseStatus;
                    targetCaseStatus = "CANCELLED";
                }
            }
            default -> {
                String err = "Unsupported target status: " + normalizedTarget;
                lastErrorMessage.set(err);
                return ServiceResult.validationError(err);
            }
        }

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                // 1. Atomic update of dispatch record
                boolean dispatchUpdated;
                if ("ARRIVED".equals(normalizedTarget)) {
                    dispatchUpdated = dispatchRecordDAO.markAsArrivedAtomic(connection, dispatchId, currentDispatchStatus);
                } else {
                    dispatchUpdated = dispatchRecordDAO.updateDispatchStatusAtomic(connection, dispatchId, currentDispatchStatus, normalizedTarget);
                }

                if (!dispatchUpdated) {
                    connection.rollback();
                    String err = "Concurrent update conflict on dispatch record ID: " + dispatchId;
                    lastErrorMessage.set(err);
                    return ServiceResult.conflict(err);
                }

                // 2. Atomic update of emergency case
                boolean caseUpdated = emergencyCaseDAO.updateCaseStatusAtomic(connection, caseId, expectedCaseStatus, targetCaseStatus);
                if (!caseUpdated) {
                    connection.rollback();
                    String err = "Concurrent update conflict on emergency case ID: " + caseId + " (expected status " + expectedCaseStatus + ").";
                    lastErrorMessage.set(err);
                    return ServiceResult.conflict(err);
                }

                // 3. Release ambulance availability if completed or cancelled
                if (releaseAmbulance) {
                    boolean otherActive = dispatchRecordDAO.hasOtherActiveDispatchForAmbulance(connection, ambulanceId, dispatchId);
                    if (!otherActive) {
                        boolean ambulanceReleased = ambulanceDAO.updateAvailabilityAtomic(connection, ambulanceId, "DISPATCHED", "AVAILABLE");
                        if (!ambulanceReleased) {
                            // Ambulance may have already been released
                            ambulanceDAO.updateAvailability(connection, ambulanceId, "AVAILABLE");
                        }
                    }
                }

                connection.commit();
                record.setDispatchStatus(normalizedTarget);
                lastErrorMessage.remove();
                return ServiceResult.success("Dispatch status updated successfully to " + normalizedTarget + ".", record);

            } catch (Exception ex) {
                connection.rollback();
                String err = "DATABASE_ERROR: Database transaction failed during status update: " + ex.getMessage();
                lastErrorMessage.set(err);
                return ServiceResult.databaseError(err);
            }
        } catch (SQLException ex) {
            String err = "DATABASE_ERROR: Database connection error: " + ex.getMessage();
            lastErrorMessage.set(err);
            return ServiceResult.databaseError(err);
        }
    }

    public boolean updateDispatchStatus(int dispatchId, String status) {
        return updateDispatchStatusResult(dispatchId, status, -1, "DISPATCHER").isSuccess();
    }

    public boolean markAsArrived(int dispatchId) {
        return updateDispatchStatus(dispatchId, "ARRIVED");
    }

    public boolean completeDispatch(int dispatchId, int ambulanceId) {
        DispatchRecord record = dispatchRecordDAO.getDispatchById(dispatchId);
        if (record == null) {
            String err = "Dispatch record ID " + dispatchId + " does not exist.";
            lastErrorMessage.set(err);
            System.out.println(err);
            return false;
        }

        if (ambulanceId > 0 && ambulanceId != record.getAmbulanceId()) {
            String err = "Provided Ambulance ID " + ambulanceId + " does not match dispatched Ambulance ID " + record.getAmbulanceId() + ".";
            lastErrorMessage.set(err);
            System.out.println(err);
            return false;
        }

        return updateDispatchStatus(dispatchId, "COMPLETED");
    }

    public boolean completeDispatch(int dispatchId) {
        return completeDispatch(dispatchId, -1);
    }

    public ServiceResult<DispatchRecord> getActiveDispatchForDriver(int driverUserId) {
        ServiceResult<Ambulance> ambResult = ambulanceService.getAmbulanceForDriver(driverUserId);
        if (ambResult.getStatus() == ServiceResult.Status.CONFLICT) {
            return ServiceResult.conflict(ambResult.getMessage());
        }
        if (ambResult.getStatus() == ServiceResult.Status.DATABASE_ERROR) {
            return ServiceResult.databaseError(ambResult.getMessage());
        }
        if (!ambResult.isSuccess()) {
            return ServiceResult.notFound("No ambulance assigned to authenticated driver.");
        }

        int ambulanceId = ambResult.getData().getAmbulanceId();
        List<DispatchRecord> activeList = dispatchRecordDAO.getActiveDispatchesForAmbulance(ambulanceId);
        if (dispatchRecordDAO.getLastDatabaseError() != null) {
            String err = "An internal database error occurred while retrieving active dispatches.";
            lastErrorMessage.set(err);
            return ServiceResult.databaseError(err);
        }

        if (activeList.isEmpty()) {
            return ServiceResult.notFound("No active dispatch found for assigned ambulance ID " + ambulanceId);
        }

        if (activeList.size() > 1) {
            // Ambiguous dispatch state
            return ServiceResult.conflict("Ambiguous dispatch state: multiple active dispatches found for ambulance ID " + ambulanceId);
        }

        return ServiceResult.success("Active dispatch retrieved.", activeList.get(0));
    }

    public ServiceResult<DispatchRecord> getDispatchByIdForCaller(int dispatchId, int callerUserId, String callerRole) {
        if (!ValidationUtil.isValidId(dispatchId)) {
            return ServiceResult.validationError("Invalid dispatch ID. ID must be greater than 0.");
        }

        DispatchRecord record = dispatchRecordDAO.getDispatchById(dispatchId);
        if (dispatchRecordDAO.getLastDatabaseError() != null) {
            String err = "An internal database error occurred while retrieving the dispatch record.";
            lastErrorMessage.set(err);
            return ServiceResult.databaseError(err);
        }
        if (record == null) {
            return ServiceResult.notFound("Dispatch record not found with ID: " + dispatchId);
        }

        if ("AMBULANCE_DRIVER".equalsIgnoreCase(callerRole)) {
            ServiceResult<Ambulance> driverAmbResult = ambulanceService.getAmbulanceForDriver(callerUserId);
            if (driverAmbResult.getStatus() == ServiceResult.Status.DATABASE_ERROR) {
                return ServiceResult.databaseError(driverAmbResult.getMessage());
            }
            if (!driverAmbResult.isSuccess() || driverAmbResult.getData().getAmbulanceId() != record.getAmbulanceId()) {
                return ServiceResult.notFound("Dispatch record not found with ID: " + dispatchId);
            }
        }

        if ("HOSPITAL_ADMIN".equalsIgnoreCase(callerRole)) {
            return ServiceResult.notFound("Dispatch record not found with ID: " + dispatchId);
        }

        return ServiceResult.success("Dispatch record retrieved successfully.", record);
    }

    public ServiceResult<List<DispatchRecord>> getAllDispatchesForCallerResult(String role) {
        if ("ADMIN".equalsIgnoreCase(role) || "DISPATCHER".equalsIgnoreCase(role)) {
            List<DispatchRecord> dispatches = dispatchRecordDAO.getAllDispatchRecords();
            if (dispatchRecordDAO.getLastDatabaseError() != null) {
                String err = "An internal database error occurred while retrieving dispatch records.";
                lastErrorMessage.set(err);
                return ServiceResult.databaseError(err);
            }
            lastErrorMessage.remove();
            return ServiceResult.success("Dispatch records retrieved successfully.", dispatches);
        }
        lastErrorMessage.remove();
        return ServiceResult.success("Dispatch records retrieved successfully.", Collections.emptyList());
    }

    public List<DispatchRecord> getAllDispatchesForCaller(String role) {
        ServiceResult<List<DispatchRecord>> result = getAllDispatchesForCallerResult(role);
        return result.isSuccess() ? result.getData() : Collections.emptyList();
    }

    public DispatchRecord getDispatchById(int dispatchId) {
        if (!ValidationUtil.isValidId(dispatchId)) {
            String err = "Invalid dispatch ID.";
            lastErrorMessage.set(err);
            System.out.println(err);
            return null;
        }

        return dispatchRecordDAO.getDispatchById(dispatchId);
    }

    public List<DispatchRecord> getDispatchesByCaseId(int caseId) {
        if (!ValidationUtil.isValidId(caseId)) {
            String err = "Invalid case ID.";
            lastErrorMessage.set(err);
            System.out.println(err);
            return List.of();
        }

        return dispatchRecordDAO.getDispatchesByCaseId(caseId);
    }

    public ServiceResult<List<DispatchRecord>> getDispatchesByCaseIdForCaller(int caseId, int userId, String role) {
        if (!ValidationUtil.isValidId(caseId)) {
            String err = "Invalid case ID. ID must be greater than 0.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        EmergencyCase emergencyCase = emergencyCaseDAO.getEmergencyCaseById(caseId);
        if (emergencyCaseDAO.getLastDatabaseError() != null) {
            String err = "An internal database error occurred while verifying emergency case.";
            lastErrorMessage.set(err);
            return ServiceResult.databaseError(err);
        }
        if (emergencyCase == null) {
            String err = "Emergency case not found with ID: " + caseId;
            lastErrorMessage.set(err);
            return ServiceResult.notFound(err);
        }

        if ("PARAMEDIC".equalsIgnoreCase(role)) {
            if (userId <= 0) {
                String err = "Emergency case not found with ID: " + caseId;
                lastErrorMessage.set(err);
                return ServiceResult.notFound(err);
            }
            boolean assigned = emergencyCaseDAO.isCaseAssignedToUser(caseId, userId, "PARAMEDIC");
            if (emergencyCaseDAO.getLastDatabaseError() != null) {
                String err = "An internal database error occurred while verifying case assignment.";
                lastErrorMessage.set(err);
                return ServiceResult.databaseError(err);
            }
            if (!assigned) {
                String err = "Emergency case not found with ID: " + caseId;
                lastErrorMessage.set(err);
                return ServiceResult.notFound(err);
            }
        } else if ("AMBULANCE_DRIVER".equalsIgnoreCase(role) || "HOSPITAL_ADMIN".equalsIgnoreCase(role)) {
            String err = "Emergency case not found with ID: " + caseId;
            lastErrorMessage.set(err);
            return ServiceResult.notFound(err);
        }

        List<DispatchRecord> dispatches = dispatchRecordDAO.getDispatchesByCaseId(caseId);
        if (dispatchRecordDAO.getLastDatabaseError() != null) {
            String err = "An internal database error occurred while retrieving dispatch records.";
            lastErrorMessage.set(err);
            return ServiceResult.databaseError(err);
        }

        lastErrorMessage.remove();
        return ServiceResult.success("Dispatches retrieved successfully.", dispatches);
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
        return lastErrorMessage.get();
    }

    public int getLastGeneratedDispatchId() {
        return lastGeneratedDispatchId.get();
    }
}
