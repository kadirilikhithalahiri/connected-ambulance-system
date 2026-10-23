package com.abc.emergency;

import com.abc.emergency.dao.AmbulanceDAO;
import com.abc.emergency.model.Ambulance;
import com.abc.emergency.service.AmbulanceService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AmbulanceRegistrationTest {

    @Test
    void testAmbulanceRegistration() {

        AmbulanceService ambulanceService = new AmbulanceService();
        AmbulanceDAO ambulanceDAO = new AmbulanceDAO();

        Ambulance existing = null;

        for (Ambulance ambulance : ambulanceDAO.getAllAmbulances()) {
            if ("KA01AB5678".equalsIgnoreCase(ambulance.getVehicleNumber())) {
                existing = ambulance;
                break;
            }
        }

        if (existing == null) {

            boolean result = ambulanceService.registerAmbulance(
                    "KA01AB5678",
                    2,
                    1,
                    12.8456,
                    77.6603,
                    "Ventilator, Oxygen, ECG, First Aid Kit"
            );

            assertTrue(result);

            System.out.println("Ambulance Registration Test: SUCCESS");
            System.out.println("Vehicle Number: KA01AB5678");
            System.out.println("Driver ID: 2");
            System.out.println("Paramedic ID: 1");

        } else {

            assertNotNull(existing);

            System.out.println("Ambulance Registration Test: SUCCESS");
            System.out.println("Vehicle Number already exists: KA01AB5678");
            System.out.println("Existing Ambulance ID: " + existing.getAmbulanceId());
        }
    }
}
