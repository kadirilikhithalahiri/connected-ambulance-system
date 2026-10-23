package com.abc.emergency.service;

import com.abc.emergency.dao.EmergencyCaseDAO;
import com.abc.emergency.dao.PatientDAO;
import com.abc.emergency.model.EmergencyCase;
import com.abc.emergency.model.Patient;
import com.abc.emergency.util.ValidationUtil;

import java.util.List;

public class EmergencyCaseService {

    private final EmergencyCaseDAO emergencyCaseDAO;
    private final PatientDAO patientDAO;

    public EmergencyCaseService() {
        emergencyCaseDAO = new EmergencyCaseDAO();
        patientDAO = new PatientDAO();
    }

    public boolean registerEmergencyCase(int patientId,
                                         String incidentLocation,
                                         String emergencyType,
                                         String symptoms) {

        if (!ValidationUtil.isValidId(patientId)) {
            System.out.println("Invalid patient ID.");
            return false;
        }

        if (incidentLocation == null || incidentLocation.trim().isEmpty()) {
            System.out.println("Incident location cannot be empty.");
            return false;
        }

        if (emergencyType == null || emergencyType.trim().isEmpty()) {
            System.out.println("Emergency type cannot be empty.");
            return false;
        }

        if (symptoms == null || symptoms.trim().isEmpty()) {
            System.out.println("Symptoms cannot be empty.");
            return false;
        }

        Patient patient = patientDAO.getPatientById(patientId);

        if (patient == null) {
            System.out.println("Patient not found.");
            return false;
        }

        EmergencyCase emergencyCase = new EmergencyCase();

        emergencyCase.setPatientId(patientId);
        emergencyCase.setIncidentLocation(incidentLocation);
        emergencyCase.setEmergencyType(emergencyType);
        emergencyCase.setSymptoms(symptoms);
        emergencyCase.setCaseStatus("REGISTERED");

        boolean result = emergencyCaseDAO.addEmergencyCase(emergencyCase);

        if (result) {
            System.out.println("Emergency case registered successfully.");
        } else {
            System.out.println("Emergency case registration failed.");
        }

        return result;
    }

    public EmergencyCase getEmergencyCaseById(int caseId) {

        if (!ValidationUtil.isValidId(caseId)) {
            System.out.println("Invalid case ID.");
            return null;
        }

        return emergencyCaseDAO.getEmergencyCaseById(caseId);
    }

    public List<EmergencyCase> getAllEmergencyCases() {
        return emergencyCaseDAO.getAllEmergencyCases();
    }

    public boolean updateCaseStatus(int caseId, String status) {

        if (!ValidationUtil.isValidId(caseId)) {
            System.out.println("Invalid case ID.");
            return false;
        }

        if (status == null || status.trim().isEmpty()) {
            System.out.println("Case status cannot be empty.");
            return false;
        }

        return emergencyCaseDAO.updateCaseStatus(caseId, status.toUpperCase());
    }
}