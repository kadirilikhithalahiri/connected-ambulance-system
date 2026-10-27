package com.abc.emergency;

import com.abc.emergency.dao.EmergencyCaseDAO;
import com.abc.emergency.dao.PatientDAO;
import com.abc.emergency.dto.CaseTriageRequest;
import com.abc.emergency.dto.CaseTriageResponse;
import com.abc.emergency.model.EmergencyCase;
import com.abc.emergency.model.Patient;
import com.abc.emergency.service.EmergencyCaseService;
import com.abc.emergency.service.ServiceResult;
import com.abc.emergency.service.TriageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 5J.10 Unit and Regression Tests for AI-Assisted Triage REST Integration.
 */
public class EmergencyCaseTriageUnitTest {

    static class StubCaseDAO extends EmergencyCaseDAO {
        final Map<Integer, EmergencyCase> store = new HashMap<>();
        String lastAtomicExpected;
        String lastAtomicTarget;
        int atomicUpdateCallCount = 0;
        boolean simulateDbError = false;

        @Override
        public EmergencyCase getEmergencyCaseById(int caseId) {
            clearDatabaseError();
            return store.get(caseId);
        }

        @Override
        public boolean updateCasePriorityAtomic(int caseId, String expectedPriority, String targetPriority) {
            clearDatabaseError();
            if (simulateDbError) {
                return false;
            }
            this.lastAtomicExpected = expectedPriority;
            this.lastAtomicTarget = targetPriority;
            this.atomicUpdateCallCount++;

            EmergencyCase ec = store.get(caseId);
            if (ec != null && ec.getPriorityLevel().equalsIgnoreCase(expectedPriority)) {
                ec.setPriorityLevel(targetPriority);
                return true;
            }
            return false;
        }

        @Override
        public boolean isCaseAssignedToUser(int caseId, int userId, String role) {
            clearDatabaseError();
            // User 201 is assigned to Case 1 and Case 2; unassigned to Case 3
            if ("PARAMEDIC".equalsIgnoreCase(role) && userId == 201) {
                return caseId == 1 || caseId == 2;
            }
            return false;
        }
    }

    static class StubPatientDAO extends PatientDAO {
        @Override
        public Patient getPatientById(int id) {
            clearDatabaseError();
            if (id > 0) {
                return new Patient(id, "Test Patient", 35, "Male", "A+", "None", "None", "1234567890");
            }
            return null;
        }
    }

    private EmergencyCaseService createService(StubCaseDAO caseDAO) {
        return new EmergencyCaseService(caseDAO, new StubPatientDAO(), new TriageService());
    }

    // -------------------------------------------------------------------------
    // 1. Priority Classification & Escalation Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Priority escalation from MODERATE to CRITICAL updates database and returns escalation flag")
    void testTriageCase_PriorityEscalation_FromModerateToCritical() {
        StubCaseDAO caseDAO = new StubCaseDAO();
        caseDAO.store.put(1, new EmergencyCase(1, 1, "MG Road", "CARDIAC", "Chest discomfort", "MODERATE", "REGISTERED"));
        EmergencyCaseService service = createService(caseDAO);

        // Vitals indicating CRITICAL condition (SpO2 85%, HR 150)
        CaseTriageRequest request = new CaseTriageRequest(
                1, 150, 75, 45, 85.0, 39.5, 35, 110.0, "Severe chest pain radiating to jaw"
        );

        ServiceResult<CaseTriageResponse> result = service.triageCaseResult(1, request, 102, "DISPATCHER");

        assertTrue(result.isSuccess());
        CaseTriageResponse data = result.getData();
        assertNotNull(data);
        assertEquals(1, data.getCaseId());
        assertEquals(1, data.getPatientId());
        assertEquals("MODERATE", data.getPreviousPriority());
        assertEquals("CRITICAL", data.getCalculatedPriority());
        assertEquals("CRITICAL", data.getActivePriority());
        assertEquals("P1", data.getPriorityCode());
        assertEquals("CARDIOLOGY", data.getSuggestedSpecialty());
        assertTrue(data.isPriorityEscalated());
        assertEquals("AI-assisted triage decision support. Not a confirmed medical diagnosis.", data.getDisclaimer());

        // Verify DAO was called and database priority updated
        assertEquals(1, caseDAO.atomicUpdateCallCount);
        assertEquals("MODERATE", caseDAO.lastAtomicExpected);
        assertEquals("CRITICAL", caseDAO.lastAtomicTarget);
        assertEquals("CRITICAL", caseDAO.store.get(1).getPriorityLevel());
    }

