package com.abc.emergency.model;

public class DispatchRecord {

    private int dispatchId;
    private int caseId;
    private int ambulanceId;
    private int estimatedArrivalMinutes;
    private String dispatchStatus;

    public DispatchRecord() {
    }

    public DispatchRecord(int dispatchId, int caseId, int ambulanceId,
                          int estimatedArrivalMinutes, String dispatchStatus) {
        this.dispatchId = dispatchId;
        this.caseId = caseId;
        this.ambulanceId = ambulanceId;
        this.estimatedArrivalMinutes = estimatedArrivalMinutes;
        this.dispatchStatus = dispatchStatus;
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
}