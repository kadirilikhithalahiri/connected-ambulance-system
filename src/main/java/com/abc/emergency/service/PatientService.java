package com.abc.emergency.service;

import com.abc.emergency.dao.PatientDAO;
import com.abc.emergency.model.Patient;
import com.abc.emergency.util.ValidationUtil;

import java.util.List;

public class PatientService {

    private final PatientDAO patientDAO;

    public PatientService() {
        patientDAO = new PatientDAO();
    }

    public boolean registerPatient(String patientName, int age, String gender,
                                   String bloodGroup, String medicalHistory,
                                   String allergies, String emergencyContact) {

        if (!ValidationUtil.isValidName(patientName)) {
            System.out.println("Invalid patient name.");
            return false;
        }

        if (!ValidationUtil.isValidAge(age)) {
            System.out.println("Invalid patient age.");
            return false;
        }

        if (gender == null || gender.trim().isEmpty()) {
            System.out.println("Gender cannot be empty.");
            return false;
        }

        gender = gender.toUpperCase();

        if (!gender.equals("MALE")
                && !gender.equals("FEMALE")
                && !gender.equals("OTHER")) {
            System.out.println("Invalid gender.");
            return false;
        }

        if (!ValidationUtil.isValidPhone(emergencyContact)) {
            System.out.println("Invalid emergency contact.");
            return false;
        }

        Patient patient = new Patient();

        patient.setPatientName(patientName);
        patient.setAge(age);
        patient.setGender(gender);
        patient.setBloodGroup(bloodGroup);
        patient.setMedicalHistory(medicalHistory);
        patient.setAllergies(allergies);
        patient.setEmergencyContact(emergencyContact);

        boolean result = patientDAO.addPatient(patient);

        if (result) {
            System.out.println("Patient registered successfully.");
        } else {
            System.out.println("Patient registration failed.");
        }

        return result;
    }

    public Patient getPatient(int patientId) {

        if (!ValidationUtil.isValidId(patientId)) {
            System.out.println("Invalid patient ID.");
            return null;
        }

        return patientDAO.getPatientById(patientId);
    }

    public List<Patient> getAllPatients() {
        return patientDAO.getAllPatients();
    }

    public boolean updatePatient(Patient patient) {

        if (patient == null) {
            return false;
        }

        if (!ValidationUtil.isValidId(patient.getPatientId())) {
            System.out.println("Invalid patient ID.");
            return false;
        }

        if (!ValidationUtil.isValidName(patient.getPatientName())) {
            System.out.println("Invalid patient name.");
            return false;
        }

        if (!ValidationUtil.isValidAge(patient.getAge())) {
            System.out.println("Invalid patient age.");
            return false;
        }

        if (!ValidationUtil.isValidPhone(patient.getEmergencyContact())) {
            System.out.println("Invalid emergency contact.");
            return false;
        }

        return patientDAO.updatePatient(patient);
    }

    public boolean deletePatient(int patientId) {

        if (!ValidationUtil.isValidId(patientId)) {
            System.out.println("Invalid patient ID.");
            return false;
        }

        return patientDAO.deletePatient(patientId);
    }
}