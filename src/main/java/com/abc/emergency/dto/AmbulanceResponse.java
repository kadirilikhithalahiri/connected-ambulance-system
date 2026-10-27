package com.abc.emergency.dto;

import com.abc.emergency.model.Ambulance;

public class AmbulanceResponse {

    private int ambulanceId;
    private String vehicleNumber;
    private int driverId;
    private int paramedicId;
    private double gpsLatitude;
    private double gpsLongitude;
    private String equipment;
    private String availability;

    public AmbulanceResponse() {
    }

    public AmbulanceResponse(int ambulanceId, String vehicleNumber, int driverId, int paramedicId,
                             double gpsLatitude, double gpsLongitude, String equipment, String availability) {
        this.ambulanceId = ambulanceId;
        this.vehicleNumber = vehicleNumber;
        this.driverId = driverId;
        this.paramedicId = paramedicId;
        this.gpsLatitude = gpsLatitude;
        this.gpsLongitude = gpsLongitude;
        this.equipment = equipment;
        this.availability = availability;
    }

    public static AmbulanceResponse from(Ambulance ambulance) {
        if (ambulance == null) {
            return null;
        }
        return new AmbulanceResponse(
                ambulance.getAmbulanceId(),
                ambulance.getVehicleNumber(),
                ambulance.getDriverId(),
                ambulance.getParamedicId(),
                ambulance.getGpsLatitude(),
                ambulance.getGpsLongitude(),
                ambulance.getEquipment(),
                ambulance.getAvailability()
        );
    }

    public int getAmbulanceId() {
        return ambulanceId;
    }

    public void setAmbulanceId(int ambulanceId) {
        this.ambulanceId = ambulanceId;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public int getDriverId() {
        return driverId;
    }

    public void setDriverId(int driverId) {
        this.driverId = driverId;
    }

    public int getParamedicId() {
        return paramedicId;
    }

    public void setParamedicId(int paramedicId) {
        this.paramedicId = paramedicId;
    }

    public double getGpsLatitude() {
        return gpsLatitude;
    }

    public void setGpsLatitude(double gpsLatitude) {
        this.gpsLatitude = gpsLatitude;
    }

    public double getGpsLongitude() {
        return gpsLongitude;
    }

    public void setGpsLongitude(double gpsLongitude) {
        this.gpsLongitude = gpsLongitude;
    }

    public String getEquipment() {
        return equipment;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }
}
