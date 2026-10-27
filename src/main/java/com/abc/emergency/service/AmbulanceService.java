package com.abc.emergency.service;

import com.abc.emergency.dao.AmbulanceCrewDAO;
import com.abc.emergency.dao.AmbulanceDAO;
import com.abc.emergency.dao.AmbulanceTrackingDAO;
import com.abc.emergency.dao.UserDAO;
import com.abc.emergency.model.Ambulance;
import com.abc.emergency.model.User;
import com.abc.emergency.util.ValidationUtil;

import java.util.Collections;
import java.util.List;

public class AmbulanceService {

    private final AmbulanceDAO ambulanceDAO;
    private final UserDAO userDAO;
    private final AmbulanceCrewDAO ambulanceCrewDAO;
    private final AmbulanceTrackingDAO ambulanceTrackingDAO;
    private final ThreadLocal<String> lastErrorMessage = new ThreadLocal<>();

    public AmbulanceService() {
        this(new AmbulanceDAO(), new UserDAO(), new AmbulanceCrewDAO(), new AmbulanceTrackingDAO());
    }

    public AmbulanceService(AmbulanceDAO ambulanceDAO, UserDAO userDAO) {
        this(ambulanceDAO, userDAO, new AmbulanceCrewDAO(), new AmbulanceTrackingDAO());
    }

    public AmbulanceService(AmbulanceDAO ambulanceDAO, UserDAO userDAO,
                            AmbulanceCrewDAO ambulanceCrewDAO, AmbulanceTrackingDAO ambulanceTrackingDAO) {
        this.ambulanceDAO = ambulanceDAO;
        this.userDAO = userDAO;
        this.ambulanceCrewDAO = ambulanceCrewDAO;
        this.ambulanceTrackingDAO = ambulanceTrackingDAO;
    }

    public boolean registerAmbulance(String vehicleNumber,
                                     int driverId,
                                     int paramedicId,
                                     double latitude,
                                     double longitude,
                                     String equipment) {

        if (vehicleNumber == null || vehicleNumber.trim().isEmpty()) {
            String err = "Vehicle number cannot be empty.";
            lastErrorMessage.set(err);
            System.out.println(err);
            return false;
        }

        if (!ValidationUtil.isValidId(driverId)) {
            String err = "Invalid driver ID.";
            lastErrorMessage.set(err);
            System.out.println(err);
            return false;
        }

        if (!ValidationUtil.isValidId(paramedicId)) {
            String err = "Invalid paramedic ID.";
            lastErrorMessage.set(err);
            System.out.println(err);
            return false;
        }

        if (!ValidationUtil.isValidCoordinates(latitude, longitude)) {
            String err = "Invalid GPS coordinates.";
            lastErrorMessage.set(err);
            System.out.println(err);
            return false;
        }

        if (equipment == null || equipment.trim().isEmpty()) {
            String err = "Equipment details cannot be empty.";
            lastErrorMessage.set(err);
            System.out.println(err);
            return false;
        }

        User driver = userDAO.getUserById(driverId);

        if (driver == null) {
            String err = "Driver not found.";
            lastErrorMessage.set(err);
            System.out.println(err);
            return false;
        }

        if (!"AMBULANCE_DRIVER".equalsIgnoreCase(driver.getRole())) {
            String err = "Selected user is not an ambulance driver.";
            lastErrorMessage.set(err);
            System.out.println(err);
            return false;
        }

        User paramedic = userDAO.getUserById(paramedicId);

        if (paramedic == null) {
            String err = "Paramedic not found.";
            lastErrorMessage.set(err);
            System.out.println(err);
            return false;
        }

        if (!"PARAMEDIC".equalsIgnoreCase(paramedic.getRole())) {
            String err = "Selected user is not a paramedic.";
            lastErrorMessage.set(err);
            System.out.println(err);
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
            lastErrorMessage.remove();
            System.out.println("Ambulance registered successfully.");
        } else {
            String err = "Ambulance registration failed.";
            lastErrorMessage.set(err);
            System.out.println(err);
        }

        return result;
    }

