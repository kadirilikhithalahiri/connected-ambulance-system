package com.abc.emergency.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CaseTriageRequest {

    @Min(value = 1, message = "Patient ID must be greater than 0.")
    private Integer patientId;

    @NotNull(message = "Heart rate is required.")
    @Min(value = 20, message = "Heart rate must be at least 20 bpm.")
    @Max(value = 250, message = "Heart rate must not exceed 250 bpm.")
    private Integer heartRate;

    @NotNull(message = "Systolic blood pressure is required.")
    @Min(value = 40, message = "Systolic BP must be at least 40 mmHg.")
    @Max(value = 250, message = "Systolic BP must not exceed 250 mmHg.")
    private Integer systolicBp;

    @NotNull(message = "Diastolic blood pressure is required.")
    @Min(value = 20, message = "Diastolic BP must be at least 20 mmHg.")
    @Max(value = 150, message = "Diastolic BP must not exceed 150 mmHg.")
    private Integer diastolicBp;

    @NotNull(message = "SpO2 is required.")
    @DecimalMin(value = "0.0", message = "SpO2 must be at least 0.0%.")
    @DecimalMax(value = "100.0", message = "SpO2 must not exceed 100.0%.")
    private Double spo2;

    @DecimalMin(value = "25.0", message = "Temperature must be at least 25.0 °C.")
    @DecimalMax(value = "45.0", message = "Temperature must not exceed 45.0 °C.")
    private Double temperature;

    @NotNull(message = "Respiratory rate is required.")
    @Min(value = 5, message = "Respiratory rate must be at least 5 breaths/min.")
    @Max(value = 80, message = "Respiratory rate must not exceed 80 breaths/min.")
    private Integer respiratoryRate;

    @DecimalMin(value = "0.0", message = "Glucose cannot be negative.")
    private Double glucose;

    private String symptoms;

    @Min(value = 1, message = "Hospital ID must be greater than 0.")
    private Integer hospitalId;

    @Min(value = 0, message = "Estimated arrival time cannot be negative.")
    private Integer estimatedArrivalMinutes;

    public CaseTriageRequest() {
    }

    public CaseTriageRequest(Integer patientId, Integer heartRate, Integer systolicBp,
                             Integer diastolicBp, Double spo2, Double temperature,
                             Integer respiratoryRate, Double glucose, String symptoms) {
        this.patientId = patientId;
        this.heartRate = heartRate;
        this.systolicBp = systolicBp;
        this.diastolicBp = diastolicBp;
        this.spo2 = spo2;
        this.temperature = temperature;
        this.respiratoryRate = respiratoryRate;
        this.glucose = glucose;
        this.symptoms = symptoms;
    }

    @AssertTrue(message = "Inconsistent blood pressure: systolic BP must be strictly greater than diastolic BP.")
    public boolean isBloodPressureConsistent() {
        if (systolicBp == null || diastolicBp == null) {
            return true; // Nulls handled by @NotNull
        }
        return systolicBp > diastolicBp;
    }

    public Integer getPatientId() {
        return patientId;
    }

    public void setPatientId(Integer patientId) {
        this.patientId = patientId;
    }

    public Integer getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(Integer heartRate) {
        this.heartRate = heartRate;
    }

    public Integer getSystolicBp() {
        return systolicBp;
    }

    public void setSystolicBp(Integer systolicBp) {
        this.systolicBp = systolicBp;
    }

    public Integer getDiastolicBp() {
        return diastolicBp;
    }

    public void setDiastolicBp(Integer diastolicBp) {
        this.diastolicBp = diastolicBp;
    }

    public Double getSpo2() {
        return spo2;
    }

    public void setSpo2(Double spo2) {
        this.spo2 = spo2;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getRespiratoryRate() {
        return respiratoryRate;
    }

    public void setRespiratoryRate(Integer respiratoryRate) {
        this.respiratoryRate = respiratoryRate;
    }

    public Double getGlucose() {
        return glucose;
    }

    public void setGlucose(Double glucose) {
        this.glucose = glucose;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    public Integer getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(Integer hospitalId) {
        this.hospitalId = hospitalId;
    }

    public Integer getEstimatedArrivalMinutes() {
        return estimatedArrivalMinutes;
    }

    public void setEstimatedArrivalMinutes(Integer estimatedArrivalMinutes) {
        this.estimatedArrivalMinutes = estimatedArrivalMinutes;
    }
}
