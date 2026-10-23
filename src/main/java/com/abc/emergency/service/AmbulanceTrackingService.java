package com.abc.emergency.service;

import com.abc.emergency.dao.AmbulanceTrackingDAO;
import com.abc.emergency.util.ValidationUtil;

public class AmbulanceTrackingService {

    private final AmbulanceTrackingDAO trackingDAO;

    public AmbulanceTrackingService() {
        trackingDAO = new AmbulanceTrackingDAO();
    }

    public boolean addTracking(
            int ambulanceId,
            double latitude,
            double longitude,
            double speed,
            String status) {

        if (!ValidationUtil.isValidId(ambulanceId)) {
            System.out.println("Invalid ambulance ID.");
            return false;
        }

        if (!ValidationUtil.isValidCoordinates(latitude, longitude)) {
            System.out.println("Invalid GPS coordinates.");
            return false;
        }

        if (speed < 0) {
            System.out.println("Speed cannot be negative.");
            return false;
        }

        if (status == null || status.trim().isEmpty()) {
            System.out.println("Tracking status cannot be empty.");
            return false;
        }

        return trackingDAO.addTracking(
                ambulanceId,
                latitude,
                longitude,
                speed,
                status.toUpperCase()
        );
    }

    public boolean updateAmbulanceLocation(
            int ambulanceId,
            double latitude,
            double longitude) {

        if (!ValidationUtil.isValidId(ambulanceId)) {
            System.out.println("Invalid ambulance ID.");
            return false;
        }

        if (!ValidationUtil.isValidCoordinates(latitude, longitude)) {
            System.out.println("Invalid GPS coordinates.");
            return false;
        }

        return trackingDAO.updateAmbulanceLocation(
                ambulanceId,
                latitude,
                longitude
        );
    }

    public void getLatestLocation(int ambulanceId) {

        if (!ValidationUtil.isValidId(ambulanceId)) {
            System.out.println("Invalid ambulance ID.");
            return;
        }

        trackingDAO.getLatestLocation(ambulanceId);
    }

    public void getTrackingHistory(int ambulanceId) {

        if (!ValidationUtil.isValidId(ambulanceId)) {
            System.out.println("Invalid ambulance ID.");
            return;
        }

        trackingDAO.getTrackingHistory(ambulanceId);
    }

    public boolean deleteTracking(int trackingId) {

        if (!ValidationUtil.isValidId(trackingId)) {
            System.out.println("Invalid tracking ID.");
            return false;
        }

        return trackingDAO.deleteTracking(trackingId);
    }
}
