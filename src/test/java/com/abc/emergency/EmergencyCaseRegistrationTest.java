package com.abc.emergency;

import com.abc.emergency.service.EmergencyCaseService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class EmergencyCaseRegistrationTest {

    @Test
    void testEmergencyCaseRegistration() {

        EmergencyCaseService service = new EmergencyCaseService();

        boolean result = service.registerEmergencyCase(
                1,
                "Electronic City, Bengaluru",
                "Breathing Difficulty",
                "Shortness of breath and chest discomfort"
        );

        System.out.println("Emergency Case Registration Test: "
                + (result ? "SUCCESS" : "FAILED"));

        assertTrue(result);
    }
}