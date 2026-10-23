package com.abc.emergency;

import com.abc.emergency.model.PatientVital;
import com.abc.emergency.service.AlertService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AlertServiceTest {

    @Test
    void testAlertManagement() {

        AlertService alertService = new AlertService();

        PatientVital criticalVital = new PatientVital();

        criticalVital.setHeartRate(160);
        criticalVital.setSystolicBp(75);
        criticalVital.setDiastolicBp(50);
        criticalVital.setSpo2(85);
        criticalVital.setTemperature(39.5);
        criticalVital.setRespiratoryRate(32);

        boolean criticalAlert =
                alertService.checkVitalsAndCreateAlert(
                        1,
                        criticalVital
                );

        assertTrue(criticalAlert);

        System.out.println("Critical Patient Alert Test: SUCCESS");

        PatientVital abnormalVital = new PatientVital();

        abnormalVital.setHeartRate(125);
        abnormalVital.setSystolicBp(88);
        abnormalVital.setDiastolicBp(60);
        abnormalVital.setSpo2(92);
        abnormalVital.setTemperature(38.5);
        abnormalVital.setRespiratoryRate(24);

        boolean abnormalAlert =
                alertService.checkVitalsAndCreateAlert(
                        1,
                        abnormalVital
                );

        assertTrue(abnormalAlert);

        System.out.println("Abnormal Vital Alert Test: SUCCESS");

        boolean hospitalAlert =
                alertService.createHospitalFullAlert(1);

        assertTrue(hospitalAlert);

        System.out.println("Hospital Full Alert Test: SUCCESS");

        boolean arrivalAlert =
                alertService.createArrivalAlert(1);

        assertTrue(arrivalAlert);

        System.out.println("Arrival Alert Test: SUCCESS");
    }
}
