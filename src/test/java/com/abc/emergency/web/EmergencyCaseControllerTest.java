package com.abc.emergency.web;

import com.abc.emergency.config.WebSecurityConfig;
import com.abc.emergency.dao.UserDAO;
import com.abc.emergency.dto.CaseStatusUpdateRequest;
import com.abc.emergency.dto.CaseTriageRequest;
import com.abc.emergency.dto.CaseTriageResponse;
import com.abc.emergency.dto.EmergencyCaseRequest;
import com.abc.emergency.model.EmergencyCase;
import com.abc.emergency.model.PatientVital;
import com.abc.emergency.model.User;
import com.abc.emergency.service.EmergencyCaseService;
import com.abc.emergency.service.ServiceResult;
import com.abc.emergency.service.TriageService;
import com.abc.emergency.util.ValidationUtil;
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

@WebMvcTest(controllers = {EmergencyCaseController.class, GlobalExceptionHandler.class})
@Import({WebSecurityConfig.class, EmergencyCaseControllerTest.TestConfig.class})
public class EmergencyCaseControllerTest {

    static class FakeEmergencyCaseService extends EmergencyCaseService {
        final Map<Integer, EmergencyCase> store = new LinkedHashMap<>();
        private String fakeLastError;
        private boolean simulateConcurrentConflict = false;

        public FakeEmergencyCaseService() {
            reset();
        }

        public void setSimulateConcurrentConflict(boolean simulate) {
            this.simulateConcurrentConflict = simulate;
        }

        public void reset() {
            store.clear();
            store.put(1, new EmergencyCase(1, 1, "MG Road, Bengaluru", "CARDIAC", "Chest pain, breathlessness", "CRITICAL", "REGISTERED"));
            store.put(2, new EmergencyCase(2, 2, "Indiranagar, Bengaluru", "ACCIDENT", "Head injury, bleeding", "HIGH", "DISPATCHED"));
            store.put(3, new EmergencyCase(3, 1, "Jayanagar, Bengaluru", "RESPIRATORY", "Severe asthma attack", "CRITICAL", "IN_TRANSIT"));
            store.put(4, new EmergencyCase(4, 2, "Koramangala, Bengaluru", "TRAUMA", "Fracture", "MODERATE", "ARRIVED"));
            store.put(5, new EmergencyCase(5, 1, "Whitefield, Bengaluru", "CARDIAC", "Cardiac arrest recovery", "LOW", "COMPLETED"));
            store.put(6, new EmergencyCase(6, 2, "Electronic City, Bengaluru", "FALSE_ALARM", "Resolved on site", "LOW", "CANCELLED"));
            fakeLastError = null;
            simulateConcurrentConflict = false;
        }

        @Override
        public List<EmergencyCase> getAllEmergencyCases() {
            return new ArrayList<>(store.values());
        }

        @Override
        public EmergencyCase getEmergencyCaseById(int caseId) {
            return store.get(caseId);
        }

        @Override
        public ServiceResult<List<EmergencyCase>> getCasesForCallerResult(int userId, String role) {
            if (fakeLastError != null && fakeLastError.contains("DB_LIST_FAIL")) {
                return ServiceResult.databaseError("DATABASE_ERROR: Connection lost.");
            }
            if ("AMBULANCE_DRIVER".equalsIgnoreCase(role)) {
                if (userId == 101) {
                    // driver01 assigned only to case 1
                    return ServiceResult.success("Emergency cases retrieved successfully.", store.containsKey(1) ? List.of(store.get(1)) : Collections.emptyList());
                }
                return ServiceResult.success("Emergency cases retrieved successfully.", Collections.emptyList());
            }
            if ("PARAMEDIC".equalsIgnoreCase(role)) {
                if (userId == 201) {
                    // paramedic01 has encounters for cases 1, 2, 3, 4; case 5, 6 are unrelated
                    List<EmergencyCase> list = new ArrayList<>();
                    for (int id : List.of(1, 2, 3, 4)) {
                        if (store.containsKey(id)) list.add(store.get(id));
                    }
                    return ServiceResult.success("Emergency cases retrieved successfully.", list);
                }
                return ServiceResult.success("Emergency cases retrieved successfully.", Collections.emptyList());
            }
            if ("HOSPITAL_ADMIN".equalsIgnoreCase(role)) {
                return ServiceResult.success("Emergency cases retrieved successfully.", Collections.emptyList());
            }
            return ServiceResult.success("Emergency cases retrieved successfully.", new ArrayList<>(store.values()));
        }

        @Override
        public List<EmergencyCase> getCasesForCaller(int userId, String role) {
            ServiceResult<List<EmergencyCase>> res = getCasesForCallerResult(userId, role);
            return res.isSuccess() ? res.getData() : Collections.emptyList();
        }

