package com.abc.emergency.service;

import com.abc.emergency.dao.HospitalFacilityDAO;
import com.abc.emergency.util.ValidationUtil;

import java.util.List;

public class HospitalFacilityService {

    private final HospitalFacilityDAO facilityDAO;

    public HospitalFacilityService() {
        facilityDAO = new HospitalFacilityDAO();
    }

    public boolean addFacility(
            int hospitalId,
            String facilityName,
            int totalUnits,
            int availableUnits,
            String status) {

        if (!ValidationUtil.isValidId(hospitalId)) {
            System.out.println("Invalid hospital ID.");
            return false;
        }

        if (facilityName == null || facilityName.trim().isEmpty()) {
            System.out.println("Facility name cannot be empty.");
            return false;
        }

        if (totalUnits < 0 || availableUnits < 0) {
            System.out.println("Facility units cannot be negative.");
            return false;
        }

        if (availableUnits > totalUnits) {
            System.out.println("Available units cannot exceed total units.");
            return false;
        }

        if (!isValidStatus(status)) {
            System.out.println("Invalid facility status.");
            return false;
        }

        return facilityDAO.addFacility(
                hospitalId,
                facilityName,
                totalUnits,
                availableUnits,
                status.toUpperCase()
        );
    }

    public List<String> getFacilitiesByHospital(int hospitalId) {

        if (!ValidationUtil.isValidId(hospitalId)) {
            System.out.println("Invalid hospital ID.");
            return List.of();
        }

        return facilityDAO.getFacilitiesByHospital(hospitalId);
    }

    public boolean updateFacility(
            int facilityId,
            String facilityName,
            int totalUnits,
            int availableUnits,
            String status) {

        if (!ValidationUtil.isValidId(facilityId)) {
            System.out.println("Invalid facility ID.");
            return false;
        }

        if (facilityName == null || facilityName.trim().isEmpty()) {
            System.out.println("Facility name cannot be empty.");
            return false;
        }

        if (totalUnits < 0 || availableUnits < 0) {
            System.out.println("Facility units cannot be negative.");
            return false;
        }

        if (availableUnits > totalUnits) {
            System.out.println("Available units cannot exceed total units.");
            return false;
        }

        if (!isValidStatus(status)) {
            System.out.println("Invalid facility status.");
            return false;
        }

        return facilityDAO.updateFacility(
                facilityId,
                facilityName,
                totalUnits,
                availableUnits,
                status.toUpperCase()
        );
    }

    public boolean updateFacilityStatus(int facilityId, String status) {

        if (!ValidationUtil.isValidId(facilityId)) {
            System.out.println("Invalid facility ID.");
            return false;
        }

        if (!isValidStatus(status)) {
            System.out.println("Invalid facility status.");
            return false;
        }

        return facilityDAO.updateFacilityStatus(
                facilityId,
                status.toUpperCase()
        );
    }

    public boolean deleteFacility(int facilityId) {

        if (!ValidationUtil.isValidId(facilityId)) {
            System.out.println("Invalid facility ID.");
            return false;
        }

        return facilityDAO.deleteFacility(facilityId);
    }

    private boolean isValidStatus(String status) {

        if (status == null) {
            return false;
        }

        String value = status.toUpperCase();

        return value.equals("AVAILABLE")
                || value.equals("LIMITED")
                || value.equals("FULL");
    }
}
