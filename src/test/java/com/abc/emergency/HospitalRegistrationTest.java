package com.abc.emergency;

import com.abc.emergency.service.HospitalService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class HospitalRegistrationTest {

    @Test
    void testHospitalRegistration() {

        HospitalService hospitalService = new HospitalService();

        boolean result = hospitalService.registerHospital(
                "ABC Emergency Hospital",
                "Electronic City, Bengaluru",
                20,
                30,
                10,
                true,
                true,
                true
        );

        assertTrue(result);

        System.out.println("Hospital Registration Test: SUCCESS");
        System.out.println("Hospital Name: ABC Emergency Hospital");
        System.out.println("Location: Electronic City, Bengaluru");
        System.out.println("ICU Beds: 20");
        System.out.println("Emergency Beds: 30");
        System.out.println("Ventilators: 10");
    }
}
