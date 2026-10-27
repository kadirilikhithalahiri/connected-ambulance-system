package com.abc.emergency.dto;

public class CaseTriageResponse {

    private int caseId;
    private int patientId;
    private String previousPriority;
    private String calculatedPriority;
    private String activePriority;
    private String priorityCode;
    private String suggestedSpecialty;
    private String recommendation;
    private boolean priorityEscalated;
    private String disclaimer;

    public CaseTriageResponse() {
    }

    public CaseTriageResponse(int caseId, int patientId, String previousPriority,
                              String calculatedPriority, String activePriority,
                              String priorityCode, String suggestedSpecialty,
                              String recommendation, boolean priorityEscalated,
                              String disclaimer) {
        this.caseId = caseId;
        this.patientId = patientId;
        this.previousPriority = previousPriority;
        this.calculatedPriority = calculatedPriority;
        this.activePriority = activePriority;
        this.priorityCode = priorityCode;
        this.suggestedSpecialty = suggestedSpecialty;
        this.recommendation = recommendation;
        this.priorityEscalated = priorityEscalated;
        this.disclaimer = disclaimer;
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

    public String getPreviousPriority() {
        return previousPriority;
    }

    public void setPreviousPriority(String previousPriority) {
        this.previousPriority = previousPriority;
    }

    public String getCalculatedPriority() {
        return calculatedPriority;
    }

    public void setCalculatedPriority(String calculatedPriority) {
        this.calculatedPriority = calculatedPriority;
    }

    public String getActivePriority() {
        return activePriority;
    }

    public void setActivePriority(String activePriority) {
        this.activePriority = activePriority;
    }

    public String getPriorityCode() {
        return priorityCode;
    }

    public void setPriorityCode(String priorityCode) {
        this.priorityCode = priorityCode;
    }

    public String getSuggestedSpecialty() {
        return suggestedSpecialty;
    }

    public void setSuggestedSpecialty(String suggestedSpecialty) {
        this.suggestedSpecialty = suggestedSpecialty;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public boolean isPriorityEscalated() {
        return priorityEscalated;
    }

    public void setPriorityEscalated(boolean priorityEscalated) {
        this.priorityEscalated = priorityEscalated;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}
