package com.abc.emergency.web;

import com.abc.emergency.config.WebSecurityConfig;
import com.abc.emergency.dao.UserDAO;
import com.abc.emergency.dto.PatientRequest;
import com.abc.emergency.model.Patient;
import com.abc.emergency.model.User;
import com.abc.emergency.service.PatientService;
import com.abc.emergency.service.ServiceResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PatientController.class, GlobalExceptionHandler.class})
@Import({WebSecurityConfig.class, PatientControllerTest.TestConfig.class})
public class PatientControllerTest {

    static class FakePatientService extends PatientService {
        final Map<Integer, Patient> store = new LinkedHashMap<>();
        private String fakeLastError;

        public FakePatientService() {
            reset();
        }

        public void reset() {
            store.clear();
            store.put(1, new Patient(1, "John Doe", 35, "MALE", "O+", "Hypertension", "Penicillin", "9876543210"));
            store.put(2, new Patient(2, "Jane Smith", 28, "FEMALE", "A+", "Asthma", "None", "9876543211"));
            fakeLastError = null;
        }

        @Override
        public List<Patient> getAllPatients() {
            return new ArrayList<>(store.values());
        }

        @Override
        public ServiceResult<List<Patient>> getPatientsForCallerResult(int userId, String role) {
            if (fakeLastError != null && fakeLastError.contains("DB_LIST_FAIL")) {
                return ServiceResult.databaseError("DATABASE_ERROR: Connection failed.");
            }
            if ("PARAMEDIC".equalsIgnoreCase(role)) {
                // Paramedic 201 only has encounter for Patient 1; Patient 2 is unrelated
                if (userId == 201 && store.containsKey(1)) {
                    return ServiceResult.success("Patients retrieved successfully.", List.of(store.get(1)));
                }
                return ServiceResult.success("Patients retrieved successfully.", Collections.emptyList());
            }
            if ("AMBULANCE_DRIVER".equalsIgnoreCase(role) || "HOSPITAL_ADMIN".equalsIgnoreCase(role)) {
                return ServiceResult.success("Patients retrieved successfully.", Collections.emptyList());
            }
            return ServiceResult.success("Patients retrieved successfully.", new ArrayList<>(store.values()));
        }

        @Override
        public List<Patient> getPatientsForCaller(int userId, String role) {
            ServiceResult<List<Patient>> res = getPatientsForCallerResult(userId, role);
            return res.isSuccess() ? res.getData() : Collections.emptyList();
        }

        @Override
        public Patient getPatient(int patientId) {
            return store.get(patientId);
        }

        @Override
        public ServiceResult<Patient> getPatientByIdForCaller(int patientId, int userId, String role) {
            if (patientId == 888) {
                return ServiceResult.databaseError("DATABASE_ERROR: Connection failed.");
            }
            Patient p = store.get(patientId);
            if (p == null) {
                return ServiceResult.notFound("Patient not found with ID: " + patientId);
            }
            if ("PARAMEDIC".equalsIgnoreCase(role)) {
                // Paramedic 201 only authorized for Patient 1
                if (userId != 201 || patientId != 1) {
                    return ServiceResult.notFound("Patient not found with ID: " + patientId);
                }
            }
            if ("AMBULANCE_DRIVER".equalsIgnoreCase(role) || "HOSPITAL_ADMIN".equalsIgnoreCase(role)) {
                return ServiceResult.notFound("Patient not found with ID: " + patientId);
            }
            return ServiceResult.success("Patient retrieved successfully.", p);
        }

        @Override
        public ServiceResult<Patient> createPatientResult(String patientName, int age, String gender,
                                                         String bloodGroup, String medicalHistory,
                                                         String allergies, String emergencyContact) {
            if ("DatabaseError".equals(patientName)) {
                fakeLastError = "DATABASE_ERROR: Simulated DB failure.";
                return ServiceResult.databaseError(fakeLastError);
            }
            if ("FailRegistration".equals(patientName)) {
                fakeLastError = "Validation failed: invalid registration.";
                return ServiceResult.validationError(fakeLastError);
            }
            int newId = store.size() + 1;
            Patient p = new Patient(newId, patientName, age, gender, bloodGroup, medicalHistory, allergies, emergencyContact);
            store.put(newId, p);
            fakeLastError = null;
            return ServiceResult.success("Patient registered successfully.", p);
        }

