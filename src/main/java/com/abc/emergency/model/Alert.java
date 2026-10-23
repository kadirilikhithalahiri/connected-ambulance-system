package com.abc.emergency.model;

public class Alert {

    private int alertId;
    private int caseId;
    private String alertType;
    private String alertMessage;
    private String severity;
    private String recipientRole;
    private boolean isRead;

    public Alert() {
    }

    public Alert(int alertId, int caseId, String alertType,
                 String alertMessage, String severity,
                 String recipientRole, boolean isRead) {
        this.alertId = alertId;
        this.caseId = caseId;
        this.alertType = alertType;
        this.alertMessage = alertMessage;
        this.severity = severity;
        this.recipientRole = recipientRole;
        this.isRead = isRead;
    }

    public int getAlertId() {
        return alertId;
    }

    public void setAlertId(int alertId) {
        this.alertId = alertId;
    }

    public int getCaseId() {
        return caseId;
    }

    public void setCaseId(int caseId) {
        this.caseId = caseId;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public String getAlertMessage() {
        return alertMessage;
    }

    public void setAlertMessage(String alertMessage) {
        this.alertMessage = alertMessage;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getRecipientRole() {
        return recipientRole;
    }

    public void setRecipientRole(String recipientRole) {
        this.recipientRole = recipientRole;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}