package com.abc.emergency.service;

import com.abc.emergency.dao.AlertDAO;
import com.abc.emergency.model.Alert;
import com.abc.emergency.model.PatientVital;

public class AlertService {

    private final AlertDAO alertDAO;

    public AlertService() {
        alertDAO = new AlertDAO();
    }

    public boolean checkVitalsAndCreateAlert(int caseId, PatientVital vital) {

        if (vital == null) {
            return false;
        }

        String alertType = null;
        String message = null;
        String severity = "MEDIUM";
        String recipientRole = "DOCTOR";

        if (vital.getSpo2() < 90 || vital.getSystolicBp() < 80
                || vital.getHeartRate() > 140 || vital.getHeartRate() < 40) {

            alertType = "CRITICAL_PATIENT";
            message = "Critical patient condition detected from vital signs.";
            severity = "CRITICAL";
            recipientRole = "DOCTOR";

        } else if (vital.getSpo2() < 94 || vital.getSystolicBp() < 90
                || vital.getHeartRate() > 120 || vital.getHeartRate() < 50) {

            alertType = "ABNORMAL_VITAL";
            message = "Abnormal patient vital signs detected.";
            severity = "HIGH";
            recipientRole = "DOCTOR";
        }

        if (alertType == null) {
            return false;
        }

        Alert alert = new Alert();

        alert.setCaseId(caseId);
        alert.setAlertType(alertType);
        alert.setAlertMessage(message);
        alert.setSeverity(severity);
        alert.setRecipientRole(recipientRole);
        alert.setRead(false);

        return alertDAO.addAlert(alert);
    }

    public boolean createHospitalFullAlert(int caseId) {

        Alert alert = new Alert();

        alert.setCaseId(caseId);
        alert.setAlertType("HOSPITAL_FULL");
        alert.setAlertMessage("Selected hospital currently has limited or no emergency capacity.");
        alert.setSeverity("HIGH");
        alert.setRecipientRole("DISPATCHER");
        alert.setRead(false);

        return alertDAO.addAlert(alert);
    }

    public boolean createArrivalAlert(int caseId) {

        Alert alert = new Alert();

        alert.setCaseId(caseId);
        alert.setAlertType("ARRIVAL");
        alert.setAlertMessage("Ambulance has arrived at the hospital.");
        alert.setSeverity("MEDIUM");
        alert.setRecipientRole("HOSPITAL");
        alert.setRead(false);

        return alertDAO.addAlert(alert);
    }
}