    public Ambulance getAmbulanceById(int ambulanceId) {

        if (!ValidationUtil.isValidId(ambulanceId)) {
            String err = "Invalid ambulance ID.";
            lastErrorMessage.set(err);
            System.out.println(err);
            return null;
        }

        return ambulanceDAO.getAmbulanceById(ambulanceId);
    }

    public List<Ambulance> getAllAmbulances() {
        return ambulanceDAO.getAllAmbulances();
    }

    public ServiceResult<Ambulance> getAmbulanceForDriver(int userId) {
        if (userId <= 0) {
            return ServiceResult.notFound("Driver user ID is invalid.");
        }

        // 1. Authoritative primary assignment: Ambulances.driver_id
        List<Ambulance> primaryAmbulances = ambulanceDAO.getAmbulancesByDriverId(userId);
        if (primaryAmbulances.size() > 1) {
            // Ambiguous driver assignment
            return ServiceResult.conflict("Ambiguous driver assignment: multiple ambulances assigned to user ID " + userId);
        }
        if (primaryAmbulances.size() == 1) {
            return ServiceResult.success("Assigned ambulance retrieved.", primaryAmbulances.get(0));
        }

        // 2. Fallback check on Ambulance_Crew if Ambulances.driver_id was unassigned
        List<Integer> crewAmbulanceIds = ambulanceCrewDAO.getActiveAmbulanceIdsForUser(userId);
        if (crewAmbulanceIds.isEmpty()) {
            return ServiceResult.notFound("No ambulance assigned to driver ID " + userId);
        }
        if (crewAmbulanceIds.size() > 1) {
            return ServiceResult.conflict("Ambiguous driver assignment: multiple active crew records for user ID " + userId);
        }

        int ambId = crewAmbulanceIds.get(0);
        Ambulance amb = ambulanceDAO.getAmbulanceById(ambId);
        if (amb == null) {
            return ServiceResult.notFound("Ambulance not found with ID: " + ambId);
        }

        // Policy 3: A stale Ambulance_Crew record must never grant access when Ambulances.driver_id identifies another driver!
        if (amb.getDriverId() > 0 && amb.getDriverId() != userId) {
            return ServiceResult.notFound("No active ambulance assignment for driver ID " + userId + " (stale crew record suppressed).");
        }

        return ServiceResult.success("Assigned ambulance retrieved.", amb);
    }

    public List<Ambulance> getAllAmbulancesForCaller(int userId, String role) {
        if ("AMBULANCE_DRIVER".equalsIgnoreCase(role)) {
            ServiceResult<Ambulance> driverAmb = getAmbulanceForDriver(userId);
            if (driverAmb.isSuccess()) {
                return List.of(driverAmb.getData());
            }
            return Collections.emptyList();
        }

        if ("HOSPITAL_ADMIN".equalsIgnoreCase(role)) {
            return Collections.emptyList();
        }

        return ambulanceDAO.getAllAmbulances();
    }

    public ServiceResult<Ambulance> getAmbulanceByIdForCaller(int ambulanceId, int userId, String role) {
        if (!ValidationUtil.isValidId(ambulanceId)) {
            return ServiceResult.validationError("Invalid ambulance ID. ID must be greater than 0.");
        }

        Ambulance ambulance = ambulanceDAO.getAmbulanceById(ambulanceId);
        if (ambulance == null) {
            return ServiceResult.notFound("Ambulance not found with ID: " + ambulanceId);
        }

        if ("AMBULANCE_DRIVER".equalsIgnoreCase(role)) {
            ServiceResult<Ambulance> driverAmb = getAmbulanceForDriver(userId);
            if (!driverAmb.isSuccess() || driverAmb.getData().getAmbulanceId() != ambulanceId) {
                // Deny access without leaking existence of other vehicles
                return ServiceResult.notFound("Ambulance not found with ID: " + ambulanceId);
            }
        }

        if ("HOSPITAL_ADMIN".equalsIgnoreCase(role)) {
            return ServiceResult.notFound("Ambulance not found with ID: " + ambulanceId);
        }

        return ServiceResult.success("Ambulance retrieved successfully.", ambulance);
    }