    @Test
    @DisplayName("Priority escalation from LOW to HIGH updates database")
    void testTriageCase_PriorityEscalation_FromLowToHigh() {
        StubCaseDAO caseDAO = new StubCaseDAO();
        caseDAO.store.put(2, new EmergencyCase(2, 2, "Indiranagar", "RESPIRATORY", "Cough", "LOW", "DISPATCHED"));
        EmergencyCaseService service = createService(caseDAO);

        // Vitals indicating HIGH condition (SpO2 92%, HR 125, RR 26)
        CaseTriageRequest request = new CaseTriageRequest(
                2, 125, 88, 55, 92.0, 38.5, 26, 100.0, "Audible wheezing and asthma"
        );

        ServiceResult<CaseTriageResponse> result = service.triageCaseResult(2, request, 101, "ADMIN");

        assertTrue(result.isSuccess());
        CaseTriageResponse data = result.getData();
        assertEquals("LOW", data.getPreviousPriority());
        assertEquals("HIGH", data.getCalculatedPriority());
        assertEquals("HIGH", data.getActivePriority());
        assertTrue(data.isPriorityEscalated());
        assertEquals("P2", data.getPriorityCode());
        assertEquals("PULMONOLOGY", data.getSuggestedSpecialty());
        assertEquals(1, caseDAO.atomicUpdateCallCount);
        assertEquals("HIGH", caseDAO.store.get(2).getPriorityLevel());
    }

    // -------------------------------------------------------------------------
    // 2. No Automatic Priority Downgrade Tests
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @CsvSource({
            "CRITICAL, LOW",
            "CRITICAL, MODERATE",
            "CRITICAL, HIGH",
            "HIGH, LOW",
            "HIGH, MODERATE",
            "MODERATE, LOW"
    })
    @DisplayName("Lower calculated triage acuity never downgrades higher existing priority")
    void testTriageCase_NoAutomaticDowngrade(String currentPriority, String lowerPriority) {
        StubCaseDAO caseDAO = new StubCaseDAO();
        caseDAO.store.put(1, new EmergencyCase(1, 1, "Whitefield", "TRAUMA", "Laceration", currentPriority, "REGISTERED"));
        EmergencyCaseService service = createService(caseDAO);

        // Supply vitals corresponding to normal/low readings (SpO2 99%, HR 70, BP 120/80, RR 15)
        CaseTriageRequest request = new CaseTriageRequest(
                1, 70, 120, 80, 99.0, 36.8, 15, 90.0, "Stable wound"
        );

        ServiceResult<CaseTriageResponse> result = service.triageCaseResult(1, request, 104, "EMERGENCY_DOCTOR");

        assertTrue(result.isSuccess());
        CaseTriageResponse data = result.getData();
        assertEquals(currentPriority, data.getPreviousPriority());
        assertEquals("LOW", data.getCalculatedPriority());
        assertEquals(currentPriority, data.getActivePriority(), "Active priority must remain unchanged at highest acuity");
        assertFalse(data.isPriorityEscalated());

        // DAO update must NOT be called for downgrades
        assertEquals(0, caseDAO.atomicUpdateCallCount);
        assertEquals(currentPriority, caseDAO.store.get(1).getPriorityLevel());
    }

    // -------------------------------------------------------------------------
    // 3. Physiological Vitals & Cross-Field Validation Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Inconsistent blood pressure (systolic <= diastolic) is rejected with validation error")
    void testTriageCase_InconsistentBloodPressure_Rejected() {
        StubCaseDAO caseDAO = new StubCaseDAO();
        caseDAO.store.put(1, new EmergencyCase(1, 1, "MG Road", "CARDIAC", "Chest pain", "MODERATE", "REGISTERED"));
        EmergencyCaseService service = createService(caseDAO);

        // Systolic 80 <= Diastolic 90 (inverted BP)
        CaseTriageRequest request = new CaseTriageRequest(
                1, 80, 80, 90, 95.0, 37.0, 18, 90.0, "Pain"
        );

        ServiceResult<CaseTriageResponse> result = service.triageCaseResult(1, request, 102, "DISPATCHER");

        assertFalse(result.isSuccess());
        assertEquals(ServiceResult.Status.VALIDATION_ERROR, result.getStatus());
        assertTrue(result.getMessage().contains("Inconsistent blood pressure"));
        assertEquals(0, caseDAO.atomicUpdateCallCount);
    }

