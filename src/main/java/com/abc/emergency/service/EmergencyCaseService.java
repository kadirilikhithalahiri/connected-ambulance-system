package com.abc.emergency.service;

import com.abc.emergency.dao.EmergencyCaseDAO;
import com.abc.emergency.dao.PatientDAO;
import com.abc.emergency.dto.CaseTriageRequest;
import com.abc.emergency.dto.CaseTriageResponse;
import com.abc.emergency.model.EmergencyCase;
import com.abc.emergency.model.Patient;
import com.abc.emergency.model.PatientVital;
import com.abc.emergency.util.ValidationUtil;

import java.util.Collections;
import java.util.List;

public class EmergencyCaseService {

    private final EmergencyCaseDAO emergencyCaseDAO;
    private final PatientDAO patientDAO;
    private final TriageService triageService;
    private final ThreadLocal<String> lastErrorMessage = new ThreadLocal<>();

    public EmergencyCaseService() {
        this(new EmergencyCaseDAO(), new PatientDAO(), new TriageService());
    }

    public EmergencyCaseService(EmergencyCaseDAO emergencyCaseDAO, PatientDAO patientDAO) {
        this(emergencyCaseDAO, patientDAO, new TriageService());
    }

    public EmergencyCaseService(EmergencyCaseDAO emergencyCaseDAO, PatientDAO patientDAO, TriageService triageService) {
        this.emergencyCaseDAO = emergencyCaseDAO;
        this.patientDAO = patientDAO;
        this.triageService = triageService != null ? triageService : new TriageService();
    }

