package com.abc.emergency.model;

public class PatientVital {

    private int vitalId;
    private int caseId;
    private int heartRate;
    private int systolicBp;
    private int diastolicBp;
    private double spo2;
    private double temperature;
    private int respiratoryRate;
    private double glucose;
    private String symptoms;

    public PatientVital() {
    }

    public PatientVital(int vitalId, int caseId, int heartRate, int systolicBp,
                        int diastolicBp, double spo2, double temperature,
                        int respiratoryRate, double glucose, String symptoms) {
        this.vitalId = vitalId;
        this.caseId = caseId;
        this.heartRate = heartRate;
        this.systolicBp = systolicBp;
        this.diastolicBp = diastolicBp;
        this.spo2 = spo2;
        this.temperature = temperature;
        this.respiratoryRate = respiratoryRate;
        this.glucose = glucose;
        this.symptoms = symptoms;
    }

    public int getVitalId() {
        return vitalId;
    }

    public void setVitalId(int vitalId) {
        this.vitalId = vitalId;
    }

    public int getCaseId() {
        return caseId;
    }

    public void setCaseId(int caseId) {
        this.caseId = caseId;
    }

    public int getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(int heartRate) {
        this.heartRate = heartRate;
    }

    public int getSystolicBp() {
        return systolicBp;
    }

    public void setSystolicBp(int systolicBp) {
        this.systolicBp = systolicBp;
    }

    public int getDiastolicBp() {
        return diastolicBp;
    }

    public void setDiastolicBp(int diastolicBp) {
        this.diastolicBp = diastolicBp;
    }

    public double getSpo2() {
        return spo2;
    }

    public void setSpo2(double spo2) {
        this.spo2 = spo2;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getRespiratoryRate() {
        return respiratoryRate;
    }

    public void setRespiratoryRate(int respiratoryRate) {
        this.respiratoryRate = respiratoryRate;
    }

    public double getGlucose() {
        return glucose;
    }

    public void setGlucose(double glucose) {
        this.glucose = glucose;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }
}