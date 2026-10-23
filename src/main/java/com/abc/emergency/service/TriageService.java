package com.abc.emergency.service;

import com.abc.emergency.dao.AITriageDAO;
import com.abc.emergency.model.AITriage;
import com.abc.emergency.model.PatientVital;
import com.abc.emergency.util.ValidationUtil;

public class TriageService {

    private final AITriageDAO triageDAO;

    public TriageService() {
        triageDAO = new AITriageDAO();
    }

    public AITriage analyzeVitals(int caseId, PatientVital vital) {

        if (!ValidationUtil.isValidId(caseId)) {
            return null;
        }

        if (vital == null) {
            return null;
        }

        String priority = determinePriority(vital);
        String priorityCode = generatePriorityCode(priority);

        AITriage result = new AITriage();

        result.setCaseId(caseId);
        result.setPriorityLevel(priority);
        result.setPriorityCode(priorityCode);
        result.setSuggestedSpecialty("GENERAL MEDICINE");
        result.setEstimatedArrivalMinutes(15);
        result.setRecommendation(
                generateRecommendation(priority, "GENERAL MEDICINE")
        );

        return result;
    }

    public boolean performTriage(
            int caseId,
            PatientVital vital,
            String symptoms,
            int hospitalId,
            int estimatedArrivalMinutes) {

        if (!ValidationUtil.isValidId(caseId)) {
            System.out.println("Invalid case ID.");
            return false;
        }

        if (vital == null) {
            System.out.println("Vital information is required.");
            return false;
        }

        if (estimatedArrivalMinutes < 0) {
            System.out.println("Invalid estimated arrival time.");
            return false;
        }

        String priority = determinePriority(vital);
        String specialty = determineSpecialty(symptoms);
        String priorityCode = generatePriorityCode(priority);
        String recommendation =
                generateRecommendation(priority, specialty);

        AITriage triage = new AITriage();

        triage.setCaseId(caseId);
        triage.setPriorityLevel(priority);
        triage.setSuggestedSpecialty(specialty);
        triage.setSuggestedHospitalId(hospitalId);
        triage.setEstimatedArrivalMinutes(estimatedArrivalMinutes);
        triage.setPriorityCode(priorityCode);
        triage.setRecommendation(recommendation);

        boolean saved = triageDAO.addTriage(triage);

        if (saved) {
            System.out.println("AI Triage completed successfully.");
            System.out.println("Priority: " + priority);
            System.out.println("Suggested Specialty: " + specialty);
            System.out.println("Priority Code: " + priorityCode);
            System.out.println("Recommendation: " + recommendation);
        }

        return saved;
    }

    public String determinePriority(PatientVital vital) {

        if (vital.getSpo2() < 90
                || vital.getSystolicBp() < 80
                || vital.getHeartRate() > 140
                || vital.getHeartRate() < 40
                || vital.getRespiratoryRate() > 30) {

            return "CRITICAL";
        }

        if (vital.getSpo2() < 94
                || vital.getSystolicBp() < 90
                || vital.getHeartRate() > 120
                || vital.getHeartRate() < 50
                || vital.getRespiratoryRate() > 24) {

            return "HIGH";
        }

        if (vital.getSpo2() < 96
                || vital.getSystolicBp() < 100
                || vital.getHeartRate() > 100
                || vital.getHeartRate() < 60
                || vital.getRespiratoryRate() > 20) {

            return "MODERATE";
        }

        return "LOW";
    }

    public String determineSpecialty(String symptoms) {

        if (symptoms == null || symptoms.trim().isEmpty()) {
            return "GENERAL MEDICINE";
        }

        String value = symptoms.toLowerCase();

        if (value.contains("chest")
                || value.contains("heart")
                || value.contains("cardiac")) {
            return "CARDIOLOGY";
        }

        if (value.contains("breathing")
                || value.contains("asthma")
                || value.contains("respiratory")) {
            return "PULMONOLOGY";
        }

        if (value.contains("head")
                || value.contains("stroke")
                || value.contains("seizure")
                || value.contains("unconscious")) {
            return "NEUROLOGY";
        }

        if (value.contains("accident")
                || value.contains("fracture")
                || value.contains("injury")
                || value.contains("trauma")) {
            return "TRAUMA";
        }

        return "GENERAL MEDICINE";
    }

    private String generatePriorityCode(String priority) {

        return switch (priority) {
            case "CRITICAL" -> "P1";
            case "HIGH" -> "P2";
            case "MODERATE" -> "P3";
            default -> "P4";
        };
    }

    private String generateRecommendation(
            String priority,
            String specialty) {

        return switch (priority) {
            case "CRITICAL" ->
                    "Immediate emergency care required. Route patient to "
                    + specialty + " facility without delay.";

            case "HIGH" ->
                    "Urgent medical attention required. Prefer "
                    + specialty + " emergency services.";

            case "MODERATE" ->
                    "Medical evaluation required. Monitor patient during transport.";

            default ->
                    "Stable condition. Continue routine monitoring during transport.";
        };
    }
}