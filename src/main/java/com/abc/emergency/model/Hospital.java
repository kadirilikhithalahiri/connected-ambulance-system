package com.abc.emergency.model;

public class Hospital {

    private int hospitalId;
    private String hospitalName;
    private String location;
    private int icuBeds;
    private int emergencyBeds;
    private int ventilators;
    private boolean traumaAvailable;
    private boolean cardioAvailable;
    private boolean neuroAvailable;
    private String availability;

    public Hospital() {
    }

    public Hospital(int hospitalId, String hospitalName, String location,
                    int icuBeds, int emergencyBeds, int ventilators,
                    boolean traumaAvailable, boolean cardioAvailable,
                    boolean neuroAvailable, String availability) {
        this.hospitalId = hospitalId;
        this.hospitalName = hospitalName;
        this.location = location;
        this.icuBeds = icuBeds;
        this.emergencyBeds = emergencyBeds;
        this.ventilators = ventilators;
        this.traumaAvailable = traumaAvailable;
        this.cardioAvailable = cardioAvailable;
        this.neuroAvailable = neuroAvailable;
        this.availability = availability;
    }

    public int getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(int hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getIcuBeds() {
        return icuBeds;
    }

    public void setIcuBeds(int icuBeds) {
        this.icuBeds = icuBeds;
    }

    public int getEmergencyBeds() {
        return emergencyBeds;
    }

    public void setEmergencyBeds(int emergencyBeds) {
        this.emergencyBeds = emergencyBeds;
    }

    public int getVentilators() {
        return ventilators;
    }

    public void setVentilators(int ventilators) {
        this.ventilators = ventilators;
    }

    public boolean isTraumaAvailable() {
        return traumaAvailable;
    }

    public void setTraumaAvailable(boolean traumaAvailable) {
        this.traumaAvailable = traumaAvailable;
    }

    public boolean isCardioAvailable() {
        return cardioAvailable;
    }

    public void setCardioAvailable(boolean cardioAvailable) {
        this.cardioAvailable = cardioAvailable;
    }

    public boolean isNeuroAvailable() {
        return neuroAvailable;
    }

    public void setNeuroAvailable(boolean neuroAvailable) {
        this.neuroAvailable = neuroAvailable;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }
}