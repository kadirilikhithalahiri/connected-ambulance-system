package com.abc.emergency.service;

import com.abc.emergency.dao.PatientVitalDAO;
import com.abc.emergency.model.PatientVital;
import com.abc.emergency.util.ValidationUtil;

import java.util.Random;

public class VitalMonitoringService implements Runnable {

    private final int caseId;
    private final PatientVitalDAO vitalDAO;
    private final Random random;
    private volatile boolean monitoring;

    public VitalMonitoringService(int caseId) {
        this.caseId = caseId;
        this.vitalDAO = new PatientVitalDAO();
        this.random = new Random();
        this.monitoring = true;
    }

    @Override
    public void run() {

        System.out.println("Vital monitoring started for Case ID: " + caseId);

        int readingCount = 0;

        while (monitoring && readingCount < 5) {

            try {
                PatientVital vital = generateVitalReading();

                if (vital != null) {
                    vitalDAO.addVital(vital);

                    System.out.println(
                            "Reading " + (readingCount + 1)
                            + " | HR: " + vital.getHeartRate()
                            + " | BP: " + vital.getSystolicBp()
                            + "/" + vital.getDiastolicBp()
                            + " | SpO2: " + vital.getSpo2()
                            + " | Temp: " + vital.getTemperature()
                            + " | RR: " + vital.getRespiratoryRate()
                    );
                }

                readingCount++;
                Thread.sleep(2000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Vital monitoring interrupted.");
                break;
            } catch (Exception e) {
                System.out.println("Vital monitoring error: " + e.getMessage());
                break;
            }
        }

        System.out.println("Vital monitoring stopped for Case ID: " + caseId);
    }

    private PatientVital generateVitalReading() {

        int heartRate = 55 + random.nextInt(91);
        int systolicBp = 85 + random.nextInt(61);
        int diastolicBp = 55 + random.nextInt(31);
        double spo2 = 90 + random.nextDouble() * 10;
        double temperature = 36 + random.nextDouble() * 3;
        int respiratoryRate = 12 + random.nextInt(19);

        if (!ValidationUtil.isValidHeartRate(heartRate)
                || !ValidationUtil.isValidBloodPressure(systolicBp, diastolicBp)
                || !ValidationUtil.isValidSpo2(spo2)
                || !ValidationUtil.isValidTemperature(temperature)
                || !ValidationUtil.isValidRespiratoryRate(respiratoryRate)) {
            return null;
        }

        PatientVital vital = new PatientVital();

        vital.setCaseId(caseId);
        vital.setHeartRate(heartRate);
        vital.setSystolicBp(systolicBp);
        vital.setDiastolicBp(diastolicBp);
        vital.setSpo2(Math.round(spo2 * 100.0) / 100.0);
        vital.setTemperature(Math.round(temperature * 10.0) / 10.0);
        vital.setRespiratoryRate(respiratoryRate);

        return vital;
    }

    public void stopMonitoring() {
        monitoring = false;
    }
}