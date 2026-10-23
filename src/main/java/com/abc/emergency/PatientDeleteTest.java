package com.abc.emergency;

import com.abc.emergency.model.Patient;
import com.abc.emergency.service.PatientService;

public class PatientDeleteTest {

    public static void main(String[] args) {

        PatientService patientService = new PatientService();

        boolean registrationResult = patientService.registerPatient(
                "Test Patient",
                30,
                "MALE",
                "A+",
                "Temporary test patient",
                "None",
                "9876543213"
        );

        if (!registrationResult) {
            System.out.println("Test patient registration failed.");
            return;
        }

        Patient testPatient = patientService.getAllPatients()
                .stream()
                .filter(p -> "Test Patient".equals(p.getPatientName()))
                .findFirst()
                .orElse(null);

        if (testPatient == null) {
            System.out.println("Test patient not found.");
            return;
        }

        System.out.println("Test Patient ID: " + testPatient.getPatientId());

        boolean deleteResult =
                patientService.deletePatient(testPatient.getPatientId());

        System.out.println();
        System.out.println("Patient Delete Test: "
                + (deleteResult ? "SUCCESS" : "FAILED"));

        Patient deletedPatient =
                patientService.getPatient(testPatient.getPatientId());

        System.out.println("Patient After Delete: "
                + (deletedPatient == null ? "NOT FOUND" : "STILL EXISTS"));
    }
}