        @Override
        public ServiceResult<EmergencyCase> getCaseByIdForCaller(int caseId, int userId, String role) {
            if (caseId == 888) {
                return ServiceResult.databaseError("DATABASE_ERROR: Connection failed.");
            }
            if (!store.containsKey(caseId)) {
                return ServiceResult.notFound("Emergency case not found with ID: " + caseId);
            }
            if ("AMBULANCE_DRIVER".equalsIgnoreCase(role)) {
                // Driver 101 only has access to case 1
                if (userId != 101 || caseId != 1) {
                    return ServiceResult.notFound("Emergency case not found with ID: " + caseId);
                }
            }
            if ("PARAMEDIC".equalsIgnoreCase(role)) {
                // Paramedic 201 only has access to cases 1, 2, 3, 4
                if (userId != 201 || (caseId != 1 && caseId != 2 && caseId != 3 && caseId != 4)) {
                    return ServiceResult.notFound("Emergency case not found with ID: " + caseId);
                }
            }
            if ("HOSPITAL_ADMIN".equalsIgnoreCase(role)) {
                return ServiceResult.notFound("Emergency case not found with ID: " + caseId);
            }
            return ServiceResult.success("Emergency case retrieved successfully.", store.get(caseId));
        }

        @Override
        public ServiceResult<EmergencyCase> createEmergencyCaseResult(int patientId, String incidentLocation, String emergencyType, String symptoms) {
            return createEmergencyCaseResult(patientId, incidentLocation, emergencyType, symptoms, null);
        }

        @Override
        public ServiceResult<EmergencyCase> createEmergencyCaseResult(int patientId, String incidentLocation, String emergencyType, String symptoms, String priorityLevel) {
            if ("DatabaseError".equals(emergencyType)) {
                fakeLastError = "DATABASE_ERROR: Connection dropped.";
                return ServiceResult.databaseError(fakeLastError);
            }
            if (patientId == 999) {
                fakeLastError = "Patient with ID 999 not found.";
                return ServiceResult.validationError(fakeLastError);
            }
            String resolvedPriority;
            if (priorityLevel != null && !priorityLevel.trim().isEmpty()) {
                String normalized = priorityLevel.trim().toUpperCase();
                if (!isValidPriority(normalized)) {
                    fakeLastError = "Invalid priority level: " + priorityLevel + ". Allowed values: CRITICAL, HIGH, MODERATE, LOW.";
                    return ServiceResult.validationError(fakeLastError);
                }
                resolvedPriority = normalized;
            } else if (priorityLevel != null && priorityLevel.trim().isEmpty()) {
                fakeLastError = "Invalid priority level: " + priorityLevel + ". Allowed values: CRITICAL, HIGH, MODERATE, LOW.";
                return ServiceResult.validationError(fakeLastError);
            } else {
                resolvedPriority = "MODERATE";
            }
            int newId = store.size() + 1;
            EmergencyCase ec = new EmergencyCase(newId, patientId, incidentLocation, emergencyType, symptoms, resolvedPriority, "REGISTERED");
            store.put(newId, ec);
            fakeLastError = null;
            return ServiceResult.success("Emergency case registered successfully.", ec);
        }

        @Override
        public EmergencyCase createEmergencyCase(int patientId, String incidentLocation, String emergencyType, String symptoms, String priorityLevel) {
            ServiceResult<EmergencyCase> res = createEmergencyCaseResult(patientId, incidentLocation, emergencyType, symptoms, priorityLevel);
            return res.isSuccess() ? res.getData() : null;
        }

        @Override
        public EmergencyCase createEmergencyCase(int patientId, String incidentLocation, String emergencyType, String symptoms) {
            return createEmergencyCase(patientId, incidentLocation, emergencyType, symptoms, null);
        }

        @Override
        public boolean registerEmergencyCase(int patientId, String incidentLocation, String emergencyType, String symptoms, String priorityLevel) {
            return createEmergencyCaseResult(patientId, incidentLocation, emergencyType, symptoms, priorityLevel).isSuccess();
        }

        @Override
        public boolean registerEmergencyCase(int patientId, String incidentLocation, String emergencyType, String symptoms) {
            return registerEmergencyCase(patientId, incidentLocation, emergencyType, symptoms, null);
        }

