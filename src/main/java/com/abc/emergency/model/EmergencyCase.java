package com.abc.emergency.model;

public class EmergencyCase {

    private int caseId;
    private int patientId;
    private String incidentLocation;
    private String emergencyType;
    private String symptoms;
    private String priorityLevel;
    private String caseStatus;

    public EmergencyCase() {
    }

    public EmergencyCase(int caseId, int patientId, String incidentLocation,
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