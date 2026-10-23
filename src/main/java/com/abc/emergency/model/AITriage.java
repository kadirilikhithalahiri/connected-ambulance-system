package com.abc.emergency.model;

public class AITriage {

    private int triageId;
    private int caseId;
    private String priorityLevel;
    private String suggestedSpecialty;
    private int suggestedHospitalId;
    private int estimatedArrivalMinutes;
    private String priorityCode;
    private String recommendation;

    public AITriage() {
    }

    public AITriage(int triageId, int caseId, String priorityLevel,
                    String suggestedSpecialty, int suggestedHospitalId,
                    int estimatedArrivalMinutes, String priorityCode,
                    String recommendation) {
        this.triageId = triageId;
        this.caseId = caseId;
        this.priorityLevel = priorityLevel;
        this.suggestedSpecialty = suggestedSpecialty;
        this.suggestedHospitalId = suggestedHospitalId;
        this.estimatedArrivalMinutes = estimatedArrivalMinutes;
        this.priorityCode = priorityCode;
        this.recommendation = recommendation;
    }

    public int getTriageId() {
        return triageId;
    }

    public void setTriageId(int triageId) {
        this.triageId = triageId;
    }

    public int getCaseId() {
        return caseId;
    }

    public void setCaseId(int caseId) {
        this.caseId = caseId;
    }

    public String getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(String priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public String getSuggestedSpecialty() {
        return suggestedSpecialty;
    }

    public void setSuggestedSpecialty(String suggestedSpecialty) {
        this.suggestedSpecialty = suggestedSpecialty;
    }

    public int getSuggestedHospitalId() {
        return suggestedHospitalId;
    }

    public void setSuggestedHospitalId(int suggestedHospitalId) {
        this.suggestedHospitalId = suggestedHospitalId;
    }

    public int getEstimatedArrivalMinutes() {
        return estimatedArrivalMinutes;
    }

    public void setEstimatedArrivalMinutes(int estimatedArrivalMinutes) {
        this.estimatedArrivalMinutes = estimatedArrivalMinutes;
    }

    public String getPriorityCode() {
        return priorityCode;
    }

    public void setPriorityCode(String priorityCode) {
        this.priorityCode = priorityCode;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }
}