        @Override
        public ServiceResult<EmergencyCase> updateCaseStatusAtomic(int caseId, String status, int userId, String role) {
            if (simulateConcurrentConflict) {
                fakeLastError = "Concurrent update conflict: case status was modified by another request.";
                return ServiceResult.conflict(fakeLastError);
            }
            EmergencyCase ec = store.get(caseId);
            if (ec == null) {
                fakeLastError = "Emergency case not found with ID: " + caseId;
                return ServiceResult.notFound(fakeLastError);
            }
            if ("PARAMEDIC".equalsIgnoreCase(role)) {
                // Paramedic 201 only authorized for cases 1, 2, 3, 4
                if (userId != 201 || (caseId != 1 && caseId != 2 && caseId != 3 && caseId != 4)) {
                    fakeLastError = "Emergency case not found with ID: " + caseId;
                    return ServiceResult.notFound(fakeLastError);
                }
            }
            if ("AMBULANCE_DRIVER".equalsIgnoreCase(role) || "HOSPITAL_ADMIN".equalsIgnoreCase(role) || "EMERGENCY_DOCTOR".equalsIgnoreCase(role)) {
                fakeLastError = "Access denied. Insufficient role permissions to update case status.";
                return ServiceResult.validationError(fakeLastError);
            }
            if ("FAIL_DB".equalsIgnoreCase(status)) {
                fakeLastError = "DATABASE_ERROR: DB update failed.";
                return ServiceResult.databaseError(fakeLastError);
            }
            String currentStatus = ec.getCaseStatus();
            String target = status.toUpperCase();

            if (!isValidStatus(target)) {
                fakeLastError = "Invalid case status: " + status;
                return ServiceResult.validationError(fakeLastError);
            }

            if (!isAllowedTransition(currentStatus, target)) {
                fakeLastError = "Invalid status transition from " + currentStatus + " to " + target + ".";
                return ServiceResult.conflict(fakeLastError);
            }

            ec.setCaseStatus(target);
            fakeLastError = null;
            return ServiceResult.success("Emergency case status updated successfully.", ec);
        }

        @Override
        public ServiceResult<EmergencyCase> updateCaseStatusAtomic(int caseId, String status) {
            return updateCaseStatusAtomic(caseId, status, -1, "ADMIN");
        }

        @Override
        public ServiceResult<CaseTriageResponse> triageCaseResult(int caseId, CaseTriageRequest request, int userId, String role) {
            if (request != null && "DatabaseError".equals(request.getSymptoms())) {
                fakeLastError = "DATABASE_ERROR: Connection dropped.";
                return ServiceResult.databaseError(fakeLastError);
            }
            if (!ValidationUtil.isValidId(caseId)) {
                fakeLastError = "Invalid case ID. ID must be greater than 0.";
                return ServiceResult.validationError(fakeLastError);
            }
            if ("AMBULANCE_DRIVER".equalsIgnoreCase(role) || "HOSPITAL_ADMIN".equalsIgnoreCase(role)) {
                fakeLastError = "Access denied. Insufficient role permissions to perform AI triage.";
                return ServiceResult.validationError(fakeLastError);
            }
            EmergencyCase ec = store.get(caseId);
            if (ec == null) {
                fakeLastError = "Emergency case not found with ID: " + caseId;
                return ServiceResult.notFound(fakeLastError);
            }
            if ("PARAMEDIC".equalsIgnoreCase(role)) {
                if (userId != 201 || (caseId != 1 && caseId != 2 && caseId != 3 && caseId != 4)) {
                    fakeLastError = "Emergency case not found with ID: " + caseId;
                    return ServiceResult.notFound(fakeLastError);
                }
            }
            if (request.getPatientId() != null && !request.getPatientId().equals(ec.getPatientId())) {
                fakeLastError = "Supplied patient ID " + request.getPatientId()
                        + " does not match emergency case patient ID " + ec.getPatientId() + ".";
                return ServiceResult.validationError(fakeLastError);
            }
            String currentStatus = ec.getCaseStatus();
            if ("COMPLETED".equalsIgnoreCase(currentStatus) || "CANCELLED".equalsIgnoreCase(currentStatus)) {
                fakeLastError = "Cannot perform AI triage on " + currentStatus + " case.";
                return ServiceResult.conflict(fakeLastError);
            }

            PatientVital vital = new PatientVital();
            vital.setCaseId(caseId);
            vital.setHeartRate(request.getHeartRate());
            vital.setSystolicBp(request.getSystolicBp());
            vital.setDiastolicBp(request.getDiastolicBp());
            vital.setSpo2(request.getSpo2());
            vital.setRespiratoryRate(request.getRespiratoryRate());
            vital.setTemperature(request.getTemperature() != null ? request.getTemperature() : 37.0);
            vital.setGlucose(request.getGlucose() != null ? request.getGlucose() : 0.0);
            String symptoms = (request.getSymptoms() != null && !request.getSymptoms().trim().isEmpty())
                    ? request.getSymptoms().trim()
                    : ec.getSymptoms();
            vital.setSymptoms(symptoms);

            TriageService triageService = new TriageService();
            String calculatedPriority = triageService.determinePriority(vital);
            String suggestedSpecialty = triageService.determineSpecialty(symptoms);
            String priorityCode = switch (calculatedPriority) {
                case "CRITICAL" -> "P1";
                case "HIGH" -> "P2";
                case "MODERATE" -> "P3";
                default -> "P4";
            };
            String recommendation = switch (calculatedPriority) {
                case "CRITICAL" -> "Immediate emergency care required. Route patient to " + suggestedSpecialty + " facility without delay.";
                case "HIGH" -> "Urgent medical attention required. Prefer " + suggestedSpecialty + " emergency services.";
                case "MODERATE" -> "Medical evaluation required. Monitor patient during transport.";
                default -> "Stable condition. Continue routine monitoring during transport.";
            };

            String previousPriority = ec.getPriorityLevel();
            boolean isEscalation = isPriorityEscalation(previousPriority, calculatedPriority);
            String activePriority = isEscalation ? calculatedPriority : previousPriority;

            if (isEscalation) {
                ec.setPriorityLevel(calculatedPriority);
            }

            CaseTriageResponse response = new CaseTriageResponse(
                    caseId,
                    ec.getPatientId(),
                    previousPriority,
                    calculatedPriority,
                    activePriority,
                    priorityCode,
                    suggestedSpecialty,
                    recommendation,
                    isEscalation,
                    "AI-assisted triage decision support. Not a confirmed medical diagnosis."
            );
            return ServiceResult.success("AI triage evaluation completed successfully.", response);
        }