        @Override
        public Patient createPatient(String patientName, int age, String gender,
                                    String bloodGroup, String medicalHistory,
                                    String allergies, String emergencyContact) {
            ServiceResult<Patient> res = createPatientResult(patientName, age, gender, bloodGroup, medicalHistory, allergies, emergencyContact);
            return res.isSuccess() ? res.getData() : null;
        }

        @Override
        public boolean registerPatient(String patientName, int age, String gender,
                                       String bloodGroup, String medicalHistory,
                                       String allergies, String emergencyContact) {
            return createPatientResult(patientName, age, gender, bloodGroup, medicalHistory, allergies, emergencyContact).isSuccess();
        }

        @Override
        public ServiceResult<Patient> updatePatientResult(Patient patient) {
            if (patient == null || !store.containsKey(patient.getPatientId())) {
                fakeLastError = "Patient not found.";
                return ServiceResult.notFound(fakeLastError);
            }
            if ("DatabaseError".equals(patient.getPatientName())) {
                fakeLastError = "DATABASE_ERROR: Simulated DB update failure.";
                return ServiceResult.databaseError(fakeLastError);
            }
            if ("FailUpdate".equals(patient.getPatientName())) {
                fakeLastError = "Validation failed.";
                return ServiceResult.validationError(fakeLastError);
            }
            store.put(patient.getPatientId(), patient);
            fakeLastError = null;
            return ServiceResult.success("Patient updated successfully.", patient);
        }

        @Override
        public ServiceResult<Patient> updatePatientResultForCaller(Patient patient, int userId, String role) {
            if (patient == null || !store.containsKey(patient.getPatientId())) {
                fakeLastError = "Patient not found with ID: " + (patient != null ? patient.getPatientId() : 0);
                return ServiceResult.notFound(fakeLastError);
            }
            if ("PARAMEDIC".equalsIgnoreCase(role)) {
                // Paramedic 201 only authorized for Patient 1; Patient 2 is unrelated
                if (userId != 201 || patient.getPatientId() != 1) {
                    fakeLastError = "Patient not found with ID: " + patient.getPatientId();
                    return ServiceResult.notFound(fakeLastError);
                }
            }
            if ("AMBULANCE_DRIVER".equalsIgnoreCase(role) || "HOSPITAL_ADMIN".equalsIgnoreCase(role) || "DISPATCHER".equalsIgnoreCase(role)) {
                fakeLastError = "Access denied. Insufficient role permissions to update patient.";
                return ServiceResult.validationError(fakeLastError);
            }
            return updatePatientResult(patient);
        }

        @Override
        public boolean updatePatient(Patient patient) {
            return updatePatientResult(patient).isSuccess();
        }

        @Override
        public String getLastErrorMessage() {
            return fakeLastError;
        }
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public PatientService patientService() {
            return new FakePatientService();
        }

