package com.abc.emergency;

import com.abc.emergency.model.Hospital;
import com.abc.emergency.service.HospitalService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HospitalUpdateTest {

    @Test
    void testHospitalUpdate() {

        HospitalService hospitalService =
                new HospitalService();

        Hospital hospital =
                hospitalService.getAllHospitals()
                        .stream()
                        .findFirst()
                        .orElse(null);

        assertNotNull(
                hospital,
                "No hospital found in database"
        );

        hospital.setIcuBeds(25);
        hospital.setEmergencyBeds(35);
        hospital.setVentilators(12);
        hospital.setTraumaAvailable(true);
        hospital.setCardioAvailable(true);
        hospital.setNeuroAvailable(true);
        hospital.setAvailability("AVAILABLE");

        boolean result =
                hospitalService.updateHospital(hospital);

        assertTrue(result);

        System.out.println(
                "Hospital Update Test: SUCCESS"
        );

        System.out.println(
                "Hospital ID: " + hospital.getHospitalId()
        );

        System.out.println(
                "Updated ICU Beds: " + hospital.getIcuBeds()
        );

        System.out.println(
                "Updated Emergency Beds: "
                        + hospital.getEmergencyBeds()
        );

        System.out.println(
                "Updated Ventilators: "
                        + hospital.getVentilators()
        );

        System.out.println(
                "Availability: "
                        + hospital.getAvailability()
        );
    }
}