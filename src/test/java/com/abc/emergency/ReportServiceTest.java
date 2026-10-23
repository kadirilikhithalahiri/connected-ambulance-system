package com.abc.emergency;

import com.abc.emergency.service.ReportService;
import org.junit.jupiter.api.Test;

public class ReportServiceTest {

    @Test
    void testReportGeneration() {

        ReportService reportService = new ReportService();

        System.out.println("\nTesting Case Report");
        reportService.generateCaseReport(1);

        System.out.println("\nTesting Vitals Report");
        reportService.generateVitalsReport(1);

        System.out.println("\nTesting Ambulance Utilization Report");
        reportService.generateAmbulanceUtilizationReport();

        System.out.println("\nTesting Hospital Readiness Report");
        reportService.generateHospitalReadinessReport();

        System.out.println("\nTesting Response Time Report");
        reportService.generateResponseTimeReport();

        System.out.println("\nTesting Daily Dashboard");
        reportService.generateDailyDashboard();

        System.out.println("\nTesting Monthly Report");
        reportService.generateMonthlyReport();

        System.out.println("\nReport Generation Test: SUCCESS");
    }
}
