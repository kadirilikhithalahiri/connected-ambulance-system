package com.abc.emergency;

import com.abc.emergency.gui.ReportFrame;
import com.abc.emergency.service.ReportService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ReportFrameTest {

    @Test
    void testReportServiceGetters() {
        ReportService service = new ReportService();

        String caseReport = service.getCaseReport(1);
        assertNotNull(caseReport);
        assertTrue(caseReport.contains("CASE REPORT"));
        assertTrue(caseReport.contains("Ananya Sharma"));

        String vitalsReport = service.getVitalsReport(1);
        assertNotNull(vitalsReport);
        assertTrue(vitalsReport.contains("VITALS REPORT"));
        assertTrue(vitalsReport.contains("Vital ID"));

        String ambReport = service.getAmbulanceUtilizationReport();
        assertNotNull(ambReport);
        assertTrue(ambReport.contains("AMBULANCE UTILIZATION REPORT"));
        assertTrue(ambReport.contains("AVAILABLE"));

        String hospReport = service.getHospitalReadinessReport();
        assertNotNull(hospReport);
        assertTrue(hospReport.contains("HOSPITAL READINESS REPORT"));
        assertTrue(hospReport.contains("ABC Emergency Hospital"));

        String respReport = service.getResponseTimeReport();
        assertNotNull(respReport);
        assertTrue(respReport.contains("RESPONSE TIME REPORT"));
        assertTrue(respReport.contains("Completed Dispatches"));

        String dailyReport = service.getDailyDashboard();
        assertNotNull(dailyReport);
        assertTrue(dailyReport.contains("DAILY DASHBOARD"));
        assertTrue(dailyReport.contains("Cases Today"));

        String monthlyReport = service.getMonthlyReport();
        assertNotNull(monthlyReport);
        assertTrue(monthlyReport.contains("MONTHLY EMERGENCY REPORT"));
        assertTrue(monthlyReport.contains("Total Cases"));
    }

    @Test
    void testReportFrameInitialization() {
        ReportFrame frame = new ReportFrame();
        assertNotNull(frame);
        assertEquals("Connected Ambulance System - Reports & Analytics", frame.getTitle());
        frame.dispose();
    }
}
