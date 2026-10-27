package com.abc.emergency;

import com.abc.emergency.dao.EmergencyCaseDAO;
import com.abc.emergency.dao.PatientDAO;
import com.abc.emergency.model.EmergencyCase;
import com.abc.emergency.model.Patient;
import com.abc.emergency.service.EmergencyCaseService;
import com.abc.emergency.service.ServiceResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 5J.9 Regression and Unit Tests for Emergency Case Priority Validation
 * and DAO Protection.
 */
public class EmergencyCasePriorityUnitTest {

    // -------------------------------------------------------------------------
    // 1. Priority Validation Logic Tests
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"CRITICAL", "HIGH", "MODERATE", "LOW", "critical", "High", "moderate", "low", "  CRITICAL  ", "  high  "})
    @DisplayName("Valid priorities are accepted regardless of case and surrounding whitespace")
    void testIsValidPriority_ValidValues(String priority) {
        assertTrue(EmergencyCaseService.isValidPriority(priority),
                "Expected valid priority for: " + priority);
    }

    @ParameterizedTest
    @ValueSource(strings = {"EXTREME", "URGENT", "MEDIUM", "SEVERE", "NORMAL", "UNKNOWN", "", "   "})
    @DisplayName("Invalid, empty, and unsupported priority values are rejected")
    void testIsValidPriority_InvalidValues(String priority) {
        assertFalse(EmergencyCaseService.isValidPriority(priority),
                "Expected invalid priority for: " + priority);
    }

    @Test
    @DisplayName("Null priority is rejected by isValidPriority predicate")
    void testIsValidPriority_Null() {
        assertFalse(EmergencyCaseService.isValidPriority(null));
    }

    // -------------------------------------------------------------------------
    // 2. Service-Level Priority Assignment & Defaulting Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Explicit valid priority is normalized to uppercase and persisted")
    void testCreateEmergencyCase_ExplicitValidPriority_Success() {
        PatientDAO stubPatientDAO = new PatientDAO() {
            @Override
            public Patient getPatientById(int id) {
                if (id == 1) {
                    return new Patient(1, "Test Patient", 30, "Male", "O+", "None", "None", "1234567890");
                }
                return null;
            }
        };

        class RecordingCaseDAO extends EmergencyCaseDAO {
            EmergencyCase savedCase;
            int callCount = 0;

            @Override
            public boolean addEmergencyCase(EmergencyCase ec) {
                this.savedCase = ec;
                this.callCount++;
                ec.setCaseId(101);
                return true;
            }
        }

        RecordingCaseDAO stubCaseDAO = new RecordingCaseDAO();
        EmergencyCaseService service = new EmergencyCaseService(stubCaseDAO, stubPatientDAO);

        ServiceResult<EmergencyCase> result = service.createEmergencyCaseResult(
                1, "Brigade Road, Bengaluru", "CARDIAC", "Chest pain", "  critical  "
        );

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("CRITICAL", result.getData().getPriorityLevel(),
                "Explicit priority must be normalized to uppercase");
        assertEquals(1, stubCaseDAO.callCount);
        assertEquals("CRITICAL", stubCaseDAO.savedCase.getPriorityLevel());
    }

    @Test
    @DisplayName("Missing priority (null) defaults to MODERATE without guessing from symptom text")
    void testCreateEmergencyCase_MissingPriority_DefaultsToModerate() {
        PatientDAO stubPatientDAO = new PatientDAO() {
            @Override
            public Patient getPatientById(int id) {
                if (id == 1) {
                    return new Patient(1, "Test Patient", 30, "Male", "O+", "None", "None", "1234567890");
                }
                return null;
            }
        };

        class RecordingCaseDAO extends EmergencyCaseDAO {
            EmergencyCase savedCase;
            int callCount = 0;

            @Override
            public boolean addEmergencyCase(EmergencyCase ec) {
                this.savedCase = ec;
                this.callCount++;
                ec.setCaseId(102);
                return true;
            }
        }

        RecordingCaseDAO stubCaseDAO = new RecordingCaseDAO();
        EmergencyCaseService service = new EmergencyCaseService(stubCaseDAO, stubPatientDAO);

        // Symptoms contain critical-sounding clinical keywords, but system must NOT invent clinical priority
        ServiceResult<EmergencyCase> result = service.createEmergencyCaseResult(
                1, "Brigade Road, Bengaluru", "CARDIAC", "Severe cardiac arrest and unconsciousness", null
        );

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("MODERATE", result.getData().getPriorityLevel(),
                "Missing priority must default to MODERATE per existing API contract");
        assertEquals(1, stubCaseDAO.callCount);
        assertEquals("MODERATE", stubCaseDAO.savedCase.getPriorityLevel());
    }

    @ParameterizedTest
    @ValueSource(strings = {"EXTREME", "URGENT", "INVALID", "", "   "})
    @DisplayName("Invalid explicit priority returns validation error and does not invoke DAO")
    void testCreateEmergencyCase_InvalidPriority_ReturnsValidationError(String invalidPriority) {
        class GuardPatientDAO extends PatientDAO {
            boolean called = false;
            @Override
            public Patient getPatientById(int id) {
                called = true;
                return new Patient(1, "Test", 30, "Male", "O+", "None", "None", "1234567890");
            }
        }

        class GuardCaseDAO extends EmergencyCaseDAO {
            boolean called = false;
            @Override
            public boolean addEmergencyCase(EmergencyCase ec) {
                called = true;
                return true;
            }
        }

        GuardPatientDAO guardPatientDAO = new GuardPatientDAO();
        GuardCaseDAO guardCaseDAO = new GuardCaseDAO();
        EmergencyCaseService service = new EmergencyCaseService(guardCaseDAO, guardPatientDAO);

        ServiceResult<EmergencyCase> result = service.createEmergencyCaseResult(
                1, "Brigade Road, Bengaluru", "CARDIAC", "Chest pain", invalidPriority
        );

        assertFalse(result.isSuccess());
        assertEquals(ServiceResult.Status.VALIDATION_ERROR, result.getStatus());
        assertTrue(result.getMessage().contains("Invalid priority level"));
        assertFalse(guardCaseDAO.called, "DAO addEmergencyCase must not be called on validation error");
        assertFalse(guardPatientDAO.called, "PatientDAO getPatientById must not be called on priority validation error");
    }

    // -------------------------------------------------------------------------
    // 3. DAO Protection & Priority Preservation Tests (Using JDK Dynamic Proxies)
    // -------------------------------------------------------------------------

    static class StatementHandler implements InvocationHandler {
        final Map<Integer, Object> boundParameters = new HashMap<>();
        int executeUpdateReturnValue = 1;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if (name.equals("setString") || name.equals("setInt")) {
                boundParameters.put((Integer) args[0], args[1]);
                return null;
            }
            if (name.equals("setNull")) {
                boundParameters.put((Integer) args[0], null);
                return null;
            }
            if (name.equals("executeUpdate")) {
                return executeUpdateReturnValue;
            }
            if (name.equals("close") || name.equals("clearParameters")) {
                return null;
            }
            if (name.equals("getGeneratedKeys")) {
                return Proxy.newProxyInstance(
                        ResultSet.class.getClassLoader(),
                        new Class<?>[]{ResultSet.class},
                        (p, m, a) -> {
                            if (m.getName().equals("next")) return true;
                            if (m.getName().equals("getInt")) return 42;
                            if (m.getName().equals("close")) return null;
                            return null;
                        }
                );
            }
            return null;
        }
    }

    static class ConnectionHandler implements InvocationHandler {
        String capturedSql;
        StatementHandler statementHandler = new StatementHandler();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getName().equals("prepareStatement")) {
                capturedSql = (String) args[0];
                return Proxy.newProxyInstance(
                        PreparedStatement.class.getClassLoader(),
                        new Class<?>[]{PreparedStatement.class},
                        statementHandler
                );
            }
            if (method.getName().equals("close")) {
                return null;
            }
            return null;
        }
    }

    private Connection createProxyConnection(ConnectionHandler handler) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                handler
        );
    }

    @Test
    @DisplayName("EmergencyCaseDAO.updateEmergencyCase preserves database priority when priority is null")
    void testUpdateEmergencyCase_PreservesPriority_WhenNull() throws Exception {
        EmergencyCaseDAO dao = new EmergencyCaseDAO();
        ConnectionHandler connectionHandler = new ConnectionHandler();
        Connection connection = createProxyConnection(connectionHandler);

        EmergencyCase updateCase = new EmergencyCase(
                1, 1, "Updated Location", "CARDIAC", "Updated symptoms", null, "REGISTERED"
        );

        boolean updated = dao.updateEmergencyCase(connection, updateCase);

        assertTrue(updated);
        // Verify SQL uses COALESCE(?, priority_level)
        String executedSql = connectionHandler.capturedSql;
        assertNotNull(executedSql);
        assertTrue(executedSql.contains("priority_level = COALESCE(?, priority_level)"),
                "SQL must use COALESCE to preserve existing priority: " + executedSql);

        // Verify parameter 5 is bound to null
        assertNull(connectionHandler.statementHandler.boundParameters.get(5),
                "Parameter 5 must be null so COALESCE retains database value");
        assertEquals(1, connectionHandler.statementHandler.boundParameters.get(1)); // patient_id
        assertEquals("Updated Location", connectionHandler.statementHandler.boundParameters.get(2)); // incident_location
        assertEquals("REGISTERED", connectionHandler.statementHandler.boundParameters.get(6)); // case_status
        assertEquals(1, connectionHandler.statementHandler.boundParameters.get(7)); // case_id
    }

    @Test
    @DisplayName("EmergencyCaseDAO.updateEmergencyCase preserves database priority when priority is blank")
    void testUpdateEmergencyCase_PreservesPriority_WhenBlank() throws Exception {
        EmergencyCaseDAO dao = new EmergencyCaseDAO();
        ConnectionHandler connectionHandler = new ConnectionHandler();
        Connection connection = createProxyConnection(connectionHandler);

        EmergencyCase updateCase = new EmergencyCase(
                1, 1, "Updated Location", "CARDIAC", "Updated symptoms", "   ", "REGISTERED"
        );

        boolean updated = dao.updateEmergencyCase(connection, updateCase);

        assertTrue(updated);
        assertNull(connectionHandler.statementHandler.boundParameters.get(5),
                "Parameter 5 must be null for blank priority so COALESCE retains database value");
    }

    @Test
    @DisplayName("EmergencyCaseDAO.updateEmergencyCase updates priority when explicit valid priority provided")
    void testUpdateEmergencyCase_UpdatesPriority_WhenExplicitlyProvided() throws Exception {
        EmergencyCaseDAO dao = new EmergencyCaseDAO();
        ConnectionHandler connectionHandler = new ConnectionHandler();
        Connection connection = createProxyConnection(connectionHandler);

        EmergencyCase updateCase = new EmergencyCase(
                1, 1, "Updated Location", "CARDIAC", "Updated symptoms", "  critical  ", "REGISTERED"
        );

        boolean updated = dao.updateEmergencyCase(connection, updateCase);

        assertTrue(updated);
        assertEquals("CRITICAL", connectionHandler.statementHandler.boundParameters.get(5),
                "Parameter 5 must be normalized to uppercase");
    }

    @Test
    @DisplayName("EmergencyCaseDAO.addEmergencyCase normalizes explicit priority on insert")
    void testAddEmergencyCase_NormalizesPriority_OnInsert() throws Exception {
        EmergencyCaseDAO dao = new EmergencyCaseDAO();
        ConnectionHandler connectionHandler = new ConnectionHandler();
        Connection connection = createProxyConnection(connectionHandler);

        EmergencyCase newCase = new EmergencyCase(
                0, 1, "Test Location", "CARDIAC", "Test Symptoms", "  high  ", "REGISTERED"
        );

        boolean added = dao.addEmergencyCase(connection, newCase);

        assertTrue(added);
        assertEquals(42, newCase.getCaseId());
        assertEquals("HIGH", connectionHandler.statementHandler.boundParameters.get(5));
    }

    @Test
    @DisplayName("EmergencyCaseDAO.addEmergencyCase defaults to MODERATE when priority is null on insert")
    void testAddEmergencyCase_DefaultsToModerate_WhenNullOnInsert() throws Exception {
        EmergencyCaseDAO dao = new EmergencyCaseDAO();
        ConnectionHandler connectionHandler = new ConnectionHandler();
        Connection connection = createProxyConnection(connectionHandler);

        EmergencyCase newCase = new EmergencyCase(
                0, 1, "Test Location", "CARDIAC", "Test Symptoms", null, "REGISTERED"
        );

        boolean added = dao.addEmergencyCase(connection, newCase);

        assertTrue(added);
        assertEquals(42, newCase.getCaseId());
        assertEquals("MODERATE", connectionHandler.statementHandler.boundParameters.get(5));
    }
}
