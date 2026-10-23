package com.abc.emergency;

import com.abc.emergency.model.Hospital;
import com.abc.emergency.service.HospitalService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class HospitalRetrievalTest {

    @Test
    void testHospitalRetrieval() {

        HospitalService hospitalService = new HospitalService();

        List<Hospital> hospitals = hospitalService.getAllHospitals();

        assertNotNull(hospitals);
        assertFalse(hospitals.isEmpty());

        Hospital hospital = hospitals.get(0);

        assertNotNull(hospital);
        assertNotNull(hospital.getHospitalName());

        System.out.println("Hospital Retrieval Test: SUCCESS");
        System.out.println("Hospital ID: " + hospital.getHospitalId());
        System.out.println("Hospital Name: " + hospital.getHospitalName());
        System.out.println("Location: " + hospital.getLocation());
        System.out.println("Availability: " + hospital.getAvailability());
        System.out.println("Total Hospitals: " + hospitals.size());
    }
}
