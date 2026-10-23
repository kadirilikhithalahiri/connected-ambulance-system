package com.abc.emergency;

import com.abc.emergency.model.Hospital;
import com.abc.emergency.service.HospitalService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HospitalAvailabilityTest {

    @Test
    void testHospitalAvailabilityUpdate() {

        HospitalService hospitalService = new HospitalService();

        Hospital hospital =
                hospitalService.getAllHospitals()
                        .stream()
                        .findFirst()
                        .orElse(null);

        assertNotNull(
                hospital,
                "No hospital found in database"
        );

        int hospitalId =
                hospital.getHospitalId();

        boolean limitedResult =
                hospitalService.updateAvailability(
                        hospitalId,
                        "LIMITED"
                );

        assertTrue(limitedResult);

        boolean availableResult =
                hospitalService.updateAvailability(
                        hospitalId,
                        "AVAILABLE"
                );

        assertTrue(availableResult);

        System.out.println(
                "Hospital Availability Test: SUCCESS"
        );

        System.out.println(
                "Hospital ID: " + hospitalId
        );

        System.out.println(
                "Availability changed: AVAILABLE -> LIMITED -> AVAILABLE"
        );
    }
}