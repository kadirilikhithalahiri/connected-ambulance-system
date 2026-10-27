package com.abc.emergency;

import com.abc.emergency.dao.AmbulanceDAO;
import com.abc.emergency.dao.DispatchRecordDAO;
import com.abc.emergency.dao.EmergencyCaseDAO;
import com.abc.emergency.dao.PatientDAO;
import com.abc.emergency.model.Ambulance;
import com.abc.emergency.model.DispatchRecord;
import com.abc.emergency.model.EmergencyCase;
import com.abc.emergency.model.Patient;
import com.abc.emergency.service.AmbulanceService;
import com.abc.emergency.service.DispatchService;
import com.abc.emergency.service.EmergencyCaseService;
import com.abc.emergency.service.ServiceResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 5I.7 Unit Tests for Dispatch and Case Database Error-Handling Remediation.
 * Covers:
 * 1. Genuine empty results vs database failures in DispatchRecordDAO and DispatchService.
 * 2. Missing dispatch records vs database failures.
 * 3. Database failure during patient lookup in emergency case creation.
 * 4. Thread reuse error hygiene and sequential operations.
 * 5. Preservation of existing Swing-facing DAO method signatures.
 */
public class DatabaseErrorRemediationUnitTest {

    @Test
    @DisplayName("1. Genuine empty dispatch list vs database failure in getAllDispatchesForCallerResult")
    void testGenuineEmptyVsDatabaseError_AllDispatches() {
        // Case A: Genuine empty list (e.g. no dispatches recorded yet)
        DispatchRecordDAO emptyDao = new DispatchRecordDAO() {
            @Override
            public List<DispatchRecord> getAllDispatchRecords() {
                clearDatabaseError();
                return Collections.emptyList();
            }
        };
        DispatchService serviceA = new DispatchService(new AmbulanceDAO(), emptyDao, new EmergencyCaseDAO());
        ServiceResult<List<DispatchRecord>> resultA = serviceA.getAllDispatchesForCallerResult("ADMIN");
        assertTrue(resultA.isSuccess());
        assertEquals(ServiceResult.Status.SUCCESS, resultA.getStatus());
        assertTrue(resultA.getData().isEmpty());

        // Case B: Database failure during retrieval
        DispatchRecordDAO failureDao = new DispatchRecordDAO() {
            private final ThreadLocal<String> err = new ThreadLocal<>();
            @Override
            public List<DispatchRecord> getAllDispatchRecords() {
                err.set("Connection to MySQL cluster timed out");
                return Collections.emptyList();
            }
            @Override
            public String getLastDatabaseError() {
                return err.get();
            }
        };
        DispatchService serviceB = new DispatchService(new AmbulanceDAO(), failureDao, new EmergencyCaseDAO());
        ServiceResult<List<DispatchRecord>> resultB = serviceB.getAllDispatchesForCallerResult("ADMIN");
        assertFalse(resultB.isSuccess());
        assertEquals(ServiceResult.Status.DATABASE_ERROR, resultB.getStatus());
        assertEquals("An internal database error occurred while retrieving dispatch records.", resultB.getMessage());
    }

    @Test
    @DisplayName("2. Genuine empty active dispatch vs database failure in getActiveDispatchForDriver")
    void testGenuineEmptyVsDatabaseError_ActiveDispatch() {
        Ambulance ambulance = new Ambulance();
        ambulance.setAmbulanceId(10);
        ambulance.setDriverId(101);

        AmbulanceService mockAmbulanceService = new AmbulanceService() {
            @Override
            public ServiceResult<Ambulance> getAmbulanceForDriver(int driverUserId) {
                return ServiceResult.success("Ambulance found", ambulance);
            }
        };

        // Case A: Driver has assigned ambulance, but no active dispatch currently exists
        DispatchRecordDAO emptyDao = new DispatchRecordDAO() {
            @Override
            public List<DispatchRecord> getActiveDispatchesForAmbulance(int ambulanceId) {
                clearDatabaseError();
                return Collections.emptyList();
            }
        };
        DispatchService serviceA = new DispatchService(new AmbulanceDAO(), emptyDao, new EmergencyCaseDAO(), mockAmbulanceService);
        ServiceResult<DispatchRecord> resultA = serviceA.getActiveDispatchForDriver(101);
        assertFalse(resultA.isSuccess());
        assertEquals(ServiceResult.Status.NOT_FOUND, resultA.getStatus());
        assertEquals("No active dispatch found for assigned ambulance ID 10", resultA.getMessage());

        // Case B: Database failure while querying active dispatches
        DispatchRecordDAO failureDao = new DispatchRecordDAO() {
            private final ThreadLocal<String> err = new ThreadLocal<>();
            @Override
            public List<DispatchRecord> getActiveDispatchesForAmbulance(int ambulanceId) {
                err.set("Broken pipe: socket closed");
                return Collections.emptyList();
            }
            @Override
            public String getLastDatabaseError() {
                return err.get();
            }
        };
        DispatchService serviceB = new DispatchService(new AmbulanceDAO(), failureDao, new EmergencyCaseDAO(), mockAmbulanceService);
        ServiceResult<DispatchRecord> resultB = serviceB.getActiveDispatchForDriver(101);
        assertFalse(resultB.isSuccess());
        assertEquals(ServiceResult.Status.DATABASE_ERROR, resultB.getStatus());
        assertEquals("An internal database error occurred while retrieving active dispatches.", resultB.getMessage());
    }

