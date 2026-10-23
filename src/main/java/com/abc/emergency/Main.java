package com.abc.emergency;

import com.abc.emergency.service.AlertService;
import com.abc.emergency.service.DispatchService;
import com.abc.emergency.service.LoginService;
import com.abc.emergency.service.ReportService;
import com.abc.emergency.service.TriageService;

public class Main {

    public static void main(String[] args) {

        System.out.println("==============================================");
        System.out.println(" CONNECTED AMBULANCE AND EMERGENCY RESPONSE");
        System.out.println(" MANAGEMENT SYSTEM");
        System.out.println("==============================================");
        System.out.println();

        LoginService loginService = new LoginService();
        TriageService triageService = new TriageService();
        DispatchService dispatchService = new DispatchService();
        AlertService alertService = new AlertService();
        ReportService reportService = new ReportService();

        System.out.println("System services initialized successfully.");
        System.out.println();

        System.out.println("Available Modules:");
        System.out.println("1. User Management");
        System.out.println("2. Patient Management");
        System.out.println("3. Ambulance Management");
        System.out.println("4. Vital Monitoring");
        System.out.println("5. AI-Assisted Triage");
        System.out.println("6. Hospital Management");
        System.out.println("7. Emergency Dispatch");
        System.out.println("8. Alerts and Notifications");
        System.out.println("9. Reports and Dashboard");
        System.out.println();

        System.out.println("Core Flow:");
        System.out.println("Login -> Patient -> Emergency Case -> Ambulance");
        System.out.println("-> Vitals -> Triage -> Hospital -> Alert -> Report");
        System.out.println();

        System.out.println("Application startup verification successful.");
    }
}