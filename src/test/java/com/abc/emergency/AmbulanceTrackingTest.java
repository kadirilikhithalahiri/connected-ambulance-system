package com.abc.emergency;

import com.abc.emergency.dao.AmbulanceTrackingDAO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AmbulanceTrackingTest {

    @Test
    void testAmbulanceTracking() {

        AmbulanceTrackingDAO trackingDAO = new AmbulanceTrackingDAO();

        boolean result = trackingDAO.addTracking(
                1,
                12.8456,
                77.6603,
                45.5,
                "EN_ROUTE"
        );

        assertTrue(result);

        System.out.println("Ambulance Tracking Test: SUCCESS");
        System.out.println("Ambulance ID: 1");
        System.out.println("Latitude: 12.8456");
        System.out.println("Longitude: 77.6603");
        System.out.println("Speed: 45.5 km/h");
        System.out.println("Status: EN_ROUTE");
    }
}
