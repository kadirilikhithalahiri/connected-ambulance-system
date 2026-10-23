package com.abc.emergency;

import com.abc.emergency.model.Ambulance;
import com.abc.emergency.service.AmbulanceService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AmbulanceAvailabilityTest {

    @Test
    void testAmbulanceAvailabilityUpdate() {

        AmbulanceService ambulanceService = new AmbulanceService();

        boolean dispatched = ambulanceService.updateAvailability(1, "DISPATCHED");

        assertTrue(dispatched);

        Ambulance ambulance = ambulanceService.getAmbulanceById(1);

        assertNotNull(ambulance);
        assertEquals("DISPATCHED", ambulance.getAvailability());

        boolean available = ambulanceService.updateAvailability(1, "AVAILABLE");

        assertTrue(available);

        ambulance = ambulanceService.getAmbulanceById(1);

        assertNotNull(ambulance);
        assertEquals("AVAILABLE", ambulance.getAvailability());

        System.out.println("Ambulance Availability Test: SUCCESS");
        System.out.println("Ambulance ID: " + ambulance.getAmbulanceId());
        System.out.println("Final Availability: " + ambulance.getAvailability());
    }
}