        @Bean
        public UserDAO userDAO() {
            return new UserDAO() {
                @Override
                public User getUserByUsername(String username) {
                    if ("driver01".equalsIgnoreCase(username)) {
                        User u = new User();
                        u.setUserId(101);
                        u.setUsername("driver01");
                        u.setRole("AMBULANCE_DRIVER");
                        return u;
                    }
                    if ("driver02".equalsIgnoreCase(username)) {
                        User u = new User();
                        u.setUserId(102);
                        u.setUsername("driver02");
                        u.setRole("AMBULANCE_DRIVER");
                        return u;
                    }
                    if ("paramedic01".equalsIgnoreCase(username)) {
                        User u = new User();
                        u.setUserId(201);
                        u.setUsername("paramedic01");
                        u.setRole("PARAMEDIC");
                        return u;
                    }
                    if ("paramedic02".equalsIgnoreCase(username)) {
                        User u = new User();
                        u.setUserId(202);
                        u.setUsername("paramedic02");
                        u.setRole("PARAMEDIC");
                        return u;
                    }
                    return null;
                }
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PatientService patientService;

    @BeforeEach
    void setUp() {
        if (patientService instanceof FakePatientService fake) {
            fake.reset();
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/patients (List) & Field Exposure Minimization
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "paramedic01", roles = {"PARAMEDIC"})
    void testGetAllPatients_Paramedic_ReceivesFullClinicalDetails() throws Exception {
        mockMvc.perform(get("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Patients retrieved successfully."))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].patientId").value(1))
                .andExpect(jsonPath("$.data[0].patientName").value("John Doe"))
                .andExpect(jsonPath("$.data[0].bloodGroup").value("O+"))
                .andExpect(jsonPath("$.data[0].medicalHistory").value("Hypertension"))
                .andExpect(jsonPath("$.data[0].allergies").value("Penicillin"));
    }

    @Test
    @WithMockUser(username = "doctor01", roles = {"EMERGENCY_DOCTOR"})
    void testGetAllPatients_Doctor_ReceivesFullClinicalDetails() throws Exception {
        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].bloodGroup").value("O+"))
                .andExpect(jsonPath("$.data[0].medicalHistory").value("Hypertension"))
                .andExpect(jsonPath("$.data[0].allergies").value("Penicillin"));
    }

    @Test
    @WithMockUser(username = "admin01", roles = {"ADMIN"})
    void testGetAllPatients_Admin_ReceivesFullClinicalDetails() throws Exception {
        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].bloodGroup").value("O+"));
    }

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testGetAllPatients_Dispatcher_ReceivesSummaryWithoutClinicalDetails() throws Exception {
        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].patientId").value(1))
                .andExpect(jsonPath("$.data[0].patientName").value("John Doe"))
                .andExpect(jsonPath("$.data[0].emergencyContact").value("9876543210"))
                .andExpect(jsonPath("$.data[0].bloodGroup").doesNotExist())
                .andExpect(jsonPath("$.data[0].medicalHistory").doesNotExist())
                .andExpect(jsonPath("$.data[0].allergies").doesNotExist());
    }