    public ServiceResult<Ambulance> updateAmbulanceLocationResult(int ambulanceId,
                                                                 Double latitude,
                                                                 Double longitude,
                                                                 Double speed,
                                                                 int callerUserId,
                                                                 String callerRole) {
        if (!ValidationUtil.isValidId(ambulanceId)) {
            String err = "Invalid ambulance ID. ID must be greater than 0.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        if (latitude == null || !Double.isFinite(latitude) || latitude < -90.0 || latitude > 90.0) {
            String err = "Invalid latitude. Must be a finite number between -90.0 and 90.0.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        if (longitude == null || !Double.isFinite(longitude) || longitude < -180.0 || longitude > 180.0) {
            String err = "Invalid longitude. Must be a finite number between -180.0 and 180.0.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        if (speed != null && (!Double.isFinite(speed) || speed < 0.0 || speed > 250.0)) {
            String err = "Invalid speed. Must be a finite number between 0.0 and 250.0 km/h.";
            lastErrorMessage.set(err);
            return ServiceResult.validationError(err);
        }

        Ambulance ambulance = ambulanceDAO.getAmbulanceById(ambulanceId);
        if (ambulance == null) {
            String err = "Ambulance not found with ID: " + ambulanceId;
            lastErrorMessage.set(err);
            return ServiceResult.notFound(err);
        }

        if ("AMBULANCE_DRIVER".equalsIgnoreCase(callerRole)) {
            ServiceResult<Ambulance> assignedResult = getAmbulanceForDriver(callerUserId);
            if (!assignedResult.isSuccess() || assignedResult.getData().getAmbulanceId() != ambulanceId) {
                String err = "Ambulance not found with ID: " + ambulanceId;
                lastErrorMessage.set(err);
                return ServiceResult.notFound(err);
            }
        }

        boolean updated = ambulanceDAO.updateLocation(ambulanceId, latitude, longitude);
        if (updated) {
            double recordedSpeed = (speed != null) ? speed : 0.0;
            ambulanceTrackingDAO.addTracking(ambulanceId, latitude, longitude, recordedSpeed, ambulance.getAvailability());

            ambulance.setGpsLatitude(latitude);
            ambulance.setGpsLongitude(longitude);
            lastErrorMessage.remove();
            return ServiceResult.success("Ambulance location updated successfully.", ambulance);
        } else {
            String err = "DATABASE_ERROR: Unable to update ambulance location.";
            lastErrorMessage.set(err);
            return ServiceResult.databaseError(err);
        }
    }

    public boolean updateAmbulance(Ambulance ambulance) {

        if (ambulance == null
                || !ValidationUtil.isValidId(ambulance.getAmbulanceId())) {
            String err = "Invalid ambulance information.";
            lastErrorMessage.set(err);
            System.out.println(err);
            return false;
        }

        if (ambulance.getVehicleNumber() == null
                || ambulance.getVehicleNumber().trim().isEmpty()) {
            String err = "Vehicle number cannot be empty.";
            lastErrorMessage.set(err);
            System.out.println(err);
            return false;
        }

        if (!ValidationUtil.isValidCoordinates(
                ambulance.getGpsLatitude(),
                ambulance.getGpsLongitude())) {
            String err = "Invalid GPS coordinates.";
            lastErrorMessage.set(err);
            System.out.println(err);
            return false;
        }

        return ambulanceDAO.updateAmbulance(ambulance);
    }

    public boolean updateAvailability(int ambulanceId, String availability) {

        if (!ValidationUtil.isValidId(ambulanceId)) {
            String err = "Invalid ambulance ID.";
            lastErrorMessage.set(err);
            System.out.println(err);
            return false;
        }

        if (availability == null || availability.trim().isEmpty()) {
            String err = "Availability cannot be empty.";
            lastErrorMessage.set(err);
            System.out.println(err);
            return false;
        }

        String status = availability.toUpperCase();

        if (!status.equals("AVAILABLE")
                && !status.equals("DISPATCHED")
                && !status.equals("MAINTENANCE")) {
            String err = "Invalid ambulance availability.";
            lastErrorMessage.set(err);
            System.out.println(err);
            return false;
        }

        return ambulanceDAO.updateAvailability(ambulanceId, status);
    }

    public String getLastErrorMessage() {
        return lastErrorMessage.get();
    }
}
