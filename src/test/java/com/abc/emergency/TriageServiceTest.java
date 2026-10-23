package com.abc.emergency;

import com.abc.emergency.model.AITriage;
import com.abc.emergency.model.PatientVital;
import com.abc.emergency.service.TriageService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TriageServiceTest {

    @Test
    void testCriticalPatient() {

        PatientVital vital = new PatientVital();

        vital.setHeartRate(160);
        vital.setSystolicBp(70);
        vital.setDiastolicBp(45);
        vital.setSpo2(85);
        vital.setTemperature(40.5);
        vital.setRespiratoryRate(35);

        TriageService service = new TriageService();

        AITriage result = service.analyzeVitals(1, vital);

        assertNotNull(result);
        assertEquals("CRITICAL", result.getPriorityLevel());
        assertEquals("P1", result.getPriorityCode());
    }

    @Test
    void testHighPriorityPatient() {

        PatientVital vital = new PatientVital();

        vital.setHeartRate(125);
        vital.setSystolicBp(88);
        vital.setDiastolicBp(55);
        vital.setSpo2(92);
        vital.setTemperature(38.5);
        vital.setRespiratoryRate(26);

        TriageService service = new TriageService();

        AITriage result = service.analyzeVitals(2, vital);

        assertNotNull(result);
        assertEquals("HIGH", result.getPriorityLevel());
        assertEquals("P2", result.getPriorityCode());
    }

    @Test
    void testModeratePriorityPatient() {

        PatientVital vital = new PatientVital();

        vital.setHeartRate(105);
        vital.setSystolicBp(95);
        vital.setDiastolicBp(65);
        vital.setSpo2(95);
        vital.setTemperature(38.2);
        vital.setRespiratoryRate(22);

        TriageService service = new TriageService();

        AITriage result = service.analyzeVitals(3, vital);

        assertNotNull(result);
        assertEquals("MODERATE", result.getPriorityLevel());
        assertEquals("P3", result.getPriorityCode());
    }

    @Test
    void testLowPriorityPatient() {

        PatientVital vital = new PatientVital();

        vital.setHeartRate(75);
        vital.setSystolicBp(120);
        vital.setDiastolicBp(80);
        vital.setSpo2(98);
        vital.setTemperature(36.8);
        vital.setRespiratoryRate(16);

        TriageService service = new TriageService();

        AITriage result = service.analyzeVitals(4, vital);

        assertNotNull(result);
        assertEquals("LOW", result.getPriorityLevel());
        assertEquals("P4", result.getPriorityCode());
    }
}