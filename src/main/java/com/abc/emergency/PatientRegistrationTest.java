package com.abc.emergency;

import com.abc.emergency.service.PatientService;

public class PatientRegistrationTest {

    public static void main(String[] args) {

        PatientService patientService = new PatientService();

        boolean result = patientService.registerPatient(
                "Ananya Sharma",
                25,
                "FEMALE",
                "O+",
                "No major medical history",
                "No known allergies",
                "9876543211"
        );

        System.out.println();
        System.out.println("Patient Registration Test: "
                + (result ? "SUCCESS" : "FAILED"));
    }
}