package com.abc.emergency;

import com.abc.emergency.model.Hospital;
import com.abc.emergency.service.HospitalService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HospitalDeleteTest {

    @Test
    void testHospitalDelete() {

        HospitalService hospitalService = new HospitalService();

        boolean registered = hospitalService.registerHospital(
                "Temporary Test Hospital",
                "Bengaluru Test Location",
                5,
                10,
                2,
                false,
                false,
                false
        );

        assertTrue(registered);

        Hospital temporaryHospital = null;

        for (Hospital hospital : hospitalService.getAllHospitals()) {
            if ("Temporary Test Hospital".equals(hospital.getHospitalName())) {
                temporaryHospital = hospital;
                break;
            }
        }

        assertTrue(temporaryHospital != null);

        int hospitalId = temporaryHospital.getHospitalId();

        boolean deleted = hospitalService.deleteHospital(hospitalId);

        assertTrue(deleted);

        Hospital deletedHospital =
                hospitalService.getHospitalById(hospitalId);

        assertFalse(deletedHospital != null);

        System.out.println("Hospital Delete Test: SUCCESS");
        System.out.println("Deleted Hospital ID: " + hospitalId);
        System.out.println("Deleted Hospital Name: Temporary Test Hospital");
    }
}