    @Test
    @WithMockUser(username = "driver01", roles = {"AMBULANCE_DRIVER"})
    void testGetAllPatients_ForbiddenRole_AmbulanceDriver_Returns403() throws Exception {
        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(containsString("Access denied")));
    }

    @Test
    @WithMockUser(username = "hospital01", roles = {"HOSPITAL_ADMIN"})
    void testGetAllPatients_ForbiddenRole_HospitalAdmin_Returns403() throws Exception {
        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void testGetAllPatients_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(containsString("Authentication required")));
    }

    // -------------------------------------------------------------------------
    // GET /api/patients/{id} (Detail) & Field Exposure Minimization
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "paramedic01", roles = {"PARAMEDIC"})
    void testGetPatientById_Paramedic_ReceivesFullClinicalDetails() throws Exception {
        mockMvc.perform(get("/api/patients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.patientId").value(1))
                .andExpect(jsonPath("$.data.patientName").value("John Doe"))
                .andExpect(jsonPath("$.data.age").value(35))
                .andExpect(jsonPath("$.data.gender").value("MALE"))
                .andExpect(jsonPath("$.data.bloodGroup").value("O+"))
                .andExpect(jsonPath("$.data.medicalHistory").value("Hypertension"))
                .andExpect(jsonPath("$.data.allergies").value("Penicillin"))
                .andExpect(jsonPath("$.data.emergencyContact").value("9876543210"));
    }

    @Test
    @WithMockUser(username = "paramedic01", roles = {"PARAMEDIC"})
    void testGetPatientById_Paramedic_UnrelatedPatient_Returns404NotFound() throws Exception {
        // Patient 2 is an unrelated patient outside paramedic01's assigned encounter history
        mockMvc.perform(get("/api/patients/2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("Patient not found with ID: 2")));
    }

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testGetPatientById_Dispatcher_ReceivesSummaryWithoutClinicalDetails() throws Exception {
        mockMvc.perform(get("/api/patients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.patientId").value(1))
                .andExpect(jsonPath("$.data.patientName").value("John Doe"))
                .andExpect(jsonPath("$.data.age").value(35))
                .andExpect(jsonPath("$.data.gender").value("MALE"))
                .andExpect(jsonPath("$.data.emergencyContact").value("9876543210"))
                .andExpect(jsonPath("$.data.bloodGroup").doesNotExist())
                .andExpect(jsonPath("$.data.medicalHistory").doesNotExist())
                .andExpect(jsonPath("$.data.allergies").doesNotExist());
    }

    @Test
    @WithMockUser(username = "paramedic01", roles = {"PARAMEDIC"})
    void testGetPatientById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/patients/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("Patient not found with ID: 999")));
    }

    @Test
    @WithMockUser(username = "paramedic01", roles = {"PARAMEDIC"})
    void testGetPatientById_InvalidId_Returns400() throws Exception {
        mockMvc.perform(get("/api/patients/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("Invalid patient ID")));
    }

    @Test
    @WithMockUser(username = "driver01", roles = {"AMBULANCE_DRIVER"})
    void testGetPatientById_ForbiddenRole_Returns403() throws Exception {
        mockMvc.perform(get("/api/patients/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // -------------------------------------------------------------------------
    // POST /api/patients (Create) & Actual Generated ID / Error Classification
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "paramedic01", roles = {"PARAMEDIC"})
    void testCreatePatient_Paramedic_ReturnsRealGeneratedIdGreaterThanZero() throws Exception {
        PatientRequest request = new PatientRequest(
                "Alice Wonder", 30, "FEMALE", "B+", "None", "None", "9876543219"
        );

        mockMvc.perform(post("/api/patients")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Patient registered successfully."))
                .andExpect(jsonPath("$.data.patientId").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.patientName").value("Alice Wonder"))
                .andExpect(jsonPath("$.data.gender").value("FEMALE"));
    }

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testCreatePatient_Dispatcher_ReturnsRealGeneratedId() throws Exception {
        PatientRequest request = new PatientRequest(
                "Bob Builder", 45, "MALE", "AB+", "Diabetes", "Peanuts", "9876543220"
        );

        mockMvc.perform(post("/api/patients")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.patientId").value(greaterThan(0)));
    }

    @Test
    @WithMockUser(username = "paramedic01", roles = {"PARAMEDIC"})
    void testCreatePatient_DatabaseFailure_Returns500InternalServerError() throws Exception {
        // Name 'DatabaseError' triggers simulated persistence failure in FakePatientService
        PatientRequest request = new PatientRequest(
                "DatabaseError", 30, "FEMALE", "B+", "None", "None", "9876543219"
        );

        mockMvc.perform(post("/api/patients")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value(containsString("An error occurred while saving the patient record")));
    }

    @Test
    @WithMockUser(username = "paramedic01", roles = {"PARAMEDIC"})
    void testCreatePatient_ValidationFailure_BlankName_Returns400() throws Exception {
        PatientRequest request = new PatientRequest(
                "", 30, "FEMALE", "B+", "None", "None", "9876543219"
        );

        mockMvc.perform(post("/api/patients")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data.patientName").exists());
    }

    @Test
    @WithMockUser(username = "paramedic01", roles = {"PARAMEDIC"})
    void testCreatePatient_ValidationFailure_InvalidAge_Returns400() throws Exception {
        PatientRequest request = new PatientRequest(
                "Alice Wonder", 150, "FEMALE", "B+", "None", "None", "9876543219"
        );

        mockMvc.perform(post("/api/patients")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data.age").exists());
    }

    @Test
    @WithMockUser(username = "paramedic01", roles = {"PARAMEDIC"})
    void testCreatePatient_ValidationFailure_InvalidGender_Returns400() throws Exception {
        PatientRequest request = new PatientRequest(
                "Alice Wonder", 30, "UNKNOWN_GENDER", "B+", "None", "None", "9876543219"
        );

        mockMvc.perform(post("/api/patients")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data.gender").exists());
    }

    @Test
    @WithMockUser(username = "paramedic01", roles = {"PARAMEDIC"})
    void testCreatePatient_ValidationFailure_InvalidPhone_Returns400() throws Exception {
        PatientRequest request = new PatientRequest(
                "Alice Wonder", 30, "FEMALE", "B+", "None", "None", "12345"
        );

        mockMvc.perform(post("/api/patients")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data.emergencyContact").exists());
    }

    @Test
    @WithMockUser(username = "doctor01", roles = {"EMERGENCY_DOCTOR"})
    void testCreatePatient_ForbiddenRole_Doctor_Returns403() throws Exception {
        PatientRequest request = new PatientRequest(
                "Alice Wonder", 30, "FEMALE", "B+", "None", "None", "9876543219"
        );

        mockMvc.perform(post("/api/patients")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // -------------------------------------------------------------------------
    // PUT /api/patients/{id} (Update) & Error Classification
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "doctor01", roles = {"EMERGENCY_DOCTOR"})
    void testUpdatePatient_Doctor_Success() throws Exception {
        PatientRequest request = new PatientRequest(
                "John Doe Updated", 36, "MALE", "O+", "Hypertension controlled", "Penicillin", "9876543210"
        );

        mockMvc.perform(put("/api/patients/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Patient updated successfully."))
                .andExpect(jsonPath("$.data.patientId").value(1))
                .andExpect(jsonPath("$.data.patientName").value("John Doe Updated"))
                .andExpect(jsonPath("$.data.age").value(36));
    }

    @Test
    @WithMockUser(username = "paramedic01", roles = {"PARAMEDIC"})
    void testUpdatePatient_Paramedic_AuthorizedPatient_Success() throws Exception {
        PatientRequest request = new PatientRequest(
                "John Doe Medic Updated", 36, "MALE", "O+", "Hypertension", "Penicillin", "9876543210"
        );

        mockMvc.perform(put("/api/patients/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Patient updated successfully."))
                .andExpect(jsonPath("$.data.patientId").value(1))
                .andExpect(jsonPath("$.data.patientName").value("John Doe Medic Updated"));
    }

    @Test
    @WithMockUser(username = "paramedic01", roles = {"PARAMEDIC"})
    void testUpdatePatient_Paramedic_UnrelatedPatient_Returns404NotFound() throws Exception {
        // Patient 2 is an unrelated patient outside paramedic01's assigned encounter history
        PatientRequest request = new PatientRequest(
                "Jane Smith Hacked", 29, "FEMALE", "A+", "None", "None", "9876543211"
        );

        mockMvc.perform(put("/api/patients/2")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("Patient not found with ID: 2")));
    }

    @Test
    @WithMockUser(username = "doctor01", roles = {"EMERGENCY_DOCTOR"})
    void testUpdatePatient_DatabaseFailure_Returns500() throws Exception {
        // Name 'DatabaseError' triggers simulated DB update failure in FakePatientService
        PatientRequest request = new PatientRequest(
                "DatabaseError", 35, "MALE", "O+", "None", "None", "9876543210"
        );

        mockMvc.perform(put("/api/patients/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value(containsString("An error occurred while updating the patient record")));
    }

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testUpdatePatient_ForbiddenRole_Dispatcher_Returns403() throws Exception {
        PatientRequest request = new PatientRequest(
                "John Doe", 35, "MALE", "O+", "Hypertension", "Penicillin", "9876543210"
        );

        mockMvc.perform(put("/api/patients/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "doctor01", roles = {"EMERGENCY_DOCTOR"})
    void testUpdatePatient_NotFound_Returns404() throws Exception {
        PatientRequest request = new PatientRequest(
                "Unknown Person", 35, "MALE", "O+", "None", "None", "9876543210"
        );

        mockMvc.perform(put("/api/patients/999")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("Patient not found with ID: 999")));
    }

    @Test
    @WithMockUser(username = "doctor01", roles = {"EMERGENCY_DOCTOR"})
    void testUpdatePatient_InvalidId_Returns400() throws Exception {
        PatientRequest request = new PatientRequest(
                "John Doe", 35, "MALE", "O+", "None", "None", "9876543210"
        );

        mockMvc.perform(put("/api/patients/-1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    void testGetAllPatients_DatabaseError_Returns500() throws Exception {
        ((FakePatientService) patientService).fakeLastError = "DB_LIST_FAIL";
        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value(containsString("An error occurred while retrieving patient records")));
    }

    @Test
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    void testGetPatientById_DatabaseError_Returns500() throws Exception {
        mockMvc.perform(get("/api/patients/888"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value(containsString("An error occurred while retrieving the patient record")));
    }

    @Test
    @WithMockUser(username = "unresolvableParamedic", roles = {"PARAMEDIC"})
    void testGetAllPatients_unresolvableParamedic_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void testGetAllPatients_domainUserPrincipal_success() throws Exception {
        User paramedicUser = new User();
        paramedicUser.setUserId(201);
        paramedicUser.setUsername("paramedicDirect");
        paramedicUser.setRole("PARAMEDIC");

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                paramedicUser, null, List.of(new SimpleGrantedAuthority("ROLE_PARAMEDIC"))
        );

        mockMvc.perform(get("/api/patients").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }
}