    public static int getPriorityRank(String priority) {
        if (priority == null) {
            return 0;
        }
        return switch (priority.trim().toUpperCase()) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "MODERATE" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    public static boolean isPriorityEscalation(String currentPriority, String calculatedPriority) {
        return getPriorityRank(calculatedPriority) > getPriorityRank(currentPriority);
    }

    public static boolean isValidPriority(String priority) {
        if (priority == null) {
            return false;
        }
        String p = priority.trim().toUpperCase();
        return p.equals("CRITICAL")
                || p.equals("HIGH")
                || p.equals("MODERATE")
                || p.equals("LOW");
    }

    public ServiceResult<EmergencyCase> createEmergencyCaseResult(int patientId,
                                                                 String incidentLocation,
                                                                 String emergencyType,
                                                                 String symptoms) {
        return createEmergencyCaseResult(patientId, incidentLocation, emergencyType, symptoms, null);
    }

    public ServiceResult<EmergencyCase> createEmergencyCaseResult(int patientId,
                                                                 String incidentLocation,
                                                                 String emergencyType,
                                                                 String symptoms,
                                                                 String priorityLevel) {

        if (!ValidationUtil.isValidId(patientId)) {
            String err = "Invalid patient ID. ID must be greater than 0.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        if (incidentLocation == null || incidentLocation.trim().isEmpty()) {
            String err = "Incident location cannot be empty.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        if (emergencyType == null || emergencyType.trim().isEmpty()) {
            String err = "Emergency type cannot be empty.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        if (symptoms == null || symptoms.trim().isEmpty()) {
            String err = "Symptoms cannot be empty.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        String resolvedPriority;
        if (priorityLevel != null && !priorityLevel.trim().isEmpty()) {
            String normalized = priorityLevel.trim().toUpperCase();
            if (!isValidPriority(normalized)) {
                String err = "Invalid priority level: " + priorityLevel + ". Allowed values: CRITICAL, HIGH, MODERATE, LOW.";
                lastErrorMessage.set(err);
                return ServiceResult.validationError(err);
            }
            resolvedPriority = normalized;
        } else if (priorityLevel != null && priorityLevel.trim().isEmpty()) {
            String err = "Invalid priority level: " + priorityLevel + ". Allowed values: CRITICAL, HIGH, MODERATE, LOW.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        } else {
            resolvedPriority = "MODERATE";
        }

        Patient patient = patientDAO.getPatientById(patientId);
        if (patientDAO.getLastDatabaseError() != null) {
            String err = "An internal database error occurred while verifying patient.";
            lastErrorMessage.set(err);
            return ServiceResult.databaseError(err);
        }

        if (patient == null) {
            String err = "Patient with ID " + patientId + " not found.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        EmergencyCase emergencyCase = new EmergencyCase();

        emergencyCase.setPatientId(patientId);
        emergencyCase.setIncidentLocation(incidentLocation.trim());
        emergencyCase.setEmergencyType(emergencyType.trim());
        emergencyCase.setSymptoms(symptoms.trim());
        emergencyCase.setPriorityLevel(resolvedPriority);
        emergencyCase.setCaseStatus("REGISTERED");

        boolean result = emergencyCaseDAO.addEmergencyCase(emergencyCase);

        if (result && emergencyCase.getCaseId() > 0) {
            lastErrorMessage.remove();
            return ServiceResult.success("Emergency case registered successfully.", emergencyCase);
        } else {
            String err = "DATABASE_ERROR: Unable to save emergency case record.";
            lastErrorMessage.set(err);
            return ServiceResult.databaseError(err);
        }
    }

    public EmergencyCase createEmergencyCase(int patientId,
                                            String incidentLocation,
                                            String emergencyType,
                                            String symptoms,
                                            String priorityLevel) {
        ServiceResult<EmergencyCase> result = createEmergencyCaseResult(patientId, incidentLocation, emergencyType, symptoms, priorityLevel);
        return result.isSuccess() ? result.getData() : null;
    }

    public EmergencyCase createEmergencyCase(int patientId,
                                            String incidentLocation,
                                            String emergencyType,
                                            String symptoms) {
        return createEmergencyCase(patientId, incidentLocation, emergencyType, symptoms, null);
    }

    public boolean registerEmergencyCase(int patientId,
                                         String incidentLocation,
                                         String emergencyType,
                                         String symptoms,
                                         String priorityLevel) {
        return createEmergencyCaseResult(patientId, incidentLocation, emergencyType, symptoms, priorityLevel).isSuccess();
    }

    public boolean registerEmergencyCase(int patientId,
                                         String incidentLocation,
                                         String emergencyType,
                                         String symptoms) {
        return registerEmergencyCase(patientId, incidentLocation, emergencyType, symptoms, null);
    }

    public EmergencyCase getEmergencyCaseById(int caseId) {
        if (!ValidationUtil.isValidId(caseId)) {
            lastErrorMessage.set("Invalid case ID. ID must be greater than 0.");
            return null;
        }

        return emergencyCaseDAO.getEmergencyCaseById(caseId);
    }

    public List<EmergencyCase> getAllEmergencyCases() {
        return emergencyCaseDAO.getAllEmergencyCases();
    }

    public List<EmergencyCase> getCasesForCaller(int userId, String role) {
        ServiceResult<List<EmergencyCase>> result = getCasesForCallerResult(userId, role);
        return result.isSuccess() ? result.getData() : Collections.emptyList();
    }

    public ServiceResult<List<EmergencyCase>> getCasesForCallerResult(int userId, String role) {
        List<EmergencyCase> cases;

        if ("AMBULANCE_DRIVER".equalsIgnoreCase(role) || "PARAMEDIC".equalsIgnoreCase(role)) {
            if (userId <= 0) {
                lastErrorMessage.remove();
                return ServiceResult.success("Emergency cases retrieved successfully.", Collections.emptyList());
            }
            cases = emergencyCaseDAO.getCasesByAssignedUserId(userId, role);
        } else if ("HOSPITAL_ADMIN".equalsIgnoreCase(role)) {
            // Schema does not contain user-to-hospital link; access is restricted
            lastErrorMessage.remove();
            return ServiceResult.success("Emergency cases retrieved successfully.", Collections.emptyList());
        } else {
            cases = emergencyCaseDAO.getAllEmergencyCases();
        }

        if (emergencyCaseDAO.getLastDatabaseError() != null) {
            String err = "An internal database error occurred while retrieving emergency cases.";
            lastErrorMessage.set(err);
            return ServiceResult.databaseError(err);
        }

        lastErrorMessage.remove();
        return ServiceResult.success("Emergency cases retrieved successfully.", cases);
    }

    public ServiceResult<EmergencyCase> getCaseByIdForCaller(int caseId, int userId, String role) {
        if (!ValidationUtil.isValidId(caseId)) {
            String err = "Invalid case ID. ID must be greater than 0.";
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
            String err = "Emergency case not found with ID: " + caseId;
            lastErrorMessage.set(err);
            return ServiceResult.notFound(err);
        }

        if ("AMBULANCE_DRIVER".equalsIgnoreCase(role) || "PARAMEDIC".equalsIgnoreCase(role)) {
            if (userId <= 0) {
                String err = "Emergency case not found with ID: " + caseId;
                lastErrorMessage.set(err);
                return ServiceResult.notFound(err);
            }
            boolean assigned = emergencyCaseDAO.isCaseAssignedToUser(caseId, userId, role);
            if (emergencyCaseDAO.getLastDatabaseError() != null) {
                String err = "An internal database error occurred while verifying case assignment.";
                lastErrorMessage.set(err);
                return ServiceResult.databaseError(err);
            }
            if (!assigned) {
                // Consistent safe response: avoid leaking existence of cross-ambulance cases
                String err = "Emergency case not found with ID: " + caseId;
                lastErrorMessage.set(err);
                return ServiceResult.notFound(err);
            }
        }

        if ("HOSPITAL_ADMIN".equalsIgnoreCase(role)) {
            String err = "Emergency case not found with ID: " + caseId;
            lastErrorMessage.set(err);
            return ServiceResult.notFound(err);
        }

        lastErrorMessage.remove();
        return ServiceResult.success("Emergency case retrieved successfully.", emergencyCase);
    }

    public boolean isValidStatus(String status) {
        if (status == null) {
            return false;
        }

        String s = status.toUpperCase();

        return s.equals("REGISTERED")
                || s.equals("DISPATCHED")
                || s.equals("IN_TRANSIT")
                || s.equals("ARRIVED")
                || s.equals("COMPLETED")
                || s.equals("CANCELLED");
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
            // Direct transition from REGISTERED to DISPATCHED is disallowed for case-status updates.
            // Ambulance assignment and dispatch must occur through the formal DispatchService workflow.
            case "REGISTERED" -> target.equals("CANCELLED");
            case "DISPATCHED" -> target.equals("IN_TRANSIT") || target.equals("CANCELLED");
            case "IN_TRANSIT" -> target.equals("ARRIVED") || target.equals("CANCELLED");
            case "ARRIVED" -> target.equals("COMPLETED") || target.equals("CANCELLED");
            case "COMPLETED", "CANCELLED" -> false;
            default -> false;
        };
    }

    public ServiceResult<EmergencyCase> updateCaseStatusAtomic(int caseId, String targetStatus, int userId, String role) {

        if (!ValidationUtil.isValidId(caseId)) {
            String err = "Invalid case ID. ID must be greater than 0.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        if (targetStatus == null || targetStatus.trim().isEmpty()) {
            String err = "Case status cannot be empty.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        String normalizedTarget = targetStatus.toUpperCase();

        if (!isValidStatus(normalizedTarget)) {
            String err = "Invalid case status: " + targetStatus;
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        EmergencyCase existingCase = emergencyCaseDAO.getEmergencyCaseById(caseId);
        if (emergencyCaseDAO.getLastDatabaseError() != null) {
            String err = "An internal database error occurred while retrieving the emergency case.";
            lastErrorMessage.set(err);
            return ServiceResult.databaseError(err);
        }

        if (existingCase == null) {
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
                // Safe 404 response to avoid ID enumeration for unauthorized cases
                String err = "Emergency case not found with ID: " + caseId;
                lastErrorMessage.set(err);
                return ServiceResult.notFound(err);
            }
        }

        if ("AMBULANCE_DRIVER".equalsIgnoreCase(role) || "HOSPITAL_ADMIN".equalsIgnoreCase(role) || "EMERGENCY_DOCTOR".equalsIgnoreCase(role)) {
            String err = "Access denied. Insufficient role permissions to update case status.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        String currentStatus = existingCase.getCaseStatus();
        if (!isAllowedTransition(currentStatus, normalizedTarget)) {
            String err = "Invalid status transition from " + currentStatus + " to " + normalizedTarget + ".";
            lastErrorMessage.set(err);
            return ServiceResult.conflict(err);
        }

        boolean updated = emergencyCaseDAO.updateCaseStatusAtomic(caseId, currentStatus, normalizedTarget);
        if (updated) {
            existingCase.setCaseStatus(normalizedTarget);
            lastErrorMessage.remove();
            return ServiceResult.success("Emergency case status updated successfully.", existingCase);
        } else {
            // Concurrent update conflict check: did another thread change the status?
            EmergencyCase recheck = emergencyCaseDAO.getEmergencyCaseById(caseId);
            if (recheck != null && !recheck.getCaseStatus().equalsIgnoreCase(currentStatus)) {
                String err = "Concurrent update conflict: case status was modified to " + recheck.getCaseStatus() + " by another request.";
                lastErrorMessage.set(err);
                return ServiceResult.conflict(err);
            }

            String err = "DATABASE_ERROR: Unable to update case status in database.";
            lastErrorMessage.set(err);
            return ServiceResult.databaseError(err);
        }
    }

    public ServiceResult<EmergencyCase> updateCaseStatusAtomic(int caseId, String targetStatus) {
        return updateCaseStatusAtomic(caseId, targetStatus, -1, "ADMIN");
    }

    public boolean updateCaseStatus(int caseId, String status) {
        return updateCaseStatusAtomic(caseId, status).isSuccess();
    }

    public ServiceResult<CaseTriageResponse> triageCaseResult(int caseId, CaseTriageRequest request, int userId, String role) {
        if (!ValidationUtil.isValidId(caseId)) {
            String err = "Invalid case ID. ID must be greater than 0.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        if (request == null) {
            String err = "Triage request body cannot be null.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        if (request.getHeartRate() == null || !ValidationUtil.isValidHeartRate(request.getHeartRate())) {
            String err = "Invalid heart rate: must be between 20 and 250 bpm.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        if (request.getSystolicBp() == null || request.getDiastolicBp() == null
                || !ValidationUtil.isValidBloodPressure(request.getSystolicBp(), request.getDiastolicBp())) {
            String err = "Invalid blood pressure: systolic must be between 40 and 250 mmHg, diastolic between 20 and 150 mmHg.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        if (request.getSystolicBp() <= request.getDiastolicBp()) {
            String err = "Inconsistent blood pressure: systolic BP (" + request.getSystolicBp()
                    + ") must be strictly greater than diastolic BP (" + request.getDiastolicBp() + ").";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        if (request.getSpo2() == null || !ValidationUtil.isValidSpo2(request.getSpo2())) {
            String err = "Invalid SpO2: must be between 0.0 and 100.0%.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        if (request.getRespiratoryRate() == null || !ValidationUtil.isValidRespiratoryRate(request.getRespiratoryRate())) {
            String err = "Invalid respiratory rate: must be between 5 and 80 breaths/min.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        if (request.getTemperature() != null && !ValidationUtil.isValidTemperature(request.getTemperature())) {
            String err = "Invalid temperature: must be between 25.0 and 45.0 °C.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        ServiceResult<EmergencyCase> caseResult = getCaseByIdForCaller(caseId, userId, role);
        if (caseResult.getStatus() == ServiceResult.Status.NOT_FOUND) {
            lastErrorMessage.set(caseResult.getMessage());
            return ServiceResult.notFound(caseResult.getMessage());
        }
        if (caseResult.getStatus() == ServiceResult.Status.DATABASE_ERROR) {
            lastErrorMessage.set(caseResult.getMessage());
            return ServiceResult.databaseError(caseResult.getMessage());
        }
        if (!caseResult.isSuccess()) {
            lastErrorMessage.set(caseResult.getMessage());
            return ServiceResult.validationError(caseResult.getMessage());
        }

        EmergencyCase existingCase = caseResult.getData();

        if ("AMBULANCE_DRIVER".equalsIgnoreCase(role) || "HOSPITAL_ADMIN".equalsIgnoreCase(role)) {
            String err = "Access denied. Insufficient role permissions to perform AI triage.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        if (request.getPatientId() != null && !request.getPatientId().equals(existingCase.getPatientId())) {
            String err = "Supplied patient ID " + request.getPatientId()
                    + " does not match emergency case patient ID " + existingCase.getPatientId() + ".";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        String currentStatus = existingCase.getCaseStatus();
        if ("COMPLETED".equalsIgnoreCase(currentStatus) || "CANCELLED".equalsIgnoreCase(currentStatus)) {
            String err = "Cannot perform AI triage on " + currentStatus + " case.";
            lastErrorMessage.set(err);
            return ServiceResult.conflict(err);
        }

        PatientVital vital = new PatientVital();
        vital.setCaseId(caseId);
        vital.setHeartRate(request.getHeartRate());
        vital.setSystolicBp(request.getSystolicBp());
        vital.setDiastolicBp(request.getDiastolicBp());
        vital.setSpo2(request.getSpo2());
        vital.setRespiratoryRate(request.getRespiratoryRate());
        vital.setTemperature(request.getTemperature() != null ? request.getTemperature() : 37.0);
        vital.setGlucose(request.getGlucose() != null ? request.getGlucose() : 0.0);

        String symptoms = (request.getSymptoms() != null && !request.getSymptoms().trim().isEmpty())
                ? request.getSymptoms().trim()
                : existingCase.getSymptoms();
        vital.setSymptoms(symptoms);

        String calculatedPriority = triageService.determinePriority(vital);
        String suggestedSpecialty = triageService.determineSpecialty(symptoms);
        String priorityCode = switch (calculatedPriority) {
            case "CRITICAL" -> "P1";
            case "HIGH" -> "P2";
            case "MODERATE" -> "P3";
            default -> "P4";
        };

        String recommendation = switch (calculatedPriority) {
            case "CRITICAL" -> "Immediate emergency care required. Route patient to " + suggestedSpecialty + " facility without delay.";
            case "HIGH" -> "Urgent medical attention required. Prefer " + suggestedSpecialty + " emergency services.";
            case "MODERATE" -> "Medical evaluation required. Monitor patient during transport.";
            default -> "Stable condition. Continue routine monitoring during transport.";
        };

        String previousPriority = existingCase.getPriorityLevel();
        boolean isEscalation = isPriorityEscalation(previousPriority, calculatedPriority);
        String activePriority = isEscalation ? calculatedPriority : previousPriority;

        if (isEscalation) {
            boolean updated = emergencyCaseDAO.updateCasePriorityAtomic(caseId, previousPriority, calculatedPriority);
            if (updated) {
                existingCase.setPriorityLevel(calculatedPriority);
            } else {
                EmergencyCase recheck = emergencyCaseDAO.getEmergencyCaseById(caseId);
                if (recheck != null) {
                    if (getPriorityRank(recheck.getPriorityLevel()) >= getPriorityRank(calculatedPriority)) {
                        activePriority = recheck.getPriorityLevel();
                        existingCase.setPriorityLevel(activePriority);
                    } else {
                        String err = "DATABASE_ERROR: Unable to synchronize emergency case priority in database.";
                        lastErrorMessage.set(err);
                        return ServiceResult.databaseError(err);
                    }
                } else {
                    String err = "DATABASE_ERROR: Unable to synchronize emergency case priority in database.";
                    lastErrorMessage.set(err);
                    return ServiceResult.databaseError(err);
                }
            }
        }

        CaseTriageResponse response = new CaseTriageResponse(
                caseId,
                existingCase.getPatientId(),
                previousPriority,
                calculatedPriority,
                activePriority,
                priorityCode,
                suggestedSpecialty,
                recommendation,
                isEscalation,
                "AI-assisted triage decision support. Not a confirmed medical diagnosis."
        );

        lastErrorMessage.remove();
        return ServiceResult.success("AI triage evaluation completed successfully.", response);
    }

    public String getLastErrorMessage() {
        return lastErrorMessage.get();
    }
}
