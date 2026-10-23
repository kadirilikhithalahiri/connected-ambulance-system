package com.abc.emergency;

import com.abc.emergency.model.Patient;
import com.abc.emergency.service.PatientService;

public class PatientUpdateTest {

    public static void main(String[] args) {

        PatientService patientService = new PatientService();

        Patient patient = patientService.getPatient(1);

        if (patient == null) {
            System.out.println("Patient not found.");
            return;
        }

        patient.setAge(26);
        patient.setMedicalHistory("No major medical history. Regular health checkup.");
        patient.setEmergencyContact("9876543212");

        boolean result = patientService.updatePatient(patient);

        System.out.println();
        System.out.println("Patient Update Test: "
                + (result ? "SUCCESS" : "FAILED"));

        Patient updatedPatient = patientService.getPatient(1);

        if (updatedPatient != null) {
            System.out.println("Patient ID: " + updatedPatient.getPatientId());
            System.out.println("Name: " + updatedPatient.getPatientName());
            System.out.println("Updated Age: " + updatedPatient.getAge());
            System.out.println("Updated Medical History: "
                    + updatedPatient.getMedicalHistory());
            System.out.println("Updated Emergency Contact: "
                    + updatedPatient.getEmergencyContact());
        }
    }
}