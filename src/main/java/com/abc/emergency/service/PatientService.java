package com.abc.emergency.service;

import com.abc.emergency.dao.PatientDAO;
import com.abc.emergency.model.Patient;
import com.abc.emergency.util.ValidationUtil;

import java.util.Collections;
import java.util.List;

public class PatientService {

    private final PatientDAO patientDAO;
    private final ThreadLocal<String> lastErrorMessage = new ThreadLocal<>();

    public PatientService() {
        this(new PatientDAO());
    }

    public PatientService(PatientDAO patientDAO) {
        this.patientDAO = patientDAO;
    }

    public ServiceResult<Patient> createPatientResult(String patientName, int age, String gender,
                                                     String bloodGroup, String medicalHistory,
                                                     String allergies, String emergencyContact) {

        if (!ValidationUtil.isValidName(patientName)) {
            String err = "Invalid patient name. Name must contain only letters and spaces.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        if (!ValidationUtil.isValidAge(age)) {
            String err = "Invalid patient age. Age must be between 1 and 120.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        if (gender == null || gender.trim().isEmpty()) {
            String err = "Gender cannot be empty.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        String normalizedGender = gender.toUpperCase();

        if (!normalizedGender.equals("MALE")
                && !normalizedGender.equals("FEMALE")
                && !normalizedGender.equals("OTHER")) {
            String err = "Invalid gender. Allowed values: MALE, FEMALE, OTHER.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        if (!ValidationUtil.isValidPhone(emergencyContact)) {
            String err = "Invalid emergency contact. Must be 10 to 15 digits.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        Patient patient = new Patient();

        patient.setPatientName(patientName);
        patient.setAge(age);
        patient.setGender(normalizedGender);
        patient.setBloodGroup(bloodGroup);
        patient.setMedicalHistory(medicalHistory);
        patient.setAllergies(allergies);
        patient.setEmergencyContact(emergencyContact);

        boolean result = patientDAO.addPatient(patient);

        if (result && patient.getPatientId() > 0) {
            lastErrorMessage.remove();
            return ServiceResult.success("Patient registered successfully.", patient);
        } else {
            String err = "DATABASE_ERROR: Unable to save patient record to database.";
            lastErrorMessage.set(err);
            return ServiceResult.databaseError(err);
        }
    }

    public Patient createPatient(String patientName, int age, String gender,
                                String bloodGroup, String medicalHistory,
                                String allergies, String emergencyContact) {
        ServiceResult<Patient> result = createPatientResult(patientName, age, gender, bloodGroup, medicalHistory, allergies, emergencyContact);
        return result.isSuccess() ? result.getData() : null;
    }

    public boolean registerPatient(String patientName, int age, String gender,
                                   String bloodGroup, String medicalHistory,
                                   String allergies, String emergencyContact) {
        return createPatientResult(patientName, age, gender, bloodGroup, medicalHistory, allergies, emergencyContact).isSuccess();
    }

    public Patient getPatient(int patientId) {

        if (!ValidationUtil.isValidId(patientId)) {
            lastErrorMessage.set("Invalid patient ID.");
            return null;
        }

        return patientDAO.getPatientById(patientId);
    }

    public List<Patient> getAllPatients() {
        return patientDAO.getAllPatients();
    }

    public List<Patient> getPatientsForCaller(int userId, String role) {
        ServiceResult<List<Patient>> result = getPatientsForCallerResult(userId, role);
        return result.isSuccess() ? result.getData() : Collections.emptyList();
    }

    public ServiceResult<List<Patient>> getPatientsForCallerResult(int userId, String role) {
        List<Patient> patients;

        if ("PARAMEDIC".equalsIgnoreCase(role)) {
            if (userId <= 0) {
                lastErrorMessage.remove();
                return ServiceResult.success("Patients retrieved successfully.", Collections.emptyList());
            }
            patients = patientDAO.getPatientsByAssignedUserId(userId);
        } else if ("AMBULANCE_DRIVER".equalsIgnoreCase(role) || "HOSPITAL_ADMIN".equalsIgnoreCase(role)) {
            lastErrorMessage.remove();
            return ServiceResult.success("Patients retrieved successfully.", Collections.emptyList());
        } else {
            patients = patientDAO.getAllPatients();
        }

        if (patientDAO.getLastDatabaseError() != null) {
            String err = "An internal database error occurred while retrieving patient records.";
            lastErrorMessage.set(err);
            return ServiceResult.databaseError(err);
        }

        lastErrorMessage.remove();
        return ServiceResult.success("Patients retrieved successfully.", patients);
    }

    public ServiceResult<Patient> getPatientByIdForCaller(int patientId, int userId, String role) {
        if (!ValidationUtil.isValidId(patientId)) {
            String err = "Invalid patient ID. ID must be greater than 0.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        Patient patient = patientDAO.getPatientById(patientId);
        if (patientDAO.getLastDatabaseError() != null) {
            String err = "An internal database error occurred while retrieving the patient record.";
            lastErrorMessage.set(err);
            return ServiceResult.databaseError(err);
        }

        if (patient == null) {
            String err = "Patient not found with ID: " + patientId;
            lastErrorMessage.set(err);
            return ServiceResult.notFound(err);
        }

        if ("PARAMEDIC".equalsIgnoreCase(role)) {
            if (userId <= 0) {
                String err = "Patient not found with ID: " + patientId;
                lastErrorMessage.set(err);
                return ServiceResult.notFound(err);
            }
            boolean assigned = patientDAO.isPatientAssignedToUser(patientId, userId);
            if (patientDAO.getLastDatabaseError() != null) {
                String err = "An internal database error occurred while verifying patient assignment.";
                lastErrorMessage.set(err);
                return ServiceResult.databaseError(err);
            }
            if (!assigned) {
                // Consistent safe response: avoid leaking existence of unrelated patient records
                String err = "Patient not found with ID: " + patientId;
                lastErrorMessage.set(err);
                return ServiceResult.notFound(err);
            }
        }

        if ("AMBULANCE_DRIVER".equalsIgnoreCase(role) || "HOSPITAL_ADMIN".equalsIgnoreCase(role)) {
            String err = "Patient not found with ID: " + patientId;
            lastErrorMessage.set(err);
            return ServiceResult.notFound(err);
        }

        lastErrorMessage.remove();
        return ServiceResult.success("Patient retrieved successfully.", patient);
    }

    public ServiceResult<Patient> updatePatientResultForCaller(Patient patient, int userId, String role) {
        if (patient == null) {
            String err = "Patient cannot be null.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        if (!ValidationUtil.isValidId(patient.getPatientId())) {
            String err = "Invalid patient ID. ID must be greater than 0.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        Patient existing = patientDAO.getPatientById(patient.getPatientId());
        if (patientDAO.getLastDatabaseError() != null) {
            String err = "An internal database error occurred while retrieving the patient record.";
            lastErrorMessage.set(err);
            return ServiceResult.databaseError(err);
        }

        if (existing == null) {
            String err = "Patient not found with ID: " + patient.getPatientId();
            lastErrorMessage.set(err);
            return ServiceResult.notFound(err);
        }

        if ("PARAMEDIC".equalsIgnoreCase(role)) {
            if (userId <= 0) {
                String err = "Patient not found with ID: " + patient.getPatientId();
                lastErrorMessage.set(err);
                return ServiceResult.notFound(err);
            }
            boolean assigned = patientDAO.isPatientAssignedToUser(patient.getPatientId(), userId);
            if (patientDAO.getLastDatabaseError() != null) {
                String err = "An internal database error occurred while verifying patient assignment.";
                lastErrorMessage.set(err);
                return ServiceResult.databaseError(err);
            }
            if (!assigned) {
                // Consistent safe response: avoid leaking existence of unrelated patient records
                String err = "Patient not found with ID: " + patient.getPatientId();
                lastErrorMessage.set(err);
                return ServiceResult.notFound(err);
            }
        }

        if ("AMBULANCE_DRIVER".equalsIgnoreCase(role) || "HOSPITAL_ADMIN".equalsIgnoreCase(role) || "DISPATCHER".equalsIgnoreCase(role)) {
            String err = "Access denied. Insufficient role permissions to update patient.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        return updatePatientResult(patient);
    }

    public ServiceResult<Patient> updatePatientResult(Patient patient) {

        if (patient == null) {
            String err = "Patient cannot be null.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        if (!ValidationUtil.isValidId(patient.getPatientId())) {
            String err = "Invalid patient ID.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        if (!ValidationUtil.isValidName(patient.getPatientName())) {
            String err = "Invalid patient name. Name must contain only letters and spaces.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        if (!ValidationUtil.isValidAge(patient.getAge())) {
            String err = "Invalid patient age. Age must be between 1 and 120.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        if (!ValidationUtil.isValidPhone(patient.getEmergencyContact())) {
            String err = "Invalid emergency contact. Must be 10 to 15 digits.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        boolean updated = patientDAO.updatePatient(patient);
        if (updated) {
            lastErrorMessage.remove();
            return ServiceResult.success("Patient updated successfully.", patient);
        } else {
            String err = "DATABASE_ERROR: Unable to update patient record in database.";
            lastErrorMessage.set(err);
            return ServiceResult.databaseError(err);
        }
    }

    public boolean updatePatient(Patient patient) {
        return updatePatientResult(patient).isSuccess();
    }

    public boolean deletePatient(int patientId) {

        if (!ValidationUtil.isValidId(patientId)) {
            lastErrorMessage.set("Invalid patient ID.");
            return false;
        }

        return patientDAO.deletePatient(patientId);
    }

    public String getLastErrorMessage() {
        return lastErrorMessage.get();
    }
}
