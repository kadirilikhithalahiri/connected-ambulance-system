package com.abc.emergency.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class DispatchRequest {

    @NotNull(message = "Case ID is required.")
    @Positive(message = "Case ID must be greater than 0.")
    private Integer caseId;

    @NotNull(message = "Ambulance ID is required.")
    @Positive(message = "Ambulance ID must be greater than 0.")
    private Integer ambulanceId;

    @NotNull(message = "Estimated arrival time is required.")
    @Min(value = 0, message = "ETA minutes cannot be negative.")
    @Max(value = 300, message = "ETA minutes cannot exceed 300.")
    private Integer estimatedArrivalMinutes;

    public DispatchRequest() {
    }

    public DispatchRequest(Integer caseId, Integer ambulanceId, Integer estimatedArrivalMinutes) {
        this.caseId = caseId;
        this.ambulanceId = ambulanceId;
        this.estimatedArrivalMinutes = estimatedArrivalMinutes;
    }

    public Integer getCaseId() {
        return caseId;
    }

    public void setCaseId(Integer caseId) {
        this.caseId = caseId;
    }

    public Integer getAmbulanceId() {
        return ambulanceId;
    }

    public void setAmbulanceId(Integer ambulanceId) {
        this.ambulanceId = ambulanceId;
    }

    public Integer getEstimatedArrivalMinutes() {
        return estimatedArrivalMinutes;
    }

    public void setEstimatedArrivalMinutes(Integer estimatedArrivalMinutes) {
        this.estimatedArrivalMinutes = estimatedArrivalMinutes;
    }
}