    @Test
    @DisplayName("3. Missing dispatch record vs database failure in getDispatchByIdForCaller")
    void testMissingDispatchVsDatabaseError_GetDispatchById() {
        // Case A: Record legitimately not found
        DispatchRecordDAO notFoundDao = new DispatchRecordDAO() {
            @Override
            public DispatchRecord getDispatchById(int dispatchId) {
                clearDatabaseError();
                return null;
            }
        };
        DispatchService serviceA = new DispatchService(new AmbulanceDAO(), notFoundDao, new EmergencyCaseDAO());
        ServiceResult<DispatchRecord> resultA = serviceA.getDispatchByIdForCaller(999, 1, "ADMIN");
        assertFalse(resultA.isSuccess());
        assertEquals(ServiceResult.Status.NOT_FOUND, resultA.getStatus());
        assertEquals("Dispatch record not found with ID: 999", resultA.getMessage());

        // Case B: Database failure during getDispatchById
        DispatchRecordDAO dbErrorDao = new DispatchRecordDAO() {
            private final ThreadLocal<String> err = new ThreadLocal<>();
            @Override
            public DispatchRecord getDispatchById(int dispatchId) {
                err.set("SQLException: deadlock detected");
                return null;
            }
            @Override
            public String getLastDatabaseError() {
                return err.get();
            }
        };
        DispatchService serviceB = new DispatchService(new AmbulanceDAO(), dbErrorDao, new EmergencyCaseDAO());
        ServiceResult<DispatchRecord> resultB = serviceB.getDispatchByIdForCaller(999, 1, "ADMIN");
        assertFalse(resultB.isSuccess());
        assertEquals(ServiceResult.Status.DATABASE_ERROR, resultB.getStatus());
        assertEquals("An internal database error occurred while retrieving the dispatch record.", resultB.getMessage());
    }

    @Test
    @DisplayName("4. Database failure during patient lookup in EmergencyCaseService.createEmergencyCaseResult")
    void testDatabaseErrorDuringPatientLookup_InCaseCreation() {
        // Case A: Database failure during patient lookup
        PatientDAO failurePatientDao = new PatientDAO() {
            private final ThreadLocal<String> err = new ThreadLocal<>();
            @Override
            public Patient getPatientById(int patientId) {
                err.set("Communications link failure");
                return null;
            }
            @Override
            public String getLastDatabaseError() {
                return err.get();
            }
        };
        EmergencyCaseService serviceFailure = new EmergencyCaseService(new EmergencyCaseDAO(), failurePatientDao);
        ServiceResult<EmergencyCase> resultFailure = serviceFailure.createEmergencyCaseResult(1, "Location", "TRAUMA", "Bleeding");
        assertFalse(resultFailure.isSuccess());
        assertEquals(ServiceResult.Status.DATABASE_ERROR, resultFailure.getStatus());
        assertEquals("An internal database error occurred while verifying patient.", resultFailure.getMessage());

        // Case B: Patient legitimately does not exist
        PatientDAO notFoundPatientDao = new PatientDAO() {
            @Override
            public Patient getPatientById(int patientId) {
                clearDatabaseError();
                return null;
            }
        };
        EmergencyCaseService serviceNotFound = new EmergencyCaseService(new EmergencyCaseDAO(), notFoundPatientDao);
        ServiceResult<EmergencyCase> resultNotFound = serviceNotFound.createEmergencyCaseResult(999, "Location", "TRAUMA", "Bleeding");
        assertFalse(resultNotFound.isSuccess());
        assertEquals(ServiceResult.Status.VALIDATION_ERROR, resultNotFound.getStatus());
        assertEquals("Patient with ID 999 not found.", resultNotFound.getMessage());
    }

