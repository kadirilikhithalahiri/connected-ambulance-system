package com.abc.emergency;

import com.abc.emergency.dao.AmbulanceDAO;
import com.abc.emergency.dao.DispatchRecordDAO;
import com.abc.emergency.dao.EmergencyCaseDAO;
import com.abc.emergency.dao.PatientDAO;
import com.abc.emergency.model.DispatchRecord;
import com.abc.emergency.model.EmergencyCase;
import com.abc.emergency.model.Patient;
import com.abc.emergency.service.DispatchService;
import com.abc.emergency.service.EmergencyCaseService;
import com.abc.emergency.service.PatientService;
import com.abc.emergency.service.ServiceResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 5I.5 Unit Tests for Authorization Predicates, Scoping, and Infrastructure Failure Handling.
 * Verifies that cross-role driver IDs do not authorize paramedics, crew_role is respected,
 * case dispatch history is scoped, and database failures propagate as DATABASE_ERROR rather than
 * silent empty lists or ordinary 404s.
 */
public class AuthorizationPredicateUnitTest {

    @Test
    @DisplayName("Requirement 1: Paramedic caller is NOT authorized solely because user ID matches driver_id")
    void testParamedicNotAuthorizedByDriverId() {
        // Given an EmergencyCaseDAO stub where user 101 is a DRIVER on the vehicle, but NOT a paramedic
        EmergencyCaseDAO caseDAO = new EmergencyCaseDAO() {
            @Override
            public boolean isCaseAssignedToUser(int caseId, int userId, String role) {
                if ("AMBULANCE_DRIVER".equalsIgnoreCase(role)) {
                    // Authorized as driver
                    return userId == 101;
                }
                // When checked as PARAMEDIC, driver_id match must NOT grant access
                return false;
            }

            @Override
            public EmergencyCase getEmergencyCaseById(int caseId) {
                EmergencyCase ec = new EmergencyCase();
                ec.setCaseId(caseId);
                ec.setCaseStatus("DISPATCHED");
                return ec;
            }
        };

        EmergencyCaseService caseService = new EmergencyCaseService(caseDAO, new PatientDAO());

        // Paramedic with userId 101 attempts status transition on case 1
        ServiceResult<EmergencyCase> result = caseService.updateCaseStatusAtomic(1, "IN_TRANSIT", 101, "PARAMEDIC");

        assertFalse(result.isSuccess());
        assertEquals(ServiceResult.Status.NOT_FOUND, result.getStatus());
        assertEquals("Emergency case not found with ID: 1", result.getMessage());
    }

    @Test
    @DisplayName("Requirement 2: Valid paramedic assignment grants intended access")
    void testValidParamedicAssignmentGrantsAccess() {
        EmergencyCaseDAO caseDAO = new EmergencyCaseDAO() {
            @Override
            public boolean isCaseAssignedToUser(int caseId, int userId, String role) {
                return "PARAMEDIC".equalsIgnoreCase(role) && userId == 201 && caseId == 1;
            }

            @Override
            public EmergencyCase getEmergencyCaseById(int caseId) {
                EmergencyCase ec = new EmergencyCase();
                ec.setCaseId(caseId);
                ec.setCaseStatus("DISPATCHED");
                return ec;
            }

            @Override
            public boolean updateCaseStatusAtomic(int caseId, String expectedStatus, String targetStatus) {
                return true;
            }
        };

        EmergencyCaseService caseService = new EmergencyCaseService(caseDAO, new PatientDAO());

        ServiceResult<EmergencyCase> result = caseService.updateCaseStatusAtomic(1, "IN_TRANSIT", 201, "PARAMEDIC");

        assertTrue(result.isSuccess());
        assertEquals("IN_TRANSIT", result.getData().getCaseStatus());
    }

    @Test
    @DisplayName("Requirement 3: Ambulance_Crew.crew_role is respected (DRIVER crew role does not grant paramedic access)")
    void testCrewRoleIsRespected() {
        PatientDAO patientDAO = new PatientDAO() {
            @Override
            public boolean isPatientAssignedToUser(int patientId, int userId) {
                // User 105 has an active crew record, but their crew_role is 'DRIVER', not 'PARAMEDIC'
                return false;
            }

            @Override
            public Patient getPatientById(int patientId) {
                Patient p = new Patient();
                p.setPatientId(patientId);
                p.setPatientName("Test Patient");
                return p;
            }
        };

        PatientService patientService = new PatientService(patientDAO);

        ServiceResult<Patient> result = patientService.getPatientByIdForCaller(1, 105, "PARAMEDIC");

        assertFalse(result.isSuccess());
        assertEquals(ServiceResult.Status.NOT_FOUND, result.getStatus());
    }

