package com.abc.emergency;

import com.abc.emergency.model.EmergencyCase;
import com.abc.emergency.service.EmergencyCaseService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EmergencyCaseStatusUpdateTest {

    @Test
    void testEmergencyCaseStatusUpdate() {

        EmergencyCaseService service = new EmergencyCaseService();

        boolean result = service.updateCaseStatus(1, "DISPATCHED");

        assertTrue(result);

        EmergencyCase emergencyCase = service.getEmergencyCaseById(1);

        assertNotNull(emergencyCase);
        assertEquals("DISPATCHED", emergencyCase.getCaseStatus());

        System.out.println("Emergency Case Status Update Test: SUCCESS");
        System.out.println("Case ID: " + emergencyCase.getCaseId());
        System.out.println("Updated Status: " + emergencyCase.getCaseStatus());
    }
}