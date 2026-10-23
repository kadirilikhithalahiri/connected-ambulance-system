package com.abc.emergency.service;

import com.abc.emergency.dao.AmbulanceDAO;
import com.abc.emergency.dao.UserDAO;
import com.abc.emergency.model.Ambulance;
import com.abc.emergency.model.User;
import com.abc.emergency.util.ValidationUtil;

import java.util.List;

public class AmbulanceService {

    private final AmbulanceDAO ambulanceDAO;
    private final UserDAO userDAO;

    public AmbulanceService() {
        ambulanceDAO = new AmbulanceDAO();
        userDAO = new UserDAO();
    }

    public boolean registerAmbulance(String vehicleNumber,
                                     int driverId,
                                     int paramedicId,
                                     double latitude,
                                     double longitude,
                                     String equipment) {

        if (vehicleNumber == null || vehicleNumber.trim().isEmpty()) {
            System.out.println("Vehicle number cannot be empty.");
            return false;
        }

        if (!ValidationUtil.isValidId(driverId)) {
            System.out.println("Invalid driver ID.");
            return false;
        }

        if (!ValidationUtil.isValidId(paramedicId)) {
            System.out.println("Invalid paramedic ID.");
            return false;
        }

        if (!ValidationUtil.isValidCoordinates(latitude, longitude)) {
            System.out.println("Invalid GPS coordinates.");
            return false;
        }

        if (equipment == null || equipment.trim().isEmpty()) {
            System.out.println("Equipment details cannot be empty.");
            return false;
        }

        User driver = userDAO.getUserById(driverId);

        if (driver == null) {
            System.out.println("Driver not found.");
            return false;
        }

        if (!"AMBULANCE_DRIVER".equalsIgnoreCase(driver.getRole())) {
            System.out.println("Selected user is not an ambulance driver.");
            return false;
        }

        User paramedic = userDAO.getUserById(paramedicId);

        if (paramedic == null) {
            System.out.println("Paramedic not found.");
            return false;
        }

        if (!"PARAMEDIC".equalsIgnoreCase(paramedic.getRole())) {
            System.out.println("Selected user is not a paramedic.");
            return false;
        }

        Ambulance ambulance = new Ambulance();

        ambulance.setVehicleNumber(vehicleNumber);
        ambulance.setDriverId(driverId);
        ambulance.setParamedicId(paramedicId);
        ambulance.setGpsLatitude(latitude);
        ambulance.setGpsLongitude(longitude);
        ambulance.setEquipment(equipment);
        ambulance.setAvailability("AVAILABLE");

        boolean result = ambulanceDAO.addAmbulance(ambulance);

        if (result) {
            System.out.println("Ambulance registered successfully.");
        } else {
            System.out.println("Ambulance registration failed.");
        }

        return result;
    }

    public Ambulance getAmbulanceById(int ambulanceId) {

        if (!ValidationUtil.isValidId(ambulanceId)) {
            System.out.println("Invalid ambulance ID.");
            return null;
        }

        return ambulanceDAO.getAmbulanceById(ambulanceId);
    }

    public List<Ambulance> getAllAmbulances() {
        return ambulanceDAO.getAllAmbulances();
    }

    public boolean updateAmbulance(Ambulance ambulance) {

        if (ambulance == null
                || !ValidationUtil.isValidId(ambulance.getAmbulanceId())) {
            System.out.println("Invalid ambulance information.");
            return false;
        }

        if (ambulance.getVehicleNumber() == null
                || ambulance.getVehicleNumber().trim().isEmpty()) {
            System.out.println("Vehicle number cannot be empty.");
            return false;
        }

        if (!ValidationUtil.isValidCoordinates(
                ambulance.getGpsLatitude(),
                ambulance.getGpsLongitude())) {
            System.out.println("Invalid GPS coordinates.");
            return false;
        }

        return ambulanceDAO.updateAmbulance(ambulance);
    }

    public boolean updateAvailability(int ambulanceId, String availability) {

        if (!ValidationUtil.isValidId(ambulanceId)) {
            System.out.println("Invalid ambulance ID.");
            return false;
        }

        if (availability == null || availability.trim().isEmpty()) {
            System.out.println("Availability cannot be empty.");
            return false;
        }

        String status = availability.toUpperCase();

        if (!status.equals("AVAILABLE")
                && !status.equals("DISPATCHED")
                && !status.equals("MAINTENANCE")) {
            System.out.println("Invalid ambulance availability.");
            return false;
        }

        return ambulanceDAO.updateAvailability(ambulanceId, status);
    }
}