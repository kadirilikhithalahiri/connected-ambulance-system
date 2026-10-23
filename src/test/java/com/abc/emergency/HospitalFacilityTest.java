package com.abc.emergency;

import com.abc.emergency.model.Hospital;
import com.abc.emergency.service.HospitalFacilityService;
import com.abc.emergency.service.HospitalService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HospitalFacilityTest {

    @Test
    void testHospitalFacilityManagement() {

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

        int hospitalId =
                hospital.getHospitalId();

        HospitalFacilityService facilityService =
                new HospitalFacilityService();

        boolean added = facilityService.addFacility(
                hospitalId,
                "Emergency ICU",
                20,
                15,
                "AVAILABLE"
        );

        assertTrue(added);

        List<String> facilities =
                facilityService.getFacilitiesByHospital(
                        hospitalId
                );

        assertFalse(facilities.isEmpty());

        System.out.println(
                "Hospital Facility Add Test: SUCCESS"
        );

        System.out.println(
                "Hospital ID: " + hospitalId
        );

        System.out.println(
                "Facility: Emergency ICU"
        );

        String facility =
                facilities.get(facilities.size() - 1);

        int facilityId =
                Integer.parseInt(
                        facility.split(" - ")[0]
                );

        boolean updated =
                facilityService.updateFacility(
                        facilityId,
                        "Emergency ICU",
                        20,
                        10,
                        "LIMITED"
                );

        assertTrue(updated);

        boolean statusUpdated =
                facilityService.updateFacilityStatus(
                        facilityId,
                        "AVAILABLE"
                );

        assertTrue(statusUpdated);

        boolean deleted =
                facilityService.deleteFacility(
                        facilityId
                );

        assertTrue(deleted);

        System.out.println(
                "Hospital Facility Update Test: SUCCESS"
        );

        System.out.println(
                "Hospital Facility Status Test: SUCCESS"
        );

        System.out.println(
                "Hospital Facility Delete Test: SUCCESS"
        );

        System.out.println(
                "Facility ID: " + facilityId
        );
    }
}