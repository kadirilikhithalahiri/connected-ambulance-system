package com.abc.emergency.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class EmergencyCaseRequest {

    @NotNull(message = "Patient ID is required.")
    @Min(value = 1, message = "Patient ID must be greater than 0.")
    private Integer patientId;

    @NotBlank(message = "Incident location cannot be empty.")
    private String incidentLocation;

    @NotBlank(message = "Emergency type cannot be empty.")
    private String emergencyType;

    @NotBlank(message = "Symptoms cannot be empty.")
    private String symptoms;

    @Pattern(regexp = "^(?i)\\s*(CRITICAL|HIGH|MODERATE|LOW)\\s*$", message = "Priority must be CRITICAL, HIGH, MODERATE, or LOW.")
    private String priorityLevel;

    public EmergencyCaseRequest() {
    }

    public EmergencyCaseRequest(Integer patientId, String incidentLocation, String emergencyType, String symptoms) {
        this(patientId, incidentLocation, emergencyType, symptoms, null);
    }

    public EmergencyCaseRequest(Integer patientId, String incidentLocation, String emergencyType, String symptoms, String priorityLevel) {
        this.patientId = patientId;
        this.incidentLocation = incidentLocation;
        this.emergencyType = emergencyType;
        this.symptoms = symptoms;
        this.priorityLevel = priorityLevel;
    }

    public Integer getPatientId() {
        return patientId;
    }

    public void setPatientId(Integer patientId) {
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
}
