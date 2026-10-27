package com.abc.emergency.dto;

import com.abc.emergency.model.Patient;

public class PatientSummaryResponse {

    private int patientId;
    private String patientName;
    private int age;
    private String gender;
    private String emergencyContact;

    public PatientSummaryResponse() {
    }

    public PatientSummaryResponse(int patientId, String patientName, int age, String gender, String emergencyContact) {
        this.patientId = patientId;
        this.patientName = patientName;
        this.age = age;
        this.gender = gender;
        this.emergencyContact = emergencyContact;
    }

    public static PatientSummaryResponse from(Patient patient) {
        if (patient == null) {
            return null;
        }
        return new PatientSummaryResponse(
                patient.getPatientId(),
                patient.getPatientName(),
                patient.getAge(),
                patient.getGender(),
                patient.getEmergencyContact()
        );
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }
}
