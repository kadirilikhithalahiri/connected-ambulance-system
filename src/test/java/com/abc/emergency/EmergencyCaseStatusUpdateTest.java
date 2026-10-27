package com.abc.emergency;

import com.abc.emergency.dao.EmergencyCaseDAO;
import com.abc.emergency.dao.PatientDAO;
import com.abc.emergency.model.EmergencyCase;
import com.abc.emergency.service.EmergencyCaseService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EmergencyCaseStatusUpdateTest {

    @Test
    void testEmergencyCaseStatusUpdate() {

        EmergencyCaseService service = new EmergencyCaseService();

        // 1. Direct transition from REGISTERED to DISPATCHED must be rejected:
        // Ambulance dispatching must occur through the formal DispatchService workflow.
        boolean dispatchResult = service.updateCaseStatus(1, "DISPATCHED");
        assertFalse(dispatchResult, "Direct REGISTERED -> DISPATCHED case status update must be rejected.");
        assertNotNull(service.getLastErrorMessage());
        assertTrue(service.getLastErrorMessage().contains("Invalid status transition"));

        // Verify Case 1 status remains unchanged in database
        EmergencyCase case1 = service.getEmergencyCaseById(1);
        assertNotNull(case1);
        assertEquals("REGISTERED", case1.getCaseStatus());

        // 2. Valid transition (REGISTERED -> CANCELLED) using an isolated stub
        // to verify atomic status updating without corrupting the live database baseline
        EmergencyCaseDAO stubDAO = new EmergencyCaseDAO() {
            private String currentStatus = "REGISTERED";

            @Override
            public EmergencyCase getEmergencyCaseById(int caseId) {
                EmergencyCase ec = new EmergencyCase();
                ec.setCaseId(caseId);
                ec.setCaseStatus(currentStatus);
                return ec;
            }

            @Override
            public boolean updateCaseStatusAtomic(int caseId, String expectedStatus, String targetStatus) {
                if (currentStatus.equals(expectedStatus)) {
                    currentStatus = targetStatus;
                    return true;
                }
                return false;
            }
        };

        EmergencyCaseService isolatedService = new EmergencyCaseService(stubDAO, new PatientDAO());
        boolean cancelResult = isolatedService.updateCaseStatus(1, "CANCELLED");
        assertTrue(cancelResult, "REGISTERED -> CANCELLED transition must succeed.");

        EmergencyCase emergencyCase = isolatedService.getEmergencyCaseById(1);
        assertNotNull(emergencyCase);
        assertEquals("CANCELLED", emergencyCase.getCaseStatus());

        System.out.println("Emergency Case Status Update Test: SUCCESS");
        System.out.println("Case ID: " + emergencyCase.getCaseId());
        System.out.println("Updated Status: " + emergencyCase.getCaseStatus());
    }
}