        @Override
        public boolean updateCaseStatus(int caseId, String status) {
            return updateCaseStatusAtomic(caseId, status).isSuccess();
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
        public EmergencyCaseService emergencyCaseService() {
            return new FakeEmergencyCaseService();
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
    private EmergencyCaseService emergencyCaseService;

    @BeforeEach
    void setUp() {
        if (emergencyCaseService instanceof FakeEmergencyCaseService fake) {
            fake.reset();
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/cases (List)
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "driver01", roles = {"AMBULANCE_DRIVER"})
    void testGetAllCases_Driver_Success() throws Exception {
        mockMvc.perform(get("/api/cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Emergency cases retrieved successfully."))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].caseId").value(1))
                .andExpect(jsonPath("$.data[0].emergencyType").value("CARDIAC"))
                .andExpect(jsonPath("$.data[0].caseStatus").value("REGISTERED"));
    }

    @Test
    @WithMockUser(username = "hospital01", roles = {"HOSPITAL_ADMIN"})
    void testGetAllCases_HospitalAdmin_Forbidden_Returns403() throws Exception {
        mockMvc.perform(get("/api/cases"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "doctor01", roles = {"EMERGENCY_DOCTOR"})
    void testGetAllCases_Doctor_Success() throws Exception {
        mockMvc.perform(get("/api/cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", hasSize(6)));
    }

    @Test
    void testGetAllCases_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/cases"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(containsString("Authentication required")));
    }

    // -------------------------------------------------------------------------
    // GET /api/cases/{id} (Detail)
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "paramedic01", roles = {"PARAMEDIC"})
    void testGetCaseById_Success() throws Exception {
        mockMvc.perform(get("/api/cases/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.caseId").value(1))
                .andExpect(jsonPath("$.data.patientId").value(1))
                .andExpect(jsonPath("$.data.incidentLocation").value("MG Road, Bengaluru"))
                .andExpect(jsonPath("$.data.emergencyType").value("CARDIAC"))
                .andExpect(jsonPath("$.data.priorityLevel").value("CRITICAL"))
                .andExpect(jsonPath("$.data.caseStatus").value("REGISTERED"));
    }

    @Test
    @WithMockUser(username = "driver01", roles = {"AMBULANCE_DRIVER"})
    void testGetCaseById_Driver_AssignedCase_Success() throws Exception {
        mockMvc.perform(get("/api/cases/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.caseId").value(1))
                .andExpect(jsonPath("$.data.caseStatus").value("REGISTERED"));
    }

    @Test
    @WithMockUser(username = "driver01", roles = {"AMBULANCE_DRIVER"})
    void testGetCaseById_Driver_UnassignedCase_Returns404() throws Exception {
        // Case 2 is not assigned to driver01; must return 404 to prevent enumeration
        mockMvc.perform(get("/api/cases/2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(username = "hospital01", roles = {"HOSPITAL_ADMIN"})
    void testGetCaseById_HospitalAdmin_Forbidden_Returns403() throws Exception {
        mockMvc.perform(get("/api/cases/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "paramedic01", roles = {"PARAMEDIC"})
    void testGetCaseById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/cases/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("Emergency case not found with ID: 999")));
    }

    @Test
    @WithMockUser(username = "paramedic01", roles = {"PARAMEDIC"})
    void testGetCaseById_InvalidId_Returns400() throws Exception {
        mockMvc.perform(get("/api/cases/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("Invalid case ID")));
    }

    // -------------------------------------------------------------------------
    // POST /api/cases (Create) & Actual Generated ID / Error Classification
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testCreateCase_Dispatcher_ReturnsRealGeneratedId() throws Exception {
        EmergencyCaseRequest request = new EmergencyCaseRequest(
                1, "Whitefield, Bengaluru", "RESPIRATORY", "Severe difficulty breathing"
        );

        mockMvc.perform(post("/api/cases")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Emergency case registered successfully."))
                .andExpect(jsonPath("$.data.caseId").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.patientId").value(1))
                .andExpect(jsonPath("$.data.emergencyType").value("RESPIRATORY"))
                .andExpect(jsonPath("$.data.priorityLevel").value("MODERATE"))
                .andExpect(jsonPath("$.data.caseStatus").value("REGISTERED"));
    }

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testCreateCase_WithExplicitValidPriorities_Success() throws Exception {
        for (String priority : List.of("CRITICAL", "HIGH", "MODERATE", "LOW")) {
            EmergencyCaseRequest request = new EmergencyCaseRequest(
                    1, "MG Road, Bengaluru", "CARDIAC", "Chest pain", priority
            );

            mockMvc.perform(post("/api/cases")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.data.priorityLevel").value(priority));
        }
    }

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testCreateCase_WithWhitespaceAndCaseNormalization_Success() throws Exception {
        EmergencyCaseRequest request = new EmergencyCaseRequest(
                1, "MG Road, Bengaluru", "CARDIAC", "Chest pain", "  critical  "
        );

        mockMvc.perform(post("/api/cases")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.priorityLevel").value("CRITICAL"));
    }

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testCreateCase_WithInvalidPriority_Returns400BadRequest() throws Exception {
        for (String invalidPriority : List.of("EXTREME", "URGENT", "INVALID", "", "   ")) {
            EmergencyCaseRequest request = new EmergencyCaseRequest(
                    1, "MG Road, Bengaluru", "CARDIAC", "Chest pain", invalidPriority
            );

            mockMvc.perform(post("/api/cases")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    @Test
    @WithMockUser(username = "paramedic01", roles = {"PARAMEDIC"})
    void testCreateCase_Paramedic_ReturnsRealGeneratedId() throws Exception {
        EmergencyCaseRequest request = new EmergencyCaseRequest(
                2, "Koramangala, Bengaluru", "TRAUMA", "Multiple fractures"
        );

        mockMvc.perform(post("/api/cases")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.caseId").value(greaterThan(0)));
    }

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testCreateCase_DatabaseFailure_Returns500InternalServerError() throws Exception {
        // 'DatabaseError' emergencyType triggers simulated DB error
        EmergencyCaseRequest request = new EmergencyCaseRequest(
                1, "Whitefield, Bengaluru", "DatabaseError", "Simulated DB failure"
        );

        mockMvc.perform(post("/api/cases")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value(containsString("An error occurred while saving the emergency case")));
    }

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testCreateCase_PatientNotFound_Returns400() throws Exception {
        EmergencyCaseRequest request = new EmergencyCaseRequest(
                999, "Unknown Location", "RESPIRATORY", "Severe difficulty breathing"
        );

        mockMvc.perform(post("/api/cases")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("Patient with ID 999 not found")));
    }

    @Test
    @WithMockUser(username = "driver01", roles = {"AMBULANCE_DRIVER"})
    void testCreateCase_ForbiddenRole_Driver_Returns403() throws Exception {
        EmergencyCaseRequest request = new EmergencyCaseRequest(
                1, "Whitefield, Bengaluru", "RESPIRATORY", "Severe difficulty breathing"
        );

        mockMvc.perform(post("/api/cases")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(containsString("Access denied")));
    }

    @Test
    @WithMockUser(username = "doctor01", roles = {"EMERGENCY_DOCTOR"})
    void testCreateCase_ForbiddenRole_Doctor_Returns403() throws Exception {
        EmergencyCaseRequest request = new EmergencyCaseRequest(
                1, "Whitefield, Bengaluru", "RESPIRATORY", "Severe difficulty breathing"
        );

        mockMvc.perform(post("/api/cases")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "hospital01", roles = {"HOSPITAL_ADMIN"})
    void testCreateCase_ForbiddenRole_HospitalAdmin_Returns403() throws Exception {
        EmergencyCaseRequest request = new EmergencyCaseRequest(
                1, "Whitefield, Bengaluru", "RESPIRATORY", "Severe difficulty breathing"
        );

        mockMvc.perform(post("/api/cases")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testCreateCase_ValidationError_MissingPatientId_Returns400() throws Exception {
        EmergencyCaseRequest request = new EmergencyCaseRequest(
                null, "Whitefield, Bengaluru", "RESPIRATORY", "Severe difficulty breathing"
        );

        mockMvc.perform(post("/api/cases")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data.patientId").exists());
    }

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testCreateCase_ValidationError_BlankLocation_Returns400() throws Exception {
        EmergencyCaseRequest request = new EmergencyCaseRequest(
                1, "", "RESPIRATORY", "Severe difficulty breathing"
        );

        mockMvc.perform(post("/api/cases")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data.incidentLocation").exists());
    }

    // -------------------------------------------------------------------------
    // PUT /api/cases/{id}/status — Valid & Invalid State Transitions
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testUpdateCaseStatus_DirectRegisteredToDispatched_RejectedWith409Conflict() throws Exception {
        CaseStatusUpdateRequest request = new CaseStatusUpdateRequest("DISPATCHED");

        // Case 1 is initially REGISTERED; direct transition to DISPATCHED is forbidden.
        // Cases must be dispatched via the formal dispatch endpoint POST /api/dispatches.
        mockMvc.perform(put("/api/cases/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("Invalid status transition from REGISTERED to DISPATCHED")));
    }

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testUpdateCaseStatus_ValidTransition_RegisteredToCancelled_Success() throws Exception {
        CaseStatusUpdateRequest request = new CaseStatusUpdateRequest("CANCELLED");

        // Case 1 is initially REGISTERED
        mockMvc.perform(put("/api/cases/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.caseStatus").value("CANCELLED"));
    }

    @Test
    @WithMockUser(username = "paramedic01", roles = {"PARAMEDIC"})
    void testUpdateCaseStatus_ValidTransition_DispatchedToInTransit_Success() throws Exception {
        CaseStatusUpdateRequest request = new CaseStatusUpdateRequest("IN_TRANSIT");

        // Case 2 is initially DISPATCHED
        mockMvc.perform(put("/api/cases/2/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.caseStatus").value("IN_TRANSIT"));
    }

    @Test
    @WithMockUser(username = "paramedic01", roles = {"PARAMEDIC"})
    void testUpdateCaseStatus_ValidTransition_InTransitToArrived_Success() throws Exception {
        CaseStatusUpdateRequest request = new CaseStatusUpdateRequest("ARRIVED");

        // Case 3 is initially IN_TRANSIT
        mockMvc.perform(put("/api/cases/3/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.caseStatus").value("ARRIVED"));
    }

    @Test
    @WithMockUser(username = "paramedic01", roles = {"PARAMEDIC"})
    void testUpdateCaseStatus_ValidTransition_ArrivedToCompleted_Success() throws Exception {
        CaseStatusUpdateRequest request = new CaseStatusUpdateRequest("COMPLETED");

        // Case 4 is initially ARRIVED
        mockMvc.perform(put("/api/cases/4/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.caseStatus").value("COMPLETED"));
    }

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testUpdateCaseStatus_InvalidTransition_CompletedToRegistered_RejectedWith409Conflict() throws Exception {
        CaseStatusUpdateRequest request = new CaseStatusUpdateRequest("REGISTERED");

        // Case 5 is COMPLETED (terminal state)
        mockMvc.perform(put("/api/cases/5/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("Invalid status transition from COMPLETED to REGISTERED")));
    }

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testUpdateCaseStatus_InvalidTransition_CancelledToDispatched_RejectedWith409Conflict() throws Exception {
        CaseStatusUpdateRequest request = new CaseStatusUpdateRequest("DISPATCHED");

        // Case 6 is CANCELLED (terminal state)
        mockMvc.perform(put("/api/cases/6/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("Invalid status transition from CANCELLED to DISPATCHED")));
    }

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testUpdateCaseStatus_InvalidTransition_RegisteredToCompletedDirectJump_RejectedWith409Conflict() throws Exception {
        CaseStatusUpdateRequest request = new CaseStatusUpdateRequest("COMPLETED");

        // Case 1 is REGISTERED; direct jump to COMPLETED is forbidden
        mockMvc.perform(put("/api/cases/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("Invalid status transition from REGISTERED to COMPLETED")));
    }

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testUpdateCaseStatus_InvalidTransition_SameState_DispatchedToDispatched_RejectedWith409Conflict() throws Exception {
        CaseStatusUpdateRequest request = new CaseStatusUpdateRequest("DISPATCHED");

        // Case 2 is already DISPATCHED
        mockMvc.perform(put("/api/cases/2/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("Invalid status transition from DISPATCHED to DISPATCHED")));
    }

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testUpdateCaseStatus_ConcurrentConflict_Returns409Conflict() throws Exception {
        if (emergencyCaseService instanceof FakeEmergencyCaseService fake) {
            fake.setSimulateConcurrentConflict(true);
        }
        CaseStatusUpdateRequest request = new CaseStatusUpdateRequest("CANCELLED");

        mockMvc.perform(put("/api/cases/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("Concurrent update conflict")));
    }

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testUpdateCaseStatus_InvalidStatusValue_RejectedWith400() throws Exception {
        CaseStatusUpdateRequest request = new CaseStatusUpdateRequest("NOT_A_VALID_STATUS");

        mockMvc.perform(put("/api/cases/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed."))
                .andExpect(jsonPath("$.data.status").value(containsString("Invalid case status")));
    }

    @Test
    @WithMockUser(username = "driver01", roles = {"AMBULANCE_DRIVER"})
    void testUpdateCaseStatus_ForbiddenRole_Driver_Returns403() throws Exception {
        CaseStatusUpdateRequest request = new CaseStatusUpdateRequest("IN_TRANSIT");

        mockMvc.perform(put("/api/cases/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(containsString("Access denied")));
    }

    @Test
    @WithMockUser(username = "hospital01", roles = {"HOSPITAL_ADMIN"})
    void testUpdateCaseStatus_ForbiddenRole_HospitalAdmin_Returns403() throws Exception {
        CaseStatusUpdateRequest request = new CaseStatusUpdateRequest("COMPLETED");

        mockMvc.perform(put("/api/cases/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testUpdateCaseStatus_NotFound_Returns404() throws Exception {
        CaseStatusUpdateRequest request = new CaseStatusUpdateRequest("DISPATCHED");

        mockMvc.perform(put("/api/cases/999/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("Emergency case not found with ID: 999")));
    }

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testUpdateCaseStatus_InvalidId_Returns400() throws Exception {
        CaseStatusUpdateRequest request = new CaseStatusUpdateRequest("DISPATCHED");

        mockMvc.perform(put("/api/cases/0/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("Invalid case ID")));
    }

    @Test
    @WithMockUser(username = "paramedic01", roles = {"PARAMEDIC"})
    void testGetAllCases_Paramedic_ReturnsOnlyAssignedCases() throws Exception {
        mockMvc.perform(get("/api/cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.data[0].caseId").value(1))
                .andExpect(jsonPath("$.data[1].caseId").value(2))
                .andExpect(jsonPath("$.data[2].caseId").value(3))
                .andExpect(jsonPath("$.data[3].caseId").value(4));
    }

    @Test
    @WithMockUser(username = "paramedic01", roles = {"PARAMEDIC"})
    void testGetCaseById_Paramedic_UnrelatedCase_Returns404NotFound() throws Exception {
        // Case 6 is not in paramedic01's assigned encounter scope
        mockMvc.perform(get("/api/cases/6"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("Emergency case not found with ID: 6")));
    }

    @Test
    @WithMockUser(username = "paramedic01", roles = {"PARAMEDIC"})
    void testUpdateCaseStatus_Paramedic_UnrelatedCase_Returns404NotFound() throws Exception {
        // Case 6 is not assigned to paramedic01; attempt to transition status must return 404
        CaseStatusUpdateRequest request = new CaseStatusUpdateRequest("DISPATCHED");

        mockMvc.perform(put("/api/cases/6/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("Emergency case not found with ID: 6")));
    }

    @Test
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    void testGetAllCases_DatabaseError_Returns500() throws Exception {
        ((FakeEmergencyCaseService) emergencyCaseService).fakeLastError = "DB_LIST_FAIL";
        mockMvc.perform(get("/api/cases"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value(containsString("An error occurred while retrieving emergency cases")));
    }

    @Test
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    void testGetCaseById_DatabaseError_Returns500() throws Exception {
        mockMvc.perform(get("/api/cases/888"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value(containsString("An error occurred while retrieving the emergency case")));
    }

    @Test
    @WithMockUser(username = "unresolvableDriver", roles = {"AMBULANCE_DRIVER"})
    void testGetAllCases_UnresolvableUser_FailsClosed() throws Exception {
        mockMvc.perform(get("/api/cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void testGetAllCases_DomainUserPrincipal_Success() throws Exception {
        User driverUser = new User();
        driverUser.setUserId(101);
        driverUser.setUsername("driverDirect");
        driverUser.setRole("AMBULANCE_DRIVER");

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                driverUser, null, List.of(new SimpleGrantedAuthority("ROLE_AMBULANCE_DRIVER"))
        );

        mockMvc.perform(get("/api/cases").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].caseId").value(1));
    }

    // -------------------------------------------------------------------------
    // POST /api/cases/{id}/triage (AI Triage Endpoint & Authorization Tests)
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testTriageCase_PriorityEscalation_FromModerateToCritical_Success() throws Exception {
        // Case 4 in fake store starts with MODERATE priority
        CaseTriageRequest request = new CaseTriageRequest(
                2, 150, 75, 45, 85.0, 39.5, 35, 110.0, "Severe cardiac arrest"
        );

        mockMvc.perform(post("/api/cases/4/triage")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("AI triage evaluation completed successfully."))
                .andExpect(jsonPath("$.data.caseId").value(4))
                .andExpect(jsonPath("$.data.patientId").value(2))
                .andExpect(jsonPath("$.data.previousPriority").value("MODERATE"))
                .andExpect(jsonPath("$.data.calculatedPriority").value("CRITICAL"))
                .andExpect(jsonPath("$.data.activePriority").value("CRITICAL"))
                .andExpect(jsonPath("$.data.priorityEscalated").value(true))
                .andExpect(jsonPath("$.data.priorityCode").value("P1"))
                .andExpect(jsonPath("$.data.suggestedSpecialty").value("CARDIOLOGY"))
                .andExpect(jsonPath("$.data.disclaimer").value("AI-assisted triage decision support. Not a confirmed medical diagnosis."));
    }

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testTriageCase_NoAutomaticDowngrade_FromCriticalToLow_PreservesCritical() throws Exception {
        // Case 1 in fake store starts with CRITICAL priority
        CaseTriageRequest request = new CaseTriageRequest(
                1, 72, 120, 80, 98.0, 36.8, 16, 95.0, "Mild symptoms"
        );

        mockMvc.perform(post("/api/cases/1/triage")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.caseId").value(1))
                .andExpect(jsonPath("$.data.previousPriority").value("CRITICAL"))
                .andExpect(jsonPath("$.data.calculatedPriority").value("LOW"))
                .andExpect(jsonPath("$.data.activePriority").value("CRITICAL"))
                .andExpect(jsonPath("$.data.priorityEscalated").value(false));
    }

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testTriageCase_InconsistentBloodPressure_Returns400() throws Exception {
        // Systolic <= Diastolic is physiologically inconsistent
        CaseTriageRequest request = new CaseTriageRequest(
                1, 80, 80, 90, 95.0, 37.0, 18, 90.0, "Headache"
        );

        mockMvc.perform(post("/api/cases/1/triage")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testTriageCase_InvalidVitalSigns_Returns400() throws Exception {
        // SpO2 > 100 is invalid
        CaseTriageRequest request = new CaseTriageRequest(
                1, 80, 120, 80, 150.0, 37.0, 18, 90.0, "Headache"
        );

        mockMvc.perform(post("/api/cases/1/triage")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testTriageCase_PatientCaseMismatch_Returns400() throws Exception {
        // Case 1 has patientId = 1, but request supplies patientId = 99
        CaseTriageRequest request = new CaseTriageRequest(
                99, 80, 120, 80, 95.0, 37.0, 18, 90.0, "Headache"
        );

        mockMvc.perform(post("/api/cases/1/triage")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("does not match emergency case patient ID")));
    }

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testTriageCase_MissingCase_Returns404NotFound() throws Exception {
        CaseTriageRequest request = new CaseTriageRequest(
                1, 80, 120, 80, 95.0, 37.0, 18, 90.0, "Headache"
        );

        mockMvc.perform(post("/api/cases/999/triage")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(username = "paramedic01", roles = {"PARAMEDIC"})
    void testTriageCase_AssignedParamedic_Success() throws Exception {
        // Paramedic 201 is assigned to Case 1
        CaseTriageRequest request = new CaseTriageRequest(
                1, 80, 120, 80, 95.0, 37.0, 18, 90.0, "Chest pain"
        );

        mockMvc.perform(post("/api/cases/1/triage")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @WithMockUser(username = "paramedic01", roles = {"PARAMEDIC"})
    void testTriageCase_UnassignedParamedic_Returns404NotFound() throws Exception {
        // Paramedic 201 is NOT assigned to Case 5
        CaseTriageRequest request = new CaseTriageRequest(
                1, 80, 120, 80, 95.0, 37.0, 18, 90.0, "Recovery"
        );

        mockMvc.perform(post("/api/cases/5/triage")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(username = "driver01", roles = {"AMBULANCE_DRIVER"})
    void testTriageCase_ForbiddenRole_Driver_Returns403() throws Exception {
        CaseTriageRequest request = new CaseTriageRequest(
                1, 80, 120, 80, 95.0, 37.0, 18, 90.0, "Chest pain"
        );

        mockMvc.perform(post("/api/cases/1/triage")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "hospital01", roles = {"HOSPITAL_ADMIN"})
    void testTriageCase_ForbiddenRole_HospitalAdmin_Returns403() throws Exception {
        CaseTriageRequest request = new CaseTriageRequest(
                1, 80, 120, 80, 95.0, 37.0, 18, 90.0, "Chest pain"
        );

        mockMvc.perform(post("/api/cases/1/triage")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testTriageCase_TerminalCase_Completed_Returns409Conflict() throws Exception {
        // Case 5 is COMPLETED in fake store
        CaseTriageRequest request = new CaseTriageRequest(
                1, 80, 120, 80, 95.0, 37.0, 18, 90.0, "Cardiac check"
        );

        mockMvc.perform(post("/api/cases/5/triage")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("Cannot perform AI triage on COMPLETED case")));
    }

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testTriageCase_TerminalCase_Cancelled_Returns409Conflict() throws Exception {
        // Case 6 is CANCELLED in fake store
        CaseTriageRequest request = new CaseTriageRequest(
                2, 80, 120, 80, 95.0, 37.0, 18, 90.0, "False alarm"
        );

        mockMvc.perform(post("/api/cases/6/triage")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("Cannot perform AI triage on CANCELLED case")));
    }

    @Test
    @WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})
    void testTriageCase_DatabaseError_Returns500() throws Exception {
        // Symptoms = 'DatabaseError' triggers simulated DB error
        CaseTriageRequest request = new CaseTriageRequest(
                1, 80, 120, 80, 95.0, 37.0, 18, 90.0, "DatabaseError"
        );

        mockMvc.perform(post("/api/cases/1/triage")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value(containsString("An error occurred during AI triage evaluation")));
    }
}
