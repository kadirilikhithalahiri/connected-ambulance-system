package com.abc.emergency.dto;

import com.abc.emergency.model.DispatchRecord;

public class DispatchResponse {

    private int dispatchId;
    private int caseId;
    private int ambulanceId;
    private String vehicleNumber;
    private int estimatedArrivalMinutes;
    private String dispatchStatus;
    private String dispatchTime;
    private String actualArrivalTime;

    public DispatchResponse() {
    }

    public DispatchResponse(int dispatchId, int caseId, int ambulanceId, String vehicleNumber,
                            int estimatedArrivalMinutes, String dispatchStatus, String dispatchTime, String actualArrivalTime) {
        this.dispatchId = dispatchId;
        this.caseId = caseId;
        this.ambulanceId = ambulanceId;
        this.vehicleNumber = vehicleNumber;
        this.estimatedArrivalMinutes = estimatedArrivalMinutes;
        this.dispatchStatus = dispatchStatus;
        this.dispatchTime = dispatchTime;
        this.actualArrivalTime = actualArrivalTime;
    }

    public static DispatchResponse from(DispatchRecord record, String vehicleNumber) {
        if (record == null) {
            return null;
        }
        return new DispatchResponse(
                record.getDispatchId(),
                record.getCaseId(),
                record.getAmbulanceId(),
                vehicleNumber,
                record.getEstimatedArrivalMinutes(),
                record.getDispatchStatus(),
                null,
                null
        );
    }

    public static DispatchResponse from(DispatchRecord record) {
        return from(record, null);
    }

    public int getDispatchId() {
        return dispatchId;
    }

    public void setDispatchId(int dispatchId) {
        this.dispatchId = dispatchId;
    }

    public int getCaseId() {
        return caseId;
    }

    public void setCaseId(int caseId) {
        this.caseId = caseId;
    }

    public int getAmbulanceId() {
        return ambulanceId;
    }

    public void setAmbulanceId(int ambulanceId) {
        this.ambulanceId = ambulanceId;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public int getEstimatedArrivalMinutes() {
        return estimatedArrivalMinutes;
    }

    public void setEstimatedArrivalMinutes(int estimatedArrivalMinutes) {
        this.estimatedArrivalMinutes = estimatedArrivalMinutes;
    }

    public String getDispatchStatus() {
        return dispatchStatus;
    }

    public void setDispatchStatus(String dispatchStatus) {
        this.dispatchStatus = dispatchStatus;
    }

    public String getDispatchTime() {
        return dispatchTime;
    }

    public void setDispatchTime(String dispatchTime) {
        this.dispatchTime = dispatchTime;
    }

    public String getActualArrivalTime() {
        return actualArrivalTime;
    }

    public void setActualArrivalTime(String actualArrivalTime) {
        this.actualArrivalTime = actualArrivalTime;
    }
}
