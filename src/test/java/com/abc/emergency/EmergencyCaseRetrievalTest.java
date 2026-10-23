package com.abc.emergency;

import com.abc.emergency.model.EmergencyCase;
import com.abc.emergency.service.EmergencyCaseService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EmergencyCaseRetrievalTest {

    @Test
    void testEmergencyCaseRetrieval() {

        EmergencyCaseService service = new EmergencyCaseService();

        EmergencyCase emergencyCase = service.getEmergencyCaseById(1);

        assertNotNull(emergencyCase);
        assertEquals(1, emergencyCase.getCaseId());
        assertEquals(1, emergencyCase.getPatientId());

        System.out.println("Emergency Case Retrieval Test: SUCCESS");
        System.out.println("Case ID: " + emergencyCase.getCaseId());
        System.out.println("Patient ID: " + emergencyCase.getPatientId());
        System.out.println("Location: " + emergencyCase.getIncidentLocation());
        System.out.println("Emergency Type: " + emergencyCase.getEmergencyType());
        System.out.println("Status: " + emergencyCase.getCaseStatus());

        List<EmergencyCase> cases = service.getAllEmergencyCases();

        assertFalse(cases.isEmpty());

        System.out.println("Total Emergency Cases: " + cases.size());
    }
}