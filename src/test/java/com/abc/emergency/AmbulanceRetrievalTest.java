package com.abc.emergency;

import com.abc.emergency.model.Ambulance;
import com.abc.emergency.service.AmbulanceService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AmbulanceRetrievalTest {

    @Test
    void testAmbulanceRetrieval() {

        AmbulanceService ambulanceService = new AmbulanceService();

        Ambulance ambulance = ambulanceService.getAmbulanceById(1);

        assertNotNull(ambulance);
        assertEquals(1, ambulance.getAmbulanceId());
        assertEquals("KA01AB1234", ambulance.getVehicleNumber());

        List<Ambulance> ambulances = ambulanceService.getAllAmbulances();

        assertFalse(ambulances.isEmpty());

        System.out.println("Ambulance Retrieval Test: SUCCESS");
        System.out.println("Ambulance ID: " + ambulance.getAmbulanceId());
        System.out.println("Vehicle Number: " + ambulance.getVehicleNumber());
        System.out.println("Availability: " + ambulance.getAvailability());
        System.out.println("Total Ambulances: " + ambulances.size());
    }
}
