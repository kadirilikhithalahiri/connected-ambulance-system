package com.abc.emergency;

import com.abc.emergency.service.EmergencyCaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EmergencyCaseTransitionUnitTest {

    private EmergencyCaseService service;

    @BeforeEach
    void setUp() {
        // No-arg constructor or test double; methods tested are pure logic
        service = new EmergencyCaseService();
    }

    @Test
    void testValidStatusTransitions() {
        // From REGISTERED: only CANCELLED is allowed directly; DISPATCHED requires formal dispatch
        assertFalse(service.isAllowedTransition("REGISTERED", "DISPATCHED"));
        assertTrue(service.isAllowedTransition("REGISTERED", "CANCELLED"));
        assertFalse(service.isAllowedTransition("REGISTERED", "IN_TRANSIT"));
        assertFalse(service.isAllowedTransition("REGISTERED", "ARRIVED"));
        assertFalse(service.isAllowedTransition("REGISTERED", "COMPLETED"));
        assertFalse(service.isAllowedTransition("REGISTERED", "REGISTERED"));

        // From DISPATCHED
        assertTrue(service.isAllowedTransition("DISPATCHED", "IN_TRANSIT"));
        assertTrue(service.isAllowedTransition("DISPATCHED", "CANCELLED"));
        assertFalse(service.isAllowedTransition("DISPATCHED", "REGISTERED"));
        assertFalse(service.isAllowedTransition("DISPATCHED", "ARRIVED"));
        assertFalse(service.isAllowedTransition("DISPATCHED", "COMPLETED"));
        assertFalse(service.isAllowedTransition("DISPATCHED", "DISPATCHED"));

        // From IN_TRANSIT
        assertTrue(service.isAllowedTransition("IN_TRANSIT", "ARRIVED"));
        assertTrue(service.isAllowedTransition("IN_TRANSIT", "CANCELLED"));
        assertFalse(service.isAllowedTransition("IN_TRANSIT", "REGISTERED"));
        assertFalse(service.isAllowedTransition("IN_TRANSIT", "DISPATCHED"));
        assertFalse(service.isAllowedTransition("IN_TRANSIT", "COMPLETED"));
        assertFalse(service.isAllowedTransition("IN_TRANSIT", "IN_TRANSIT"));

        // From ARRIVED
        assertTrue(service.isAllowedTransition("ARRIVED", "COMPLETED"));
        assertTrue(service.isAllowedTransition("ARRIVED", "CANCELLED"));
        assertFalse(service.isAllowedTransition("ARRIVED", "REGISTERED"));
        assertFalse(service.isAllowedTransition("ARRIVED", "DISPATCHED"));
        assertFalse(service.isAllowedTransition("ARRIVED", "IN_TRANSIT"));
        assertFalse(service.isAllowedTransition("ARRIVED", "ARRIVED"));
    }

    @Test
    void testTerminalStatesAreImmutable() {
        // From COMPLETED (terminal)
        assertFalse(service.isAllowedTransition("COMPLETED", "REGISTERED"));
        assertFalse(service.isAllowedTransition("COMPLETED", "DISPATCHED"));
        assertFalse(service.isAllowedTransition("COMPLETED", "IN_TRANSIT"));
        assertFalse(service.isAllowedTransition("COMPLETED", "ARRIVED"));
        assertFalse(service.isAllowedTransition("COMPLETED", "CANCELLED"));
        assertFalse(service.isAllowedTransition("COMPLETED", "COMPLETED"));

        // From CANCELLED (terminal)
        assertFalse(service.isAllowedTransition("CANCELLED", "REGISTERED"));
        assertFalse(service.isAllowedTransition("CANCELLED", "DISPATCHED"));
        assertFalse(service.isAllowedTransition("CANCELLED", "IN_TRANSIT"));
        assertFalse(service.isAllowedTransition("CANCELLED", "ARRIVED"));
        assertFalse(service.isAllowedTransition("CANCELLED", "COMPLETED"));
        assertFalse(service.isAllowedTransition("CANCELLED", "CANCELLED"));
    }

    @Test
    void testNullAndInvalidInputs() {
        assertFalse(service.isAllowedTransition(null, "DISPATCHED"));
        assertFalse(service.isAllowedTransition("REGISTERED", null));
        assertFalse(service.isAllowedTransition(null, null));
        assertFalse(service.isAllowedTransition("UNKNOWN_STATUS", "DISPATCHED"));
        assertFalse(service.isAllowedTransition("REGISTERED", "UNKNOWN_STATUS"));
    }

    @Test
    void testCaseStatusWhitelist() {
        assertTrue(service.isValidStatus("REGISTERED"));
        assertTrue(service.isValidStatus("DISPATCHED"));
        assertTrue(service.isValidStatus("IN_TRANSIT"));
        assertTrue(service.isValidStatus("ARRIVED"));
        assertTrue(service.isValidStatus("COMPLETED"));
        assertTrue(service.isValidStatus("CANCELLED"));

        // Case insensitivity
        assertTrue(service.isValidStatus("registered"));
        assertTrue(service.isValidStatus("Dispatched"));

        // Invalid
        assertFalse(service.isValidStatus(null));
        assertFalse(service.isValidStatus(""));
        assertFalse(service.isValidStatus("UNKNOWN"));
        assertFalse(service.isValidStatus("PENDING"));
    }

    @Test
    void testDirectRegisteredToDispatchedIsProhibitedInService() {
        // Direct transition from REGISTERED to DISPATCHED must be rejected with CONFLICT
        assertFalse(service.isAllowedTransition("REGISTERED", "DISPATCHED"));

        com.abc.emergency.dao.EmergencyCaseDAO stubCaseDAO = new com.abc.emergency.dao.EmergencyCaseDAO() {
            @Override
            public com.abc.emergency.model.EmergencyCase getEmergencyCaseById(int caseId) {
                com.abc.emergency.model.EmergencyCase ec = new com.abc.emergency.model.EmergencyCase();
                ec.setCaseId(caseId);
                ec.setCaseStatus("REGISTERED");
                return ec;
            }

            @Override
            public boolean updateCaseStatusAtomic(int caseId, String expectedStatus, String targetStatus) {
                fail("DAO updateCaseStatusAtomic should NOT be called when transition is rejected");
                return false;
            }
        };

        com.abc.emergency.dao.PatientDAO stubPatientDAO = new com.abc.emergency.dao.PatientDAO();
        EmergencyCaseService scopedService = new EmergencyCaseService(stubCaseDAO, stubPatientDAO);

        com.abc.emergency.service.ServiceResult<com.abc.emergency.model.EmergencyCase> result =
                scopedService.updateCaseStatusAtomic(1, "DISPATCHED");

        assertFalse(result.isSuccess());
        assertEquals(com.abc.emergency.service.ServiceResult.Status.CONFLICT, result.getStatus());
        assertEquals("Invalid status transition from REGISTERED to DISPATCHED.", result.getMessage());
        assertFalse(scopedService.updateCaseStatus(1, "DISPATCHED"));
    }

    @Test
    void testDirectRegisteredToCancelledIsAllowedInService() {
        assertTrue(service.isAllowedTransition("REGISTERED", "CANCELLED"));

        com.abc.emergency.dao.EmergencyCaseDAO stubCaseDAO = new com.abc.emergency.dao.EmergencyCaseDAO() {
            @Override
            public com.abc.emergency.model.EmergencyCase getEmergencyCaseById(int caseId) {
                com.abc.emergency.model.EmergencyCase ec = new com.abc.emergency.model.EmergencyCase();
                ec.setCaseId(caseId);
                ec.setCaseStatus("REGISTERED");
                return ec;
            }

            @Override
            public boolean updateCaseStatusAtomic(int caseId, String expectedStatus, String targetStatus) {
                return true;
            }
        };

        com.abc.emergency.dao.PatientDAO stubPatientDAO = new com.abc.emergency.dao.PatientDAO();
        EmergencyCaseService scopedService = new EmergencyCaseService(stubCaseDAO, stubPatientDAO);

        com.abc.emergency.service.ServiceResult<com.abc.emergency.model.EmergencyCase> result =
                scopedService.updateCaseStatusAtomic(1, "CANCELLED");

        assertTrue(result.isSuccess());
        assertEquals("CANCELLED", result.getData().getCaseStatus());
        assertTrue(scopedService.updateCaseStatus(1, "CANCELLED"));
    }

    @Test
    void testParamedicCaseStatusUpdate_UnassignedCase_ReturnsNotFound() {
        com.abc.emergency.dao.EmergencyCaseDAO stubCaseDAO = new com.abc.emergency.dao.EmergencyCaseDAO() {
            @Override
            public com.abc.emergency.model.EmergencyCase getEmergencyCaseById(int caseId) {
                com.abc.emergency.model.EmergencyCase ec = new com.abc.emergency.model.EmergencyCase();
                ec.setCaseId(caseId);
                ec.setCaseStatus("DISPATCHED");
                return ec;
            }

            @Override
            public boolean isCaseAssignedToUser(int caseId, int userId) {
                // Paramedic 201 is NOT assigned to case 2
                return false;
            }

            @Override
            public boolean isCaseAssignedToUser(int caseId, int userId, String role) {
                return isCaseAssignedToUser(caseId, userId);
            }

            @Override
            public boolean updateCaseStatusAtomic(int caseId, String expectedStatus, String targetStatus) {
                fail("DAO update must NOT be executed for unassigned case");
                return false;
            }
        };

        EmergencyCaseService scopedService = new EmergencyCaseService(stubCaseDAO, new com.abc.emergency.dao.PatientDAO());

        com.abc.emergency.service.ServiceResult<com.abc.emergency.model.EmergencyCase> result =
                scopedService.updateCaseStatusAtomic(2, "IN_TRANSIT", 201, "PARAMEDIC");

        assertFalse(result.isSuccess());
        assertEquals(com.abc.emergency.service.ServiceResult.Status.NOT_FOUND, result.getStatus());
        assertEquals("Emergency case not found with ID: 2", result.getMessage());
    }

    @Test
    void testParamedicCaseStatusUpdate_AssignedCase_Success() {
        com.abc.emergency.dao.EmergencyCaseDAO stubCaseDAO = new com.abc.emergency.dao.EmergencyCaseDAO() {
            @Override
            public com.abc.emergency.model.EmergencyCase getEmergencyCaseById(int caseId) {
                com.abc.emergency.model.EmergencyCase ec = new com.abc.emergency.model.EmergencyCase();
                ec.setCaseId(caseId);
                ec.setCaseStatus("DISPATCHED");
                return ec;
            }

            @Override
            public boolean isCaseAssignedToUser(int caseId, int userId) {
                // Paramedic 201 is assigned to case 2
                return caseId == 2 && userId == 201;
            }

            @Override
            public boolean isCaseAssignedToUser(int caseId, int userId, String role) {
                return isCaseAssignedToUser(caseId, userId);
            }

            @Override
            public boolean updateCaseStatusAtomic(int caseId, String expectedStatus, String targetStatus) {
                return true;
            }
        };

        EmergencyCaseService scopedService = new EmergencyCaseService(stubCaseDAO, new com.abc.emergency.dao.PatientDAO());

        com.abc.emergency.service.ServiceResult<com.abc.emergency.model.EmergencyCase> result =
                scopedService.updateCaseStatusAtomic(2, "IN_TRANSIT", 201, "PARAMEDIC");

        assertTrue(result.isSuccess());
        assertEquals("IN_TRANSIT", result.getData().getCaseStatus());
    }

    @Test
    void testParamedicCaseStatusUpdate_DatabaseError_ReturnsDatabaseError() {
        com.abc.emergency.dao.EmergencyCaseDAO stubCaseDAO = new com.abc.emergency.dao.EmergencyCaseDAO() {
            @Override
            public com.abc.emergency.model.EmergencyCase getEmergencyCaseById(int caseId) {
                com.abc.emergency.model.EmergencyCase ec = new com.abc.emergency.model.EmergencyCase();
                ec.setCaseId(caseId);
                ec.setCaseStatus("DISPATCHED");
                return ec;
            }

            @Override
            public boolean isCaseAssignedToUser(int caseId, int userId, String role) {
                return false;
            }

            @Override
            public String getLastDatabaseError() {
                return "Simulated database connection failure";
            }
        };

        EmergencyCaseService scopedService = new EmergencyCaseService(stubCaseDAO, new com.abc.emergency.dao.PatientDAO());

        com.abc.emergency.service.ServiceResult<com.abc.emergency.model.EmergencyCase> result =
                scopedService.updateCaseStatusAtomic(2, "IN_TRANSIT", 201, "PARAMEDIC");

        assertFalse(result.isSuccess());
        assertEquals(com.abc.emergency.service.ServiceResult.Status.DATABASE_ERROR, result.getStatus());
        assertTrue(result.getMessage().contains("database error"));
    }
}
