package com.abc.emergency.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class PatientRequest {

    @NotBlank(message = "Patient name cannot be empty.")
    @Pattern(regexp = "^[a-zA-Z ]+$", message = "Patient name must contain only letters and spaces.")
    private String patientName;

    @Min(value = 1, message = "Age must be greater than 0.")
    @Max(value = 120, message = "Age cannot exceed 120.")
    private int age;

    @NotBlank(message = "Gender cannot be empty.")
    @Pattern(regexp = "(?i)^(MALE|FEMALE|OTHER)$", message = "Gender must be MALE, FEMALE, or OTHER.")
    private String gender;

    private String bloodGroup;
    private String medicalHistory;
    private String allergies;

    @NotBlank(message = "Emergency contact cannot be empty.")
    @Pattern(regexp = "^\\d{10,15}$", message = "Emergency contact must be between 10 and 15 digits.")
    private String emergencyContact;

    public PatientRequest() {
    }

    public PatientRequest(String patientName, int age, String gender, String bloodGroup,
                          String medicalHistory, String allergies, String emergencyContact) {
        this.patientName = patientName;
        this.age = age;
        this.gender = gender;
        this.bloodGroup = bloodGroup;
        this.medicalHistory = medicalHistory;
        this.allergies = allergies;
        this.emergencyContact = emergencyContact;
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