    @Test
    @DisplayName("Identical systolic and diastolic blood pressure is rejected with validation error")
    void testTriageCase_IdenticalBloodPressure_Rejected() {
        StubCaseDAO caseDAO = new StubCaseDAO();
        caseDAO.store.put(1, new EmergencyCase(1, 1, "MG Road", "CARDIAC", "Chest pain", "MODERATE", "REGISTERED"));
        EmergencyCaseService service = createService(caseDAO);

        CaseTriageRequest request = new CaseTriageRequest(
                1, 80, 100, 100, 95.0, 37.0, 18, 90.0, "Pain"
        );

        ServiceResult<CaseTriageResponse> result = service.triageCaseResult(1, request, 102, "DISPATCHER");

        assertFalse(result.isSuccess());
        assertEquals(ServiceResult.Status.VALIDATION_ERROR, result.getStatus());
        assertTrue(result.getMessage().contains("Inconsistent blood pressure"));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-5.0, 105.0})
    @DisplayName("Out-of-range SpO2 is rejected")
    void testTriageCase_InvalidSpo2_Rejected(double invalidSpo2) {
        StubCaseDAO caseDAO = new StubCaseDAO();
        caseDAO.store.put(1, new EmergencyCase(1, 1, "MG Road", "CARDIAC", "Chest pain", "MODERATE", "REGISTERED"));
        EmergencyCaseService service = createService(caseDAO);

        CaseTriageRequest request = new CaseTriageRequest(
                1, 80, 120, 80, invalidSpo2, 37.0, 18, 90.0, "Pain"
        );

        ServiceResult<CaseTriageResponse> result = service.triageCaseResult(1, request, 102, "DISPATCHER");

        assertFalse(result.isSuccess());
        assertEquals(ServiceResult.Status.VALIDATION_ERROR, result.getStatus());
        assertTrue(result.getMessage().contains("Invalid SpO2"));
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 300})
    @DisplayName("Out-of-range heart rate is rejected")
    void testTriageCase_InvalidHeartRate_Rejected(int invalidHeartRate) {
        StubCaseDAO caseDAO = new StubCaseDAO();
        caseDAO.store.put(1, new EmergencyCase(1, 1, "MG Road", "CARDIAC", "Chest pain", "MODERATE", "REGISTERED"));
        EmergencyCaseService service = createService(caseDAO);

        CaseTriageRequest request = new CaseTriageRequest(
                1, invalidHeartRate, 120, 80, 95.0, 37.0, 18, 90.0, "Pain"
        );

        ServiceResult<CaseTriageResponse> result = service.triageCaseResult(1, request, 102, "DISPATCHER");

        assertFalse(result.isSuccess());
        assertEquals(ServiceResult.Status.VALIDATION_ERROR, result.getStatus());
        assertTrue(result.getMessage().contains("Invalid heart rate"));
    }

    // -------------------------------------------------------------------------
    // 4. Patient-Case Association & Terminal State Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Patient-case mismatch is rejected with validation error")
    void testTriageCase_PatientMismatch_Rejected() {
        StubCaseDAO caseDAO = new StubCaseDAO();
        // Case 1 belongs to Patient 1
        caseDAO.store.put(1, new EmergencyCase(1, 1, "MG Road", "CARDIAC", "Chest pain", "MODERATE", "REGISTERED"));
        EmergencyCaseService service = createService(caseDAO);

        // Request specifies Patient 99
        CaseTriageRequest request = new CaseTriageRequest(
                99, 80, 120, 80, 95.0, 37.0, 18, 90.0, "Chest pain"
        );

        ServiceResult<CaseTriageResponse> result = service.triageCaseResult(1, request, 102, "DISPATCHER");

        assertFalse(result.isSuccess());
        assertEquals(ServiceResult.Status.VALIDATION_ERROR, result.getStatus());
        assertTrue(result.getMessage().contains("does not match emergency case patient ID"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"COMPLETED", "CANCELLED"})
    @DisplayName("Terminal case status (COMPLETED or CANCELLED) rejects AI triage with conflict error")
    void testTriageCase_TerminalCase_RejectedWithConflict(String terminalStatus) {
        StubCaseDAO caseDAO = new StubCaseDAO();
        caseDAO.store.put(1, new EmergencyCase(1, 1, "MG Road", "CARDIAC", "Chest pain", "MODERATE", terminalStatus));
        EmergencyCaseService service = createService(caseDAO);

        CaseTriageRequest request = new CaseTriageRequest(
                1, 80, 120, 80, 95.0, 37.0, 18, 90.0, "Chest pain"
        );

        ServiceResult<CaseTriageResponse> result = service.triageCaseResult(1, request, 102, "DISPATCHER");

        assertFalse(result.isSuccess());
        assertEquals(ServiceResult.Status.CONFLICT, result.getStatus());
        assertTrue(result.getMessage().contains("Cannot perform AI triage on " + terminalStatus + " case"));
    }

    // -------------------------------------------------------------------------
    // 5. Role & Assignment Authorization Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Paramedic assigned to case successfully executes AI triage")
    void testTriageCase_AssignedParamedic_Success() {
        StubCaseDAO caseDAO = new StubCaseDAO();
        // User 201 is assigned to Case 1
        caseDAO.store.put(1, new EmergencyCase(1, 1, "MG Road", "CARDIAC", "Chest pain", "MODERATE", "REGISTERED"));
        EmergencyCaseService service = createService(caseDAO);

        CaseTriageRequest request = new CaseTriageRequest(
                1, 80, 120, 80, 95.0, 37.0, 18, 90.0, "Chest pain"
        );

        ServiceResult<CaseTriageResponse> result = service.triageCaseResult(1, request, 201, "PARAMEDIC");

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().getCaseId());
    }

    @Test
    @DisplayName("Paramedic unassigned to case returns not found (safe anti-enumeration)")
    void testTriageCase_UnassignedParamedic_ReturnsNotFound() {
        StubCaseDAO caseDAO = new StubCaseDAO();
        // User 201 is NOT assigned to Case 3
        caseDAO.store.put(3, new EmergencyCase(3, 1, "MG Road", "CARDIAC", "Chest pain", "MODERATE", "REGISTERED"));
        EmergencyCaseService service = createService(caseDAO);

        CaseTriageRequest request = new CaseTriageRequest(
                1, 80, 120, 80, 95.0, 37.0, 18, 90.0, "Chest pain"
        );

        ServiceResult<CaseTriageResponse> result = service.triageCaseResult(3, request, 201, "PARAMEDIC");

        assertFalse(result.isSuccess());
        assertEquals(ServiceResult.Status.NOT_FOUND, result.getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {"AMBULANCE_DRIVER", "HOSPITAL_ADMIN"})
    @DisplayName("Unauthorized roles (driver, hospital admin) are denied AI triage execution")
    void testTriageCase_UnauthorizedRoles_Denied(String unauthorizedRole) {
        StubCaseDAO caseDAO = new StubCaseDAO();
        caseDAO.store.put(1, new EmergencyCase(1, 1, "MG Road", "CARDIAC", "Chest pain", "MODERATE", "REGISTERED"));
        EmergencyCaseService service = createService(caseDAO);

        CaseTriageRequest request = new CaseTriageRequest(
                1, 80, 120, 80, 95.0, 37.0, 18, 90.0, "Chest pain"
        );

        ServiceResult<CaseTriageResponse> result = service.triageCaseResult(1, request, 105, unauthorizedRole);

        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Missing case ID returns not found")
    void testTriageCase_MissingCase_ReturnsNotFound() {
        StubCaseDAO caseDAO = new StubCaseDAO();
        EmergencyCaseService service = createService(caseDAO);

        CaseTriageRequest request = new CaseTriageRequest(
                1, 80, 120, 80, 95.0, 37.0, 18, 90.0, "Chest pain"
        );

        ServiceResult<CaseTriageResponse> result = service.triageCaseResult(999, request, 101, "ADMIN");

        assertFalse(result.isSuccess());
        assertEquals(ServiceResult.Status.NOT_FOUND, result.getStatus());
    }

    // -------------------------------------------------------------------------
    // 6. Database Error & Concurrency Protection Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Database failure during priority escalation returns database error")
    void testTriageCase_DatabaseError_ReturnsError() {
        StubCaseDAO caseDAO = new StubCaseDAO();
        caseDAO.store.put(1, new EmergencyCase(1, 1, "MG Road", "CARDIAC", "Chest pain", "MODERATE", "REGISTERED"));
        caseDAO.simulateDbError = true;
        EmergencyCaseService service = createService(caseDAO);

        CaseTriageRequest request = new CaseTriageRequest(
                1, 150, 70, 40, 85.0, 39.0, 35, 110.0, "Cardiac arrest"
        );

        ServiceResult<CaseTriageResponse> result = service.triageCaseResult(1, request, 101, "ADMIN");

        assertFalse(result.isSuccess());
        assertEquals(ServiceResult.Status.DATABASE_ERROR, result.getStatus());
    }
}
