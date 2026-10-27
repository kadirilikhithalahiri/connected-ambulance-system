package com.abc.emergency.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class DispatchStatusUpdateRequest {

    @NotBlank(message = "Dispatch status is required.")
    @Pattern(
            regexp = "(?i)^(DISPATCHED|EN_ROUTE|ARRIVED|COMPLETED|CANCELLED)$",
            message = "Invalid dispatch status. Allowed values: DISPATCHED, EN_ROUTE, ARRIVED, COMPLETED, CANCELLED."
    )
    private String status;

    public DispatchStatusUpdateRequest() {
    }

    public DispatchStatusUpdateRequest(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
