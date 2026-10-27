package com.abc.emergency.dto;

import com.abc.emergency.model.EmergencyCase;

public class EmergencyCaseResponse {

    private int caseId;
    private int patientId;
    private String incidentLocation;
    private String emergencyType;
    private String symptoms;
    private String priorityLevel;
    private String caseStatus;

    public EmergencyCaseResponse() {
    }

    public EmergencyCaseResponse(int caseId, int patientId, String incidentLocation,
                                 String emergencyType, String symptoms,
                                 String priorityLevel, String caseStatus) {
        this.caseId = caseId;
        this.patientId = patientId;
        this.incidentLocation = incidentLocation;
        this.emergencyType = emergencyType;
        this.symptoms = symptoms;
        this.priorityLevel = priorityLevel;
        this.caseStatus = caseStatus;
    }

    public static EmergencyCaseResponse from(EmergencyCase ec) {
        if (ec == null) {
            return null;
        }
        return new EmergencyCaseResponse(
                ec.getCaseId(),
                ec.getPatientId(),
                ec.getIncidentLocation(),
                ec.getEmergencyType(),
                ec.getSymptoms(),
                ec.getPriorityLevel(),
                ec.getCaseStatus()
        );
    }

    public int getCaseId() {
        return caseId;
    }

    public void setCaseId(int caseId) {
        this.caseId = caseId;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public String getIncidentLocation() {
        return incidentLocation;
    }

    public void setIncidentLocation(String incidentLocation) {
        this.incidentLocation = incidentLocation;
    }

    public String getEmergencyType() {
        return emergencyType;
    }

    public void setEmergencyType(String emergencyType) {
        this.emergencyType = emergencyType;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    public String getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(String priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public String getCaseStatus() {
        return caseStatus;
    }

    public void setCaseStatus(String caseStatus) {
        this.caseStatus = caseStatus;
    }
}
