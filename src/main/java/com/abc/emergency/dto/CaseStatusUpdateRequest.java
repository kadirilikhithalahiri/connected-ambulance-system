package com.abc.emergency.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CaseStatusUpdateRequest {

    @NotBlank(message = "Case status cannot be empty.")
    @Pattern(
            regexp = "(?i)^(REGISTERED|DISPATCHED|IN_TRANSIT|ARRIVED|COMPLETED|CANCELLED)$",
            message = "Invalid case status. Allowed values: REGISTERED, DISPATCHED, IN_TRANSIT, ARRIVED, COMPLETED, CANCELLED."
    )
    private String status;

    public CaseStatusUpdateRequest() {
    }

    public CaseStatusUpdateRequest(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
