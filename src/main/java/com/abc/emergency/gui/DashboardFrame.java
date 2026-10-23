package com.abc.emergency.gui;

import com.abc.emergency.model.User;

import javax.swing.*;
import java.awt.*;

public class DashboardFrame extends JFrame {

    private final User user;

    public DashboardFrame(User user) {

        this.user = user;

        setTitle("Connected Ambulance System - Dashboard");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        );

        JLabel titleLabel = new JLabel(
                "EMERGENCY RESPONSE MANAGEMENT DASHBOARD",
                SwingConstants.CENTER
        );
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));

        JLabel userLabel = new JLabel(
                "Welcome, " + user.getFullName()
                        + " | Role: " + user.getRole(),
                SwingConstants.CENTER
        );
        userLabel.setFont(new Font("Arial", Font.PLAIN, 16));

        JPanel headerPanel = new JPanel(new GridLayout(2, 1));
        headerPanel.add(titleLabel);
        headerPanel.add(userLabel);

        JPanel modulePanel = new JPanel(
                new GridLayout(3, 3, 15, 15)
        );

        JButton patientButton = new JButton("Patient Management");
        JButton emergencyButton = new JButton("Emergency Cases");
        JButton ambulanceButton = new JButton("Ambulance Management");
        JButton vitalButton = new JButton("Vital Monitoring");
        JButton triageButton = new JButton("AI Triage");
        JButton hospitalButton = new JButton("Hospital Management");
        JButton dispatchButton = new JButton("Emergency Dispatch");
        JButton alertButton = new JButton("Alerts");
        JButton reportButton = new JButton("Reports");

        modulePanel.add(patientButton);
        modulePanel.add(emergencyButton);
        modulePanel.add(ambulanceButton);
        modulePanel.add(vitalButton);
        modulePanel.add(triageButton);
        modulePanel.add(hospitalButton);
        modulePanel.add(dispatchButton);
        modulePanel.add(alertButton);
        modulePanel.add(reportButton);

        JPanel bottomPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT)
        );

        JButton logoutButton = new JButton("Logout");
        bottomPanel.add(logoutButton);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(modulePanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);

        patientButton.addActionListener(e -> {
            PatientFrame patientFrame = new PatientFrame();
            patientFrame.setVisible(true);
        });

        emergencyButton.addActionListener(e -> {
            EmergencyCaseFrame emergencyCaseFrame = new EmergencyCaseFrame();
            emergencyCaseFrame.setVisible(true);
        });

        ambulanceButton.addActionListener(e -> {
            AmbulanceFrame ambulanceFrame = new AmbulanceFrame();
            ambulanceFrame.setVisible(true);
        });

        vitalButton.addActionListener(e -> {
            VitalFrame vitalFrame = new VitalFrame();
            vitalFrame.setVisible(true);
        });

        triageButton.addActionListener(e -> {
            TriageFrame triageFrame = new TriageFrame();
            triageFrame.setVisible(true);
        });

        hospitalButton.addActionListener(e -> {
            HospitalFrame hospitalFrame = new HospitalFrame();
            hospitalFrame.setVisible(true);
        });

        dispatchButton.addActionListener(e -> {
            DispatchFrame dispatchFrame = new DispatchFrame();
            dispatchFrame.setVisible(true);
        });

        alertButton.addActionListener(e -> {
            AlertFrame alertFrame = new AlertFrame();
            alertFrame.setVisible(true);
        });

        reportButton.addActionListener(e -> {
            ReportFrame reportFrame = new ReportFrame();
            reportFrame.setVisible(true);
        });

        logoutButton.addActionListener(e -> logout());
    }

    private void logout() {

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to logout?",
                "Logout",
                JOptionPane.YES_NO_OPTION
        );

        if (choice == JOptionPane.YES_OPTION) {
            dispose();
            new LoginFrame().setVisible(true);
        }
    }
}