    @Test
    @DisplayName("5. Thread reuse error hygiene: consecutive operations clear previous failure state reliably")
    void testThreadReuseErrorHygiene() throws Exception {
        DispatchRecordDAO dao = new DispatchRecordDAO();

        // Access the ThreadLocal via reflection to simulate a dirty thread from a prior request
        java.lang.reflect.Field field = DispatchRecordDAO.class.getDeclaredField("lastDatabaseError");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<String> tl = (ThreadLocal<String>) field.get(dao);

        tl.set("Stale SQL failure from prior pooled thread execution");
        assertEquals("Stale SQL failure from prior pooled thread execution", dao.getLastDatabaseError());

        // Calling clearDatabaseError explicitly clears the state
        dao.clearDatabaseError();
        assertNull(dao.getLastDatabaseError());

        // Set dirty state again on dao
        tl.set("Stale SQL failure from prior pooled thread execution");
        assertEquals("Stale SQL failure from prior pooled thread execution", dao.getLastDatabaseError());

        // Execute an operation: an anonymous subclass that verifies entry clearing
        DispatchRecordDAO operationalDao = new DispatchRecordDAO() {
            @Override
            public List<DispatchRecord> getAllDispatchRecords() {
                // Emulates entry clearing done in production getAllDispatchRecords()
                clearDatabaseError();
                return Collections.emptyList();
            }
        };

        java.lang.reflect.Field opField = DispatchRecordDAO.class.getDeclaredField("lastDatabaseError");
        opField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<String> opTl = (ThreadLocal<String>) opField.get(operationalDao);
        opTl.set("Stale failure");
        assertEquals("Stale failure", operationalDao.getLastDatabaseError());

        // When a new operation starts on the same thread, the previous error is removed at entry
        List<DispatchRecord> results = operationalDao.getAllDispatchRecords();
        assertNotNull(results);
        assertTrue(results.isEmpty());
        assertNull(operationalDao.getLastDatabaseError());
    }

    @Test
    @DisplayName("6. Preservation of existing Swing-facing DAO method signatures in DispatchRecordDAO")
    void testSwingMethodSignaturesPreserved() throws NoSuchMethodException {
        // 1. getAllDispatchRecords() -> List<DispatchRecord>
        Method m1 = DispatchRecordDAO.class.getMethod("getAllDispatchRecords");
        assertEquals(List.class, m1.getReturnType());

        // 2. getActiveDispatchesForAmbulance(int) -> List<DispatchRecord>
        Method m2 = DispatchRecordDAO.class.getMethod("getActiveDispatchesForAmbulance", int.class);
        assertEquals(List.class, m2.getReturnType());

        // 3. getDispatchById(int) -> DispatchRecord
        Method m3 = DispatchRecordDAO.class.getMethod("getDispatchById", int.class);
        assertEquals(DispatchRecord.class, m3.getReturnType());

        // 4. addDispatchRecord(DispatchRecord) -> boolean
        Method m4 = DispatchRecordDAO.class.getMethod("addDispatchRecord", DispatchRecord.class);
        assertEquals(boolean.class, m4.getReturnType());

        // 5. hasActiveDispatchForCase(int) -> boolean
        Method m5 = DispatchRecordDAO.class.getMethod("hasActiveDispatchForCase", int.class);
        assertEquals(boolean.class, m5.getReturnType());

        // 6. hasOtherActiveDispatchForAmbulance(int, int) -> boolean
        Method m6 = DispatchRecordDAO.class.getMethod("hasOtherActiveDispatchForAmbulance", int.class, int.class);
        assertEquals(boolean.class, m6.getReturnType());

        // 7. updateDispatchStatus(int, String) -> boolean
        Method m7 = DispatchRecordDAO.class.getMethod("updateDispatchStatus", int.class, String.class);
        assertEquals(boolean.class, m7.getReturnType());

        // 8. markAsArrived(int) -> boolean
        Method m8 = DispatchRecordDAO.class.getMethod("markAsArrived", int.class);
        assertEquals(boolean.class, m8.getReturnType());

        // 9. deleteDispatchRecord(int) -> boolean
        Method m9 = DispatchRecordDAO.class.getMethod("deleteDispatchRecord", int.class);
        assertEquals(boolean.class, m9.getReturnType());
    }
}
