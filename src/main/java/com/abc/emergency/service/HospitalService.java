package com.abc.emergency.service;

import com.abc.emergency.dao.HospitalDAO;
import com.abc.emergency.model.Hospital;
import com.abc.emergency.util.ValidationUtil;

import java.util.List;

public class HospitalService {

    private final HospitalDAO hospitalDAO;

    public HospitalService() {
        hospitalDAO = new HospitalDAO();
    }

    public boolean registerHospital(
            String hospitalName,
            String location,
            int icuBeds,
            int emergencyBeds,
            int ventilators,
            boolean traumaAvailable,
            boolean cardioAvailable,
            boolean neuroAvailable) {

        if (hospitalName == null || hospitalName.trim().isEmpty()) {
            System.out.println("Hospital name cannot be empty.");
            return false;
        }

        if (location == null || location.trim().isEmpty()) {
            System.out.println("Hospital location cannot be empty.");
            return false;
        }

        if (icuBeds < 0 || emergencyBeds < 0 || ventilators < 0) {
            System.out.println("Hospital capacity values cannot be negative.");
            return false;
        }

        Hospital hospital = new Hospital();

        hospital.setHospitalName(hospitalName);
        hospital.setLocation(location);
        hospital.setIcuBeds(icuBeds);
        hospital.setEmergencyBeds(emergencyBeds);
        hospital.setVentilators(ventilators);
        hospital.setTraumaAvailable(traumaAvailable);
        hospital.setCardioAvailable(cardioAvailable);
        hospital.setNeuroAvailable(neuroAvailable);
        hospital.setAvailability("AVAILABLE");

        boolean result = hospitalDAO.addHospital(hospital);

        if (result) {
            System.out.println("Hospital registered successfully.");
        } else {
            System.out.println("Hospital registration failed.");
        }

        return result;
    }

    public Hospital getHospitalById(int hospitalId) {

        if (!ValidationUtil.isValidId(hospitalId)) {
            System.out.println("Invalid hospital ID.");
            return null;
        }

        return hospitalDAO.getHospitalById(hospitalId);
    }

    public List<Hospital> getAllHospitals() {

        return hospitalDAO.getAllHospitals();
    }

    public boolean updateHospital(Hospital hospital) {

        if (hospital == null) {
            System.out.println("Hospital information cannot be null.");
            return false;
        }

        if (!ValidationUtil.isValidId(hospital.getHospitalId())) {
            System.out.println("Invalid hospital ID.");
            return false;
        }

        return hospitalDAO.updateHospital(hospital);
    }

    public boolean updateAvailability(int hospitalId, String availability) {

        if (!ValidationUtil.isValidId(hospitalId)) {
            System.out.println("Invalid hospital ID.");
            return false;
        }

        if (availability == null || availability.trim().isEmpty()) {
            System.out.println("Hospital availability cannot be empty.");
            return false;
        }

        String status = availability.toUpperCase();

        if (!status.equals("AVAILABLE")
                && !status.equals("LIMITED")
                && !status.equals("FULL")) {

            System.out.println("Invalid hospital availability.");
            return false;
        }

        return hospitalDAO.updateAvailability(hospitalId, status);
    }

    public boolean deleteHospital(int hospitalId) {

        if (!ValidationUtil.isValidId(hospitalId)) {
            System.out.println("Invalid hospital ID.");
            return false;
        }

        return hospitalDAO.deleteHospital(hospitalId);
    }
}
