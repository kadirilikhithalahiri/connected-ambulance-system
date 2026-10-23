package com.abc.emergency;

import com.abc.emergency.model.Patient;
import com.abc.emergency.service.PatientService;

import java.util.List;

public class PatientRetrievalTest {

    public static void main(String[] args) {

        PatientService patientService = new PatientService();

        Patient patient = patientService.getPatient(1);

        System.out.println("========== PATIENT BY ID ==========");

        if (patient != null) {
            System.out.println("Patient ID: " + patient.getPatientId());
            System.out.println("Name: " + patient.getPatientName());
            System.out.println("Age: " + patient.getAge());
            System.out.println("Gender: " + patient.getGender());
            System.out.println("Blood Group: " + patient.getBloodGroup());
            System.out.println("Emergency Contact: " + patient.getEmergencyContact());
        } else {
            System.out.println("Patient not found.");
        }

        System.out.println();
        System.out.println("========== ALL PATIENTS ==========");

        List<Patient> patients = patientService.getAllPatients();

        for (Patient p : patients) {
            System.out.println(
                    p.getPatientId() + " - "
                    + p.getPatientName() + " - "
                    + p.getAge() + " - "
                    + p.getGender()
            );
        }

        System.out.println();
        System.out.println("Patient Retrieval Test: "
                + (patient != null && !patients.isEmpty() ? "SUCCESS" : "FAILED"));
    }
}