package com.abc.emergency.dto;

import com.abc.emergency.model.Patient;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PatientResponse {

    private int patientId;
    private String patientName;
    private int age;
    private String gender;
    private String bloodGroup;
    private String medicalHistory;
    private String allergies;
    private String emergencyContact;

    public PatientResponse() {
    }

    public PatientResponse(int patientId, String patientName, int age, String gender,
                           String bloodGroup, String medicalHistory, String allergies,
                           String emergencyContact) {
        this.patientId = patientId;
        this.patientName = patientName;
        this.age = age;
        this.gender = gender;
        this.bloodGroup = bloodGroup;
        this.medicalHistory = medicalHistory;
        this.allergies = allergies;
        this.emergencyContact = emergencyContact;
    }

    public static PatientResponse from(Patient patient) {
        if (patient == null) {
            return null;
        }
        return new PatientResponse(
                patient.getPatientId(),
                patient.getPatientName(),
                patient.getAge(),
                patient.getGender(),
                patient.getBloodGroup(),
                patient.getMedicalHistory(),
                patient.getAllergies(),
                patient.getEmergencyContact()
        );
    }

    public static PatientResponse summary(Patient patient) {
        if (patient == null) {
            return null;
        }
        return new PatientResponse(
                patient.getPatientId(),
                patient.getPatientName(),
                patient.getAge(),
                patient.getGender(),
                null, // bloodGroup omitted for dispatchers
                null, // medicalHistory omitted for dispatchers
                null, // allergies omitted for dispatchers
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

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public String getMedicalHistory() {
        return medicalHistory;
    }

    public void setMedicalHistory(String medicalHistory) {
        this.medicalHistory = medicalHistory;
    }

    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }
}