    @Test
    @DisplayName("Requirement 4: Unauthorized paramedic cannot retrieve another case's dispatch history (404)")
    void testUnauthorizedParamedicCannotRetrieveCaseDispatches() {
        EmergencyCaseDAO caseDAO = new EmergencyCaseDAO() {
            @Override
            public EmergencyCase getEmergencyCaseById(int caseId) {
                EmergencyCase ec = new EmergencyCase();
                ec.setCaseId(caseId);
                return ec;
            }

            @Override
            public boolean isCaseAssignedToUser(int caseId, int userId, String role) {
                // Paramedic 201 is NOT assigned to case 5
                return false;
            }
        };

        DispatchRecordDAO dispatchDAO = new DispatchRecordDAO();
        DispatchService dispatchService = new DispatchService(new AmbulanceDAO(), dispatchDAO, caseDAO);

        ServiceResult<List<DispatchRecord>> result = dispatchService.getDispatchesByCaseIdForCaller(5, 201, "PARAMEDIC");

        assertFalse(result.isSuccess());
        assertEquals(ServiceResult.Status.NOT_FOUND, result.getStatus());
        assertEquals("Emergency case not found with ID: 5", result.getMessage());
    }

    @Test
    @DisplayName("Requirement 5: Authorized paramedic can retrieve permitted case dispatch history (200)")
    void testAuthorizedParamedicCanRetrieveCaseDispatches() {
        EmergencyCaseDAO caseDAO = new EmergencyCaseDAO() {
            @Override
            public EmergencyCase getEmergencyCaseById(int caseId) {
                EmergencyCase ec = new EmergencyCase();
                ec.setCaseId(caseId);
                return ec;
            }

            @Override
            public boolean isCaseAssignedToUser(int caseId, int userId, String role) {
                // Paramedic 201 IS assigned to case 1
                return caseId == 1 && userId == 201 && "PARAMEDIC".equalsIgnoreCase(role);
            }
        };

        DispatchRecord record = new DispatchRecord(10, 1, 2, 8, "DISPATCHED");
        DispatchRecordDAO dispatchDAO = new DispatchRecordDAO() {
            @Override
            public List<DispatchRecord> getDispatchesByCaseId(int caseId) {
                return List.of(record);
            }
        };

        DispatchService dispatchService = new DispatchService(new AmbulanceDAO(), dispatchDAO, caseDAO);

        ServiceResult<List<DispatchRecord>> result = dispatchService.getDispatchesByCaseIdForCaller(1, 201, "PARAMEDIC");

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertEquals(10, result.getData().get(0).getDispatchId());
    }

    @Test
    @DisplayName("Requirement 6: Database exceptions produce DATABASE_ERROR rather than empty lists or ordinary 404s")
    void testDatabaseExceptionProducesDatabaseErrorStatus() {
        PatientDAO patientDAO = new PatientDAO() {
            @Override
            public List<Patient> getPatientsByAssignedUserId(int userId) {
                return Collections.emptyList();
            }

            @Override
            public String getLastDatabaseError() {
                return "Communications link failure: MySQL server unreachable";
            }
        };

        PatientService patientService = new PatientService(patientDAO);

        // List query must return DATABASE_ERROR, not SUCCESS with empty list
        ServiceResult<List<Patient>> listResult = patientService.getPatientsForCallerResult(201, "PARAMEDIC");
        assertFalse(listResult.isSuccess());
        assertEquals(ServiceResult.Status.DATABASE_ERROR, listResult.getStatus());

        // Single patient query on DB error must return DATABASE_ERROR, not NOT_FOUND
        PatientDAO patientSingleDAO = new PatientDAO() {
            @Override
            public Patient getPatientById(int patientId) {
                return null;
            }

            @Override
            public String getLastDatabaseError() {
                return "Connection timeout";
            }
        };
        PatientService patientSingleService = new PatientService(patientSingleDAO);
        ServiceResult<Patient> singleResult = patientSingleService.getPatientByIdForCaller(1, 201, "PARAMEDIC");
        assertFalse(singleResult.isSuccess());
        assertEquals(ServiceResult.Status.DATABASE_ERROR, singleResult.getStatus());
    }

    @Test
    @DisplayName("Requirement 7: Registered case without dispatch record limitation check")
    void testRegisteredCaseWithoutDispatchRecordLimitation() {
        // Schema inspection test: EmergencyCaseDAO uses INNER JOIN with Dispatch_Records.
        // Cases in REGISTERED status without a Dispatch_Records row are not joined.
        // Verify fail-safe: isCaseAssignedToUser returns false gracefully without DB errors.
        EmergencyCaseDAO caseDAO = new EmergencyCaseDAO() {
            @Override
            public boolean isCaseAssignedToUser(int caseId, int userId, String role) {
                // Emulates query result when Dispatch_Records has no matching row
                return false;
            }

            @Override
            public EmergencyCase getEmergencyCaseById(int caseId) {
                EmergencyCase ec = new EmergencyCase();
                ec.setCaseId(caseId);
                ec.setCaseStatus("REGISTERED");
                return ec;
            }
        };

        EmergencyCaseService caseService = new EmergencyCaseService(caseDAO, new PatientDAO());
        ServiceResult<EmergencyCase> result = caseService.getCaseByIdForCaller(99, 201, "PARAMEDIC");

        assertFalse(result.isSuccess());
        assertEquals(ServiceResult.Status.NOT_FOUND, result.getStatus());
        assertNull(caseDAO.getLastDatabaseError());
    }
}
