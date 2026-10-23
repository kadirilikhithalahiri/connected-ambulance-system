package com.abc.emergency.gui;

import com.abc.emergency.service.ReportService;
import com.abc.emergency.util.FileUtil;
import com.abc.emergency.util.ValidationUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ReportFrame extends JFrame {

    private final ReportService reportService;

    private JComboBox<String> reportTypeCombo;
    private JTextField caseIdField;
    private JLabel caseIdLabel;
    private JTextArea reportTextArea;
    private JLabel statusLabel;

    public ReportFrame() {

        this.reportService = new ReportService();

        setTitle("Connected Ambulance System - Reports & Analytics");
        setSize(950, 650);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel titleLabel = new JLabel(
                "EMERGENCY SYSTEM REPORTS AND ANALYTICS",
                SwingConstants.CENTER
        );
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));

        JPanel controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBorder(BorderFactory.createTitledBorder("Report Configuration"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel reportTypeLabel = new JLabel("Report Type:");
        reportTypeLabel.setFont(new Font("Arial", Font.BOLD, 13));

        reportTypeCombo = new JComboBox<>(new String[]{
                "Case Report",
                "Vitals Report",
                "Ambulance Utilization",
                "Hospital Readiness",
                "Response Time",
                "Daily Dashboard",
                "Monthly Report"
        });
        reportTypeCombo.setFont(new Font("Arial", Font.PLAIN, 13));

        caseIdLabel = new JLabel("Case ID (Required):");
        caseIdLabel.setFont(new Font("Arial", Font.BOLD, 13));

        caseIdField = new JTextField(10);
        caseIdField.setFont(new Font("Arial", Font.PLAIN, 13));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        controlPanel.add(reportTypeLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.6;
        controlPanel.add(reportTypeCombo, gbc);

        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        controlPanel.add(caseIdLabel, gbc);

        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.weightx = 0.4;
        controlPanel.add(caseIdField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        JButton generateButton = new JButton("Generate Report");
        JButton exportButton = new JButton("Export Report");
        JButton clearButton = new JButton("Clear");
        JButton closeButton = new JButton("Close");

        buttonPanel.add(generateButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(closeButton);

        JPanel topContainer = new JPanel(new BorderLayout(8, 8));
        topContainer.add(titleLabel, BorderLayout.NORTH);
        topContainer.add(controlPanel, BorderLayout.CENTER);
        topContainer.add(buttonPanel, BorderLayout.SOUTH);

        reportTextArea = new JTextArea();
        reportTextArea.setEditable(false);
        reportTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        reportTextArea.setBackground(new Color(250, 250, 250));
        reportTextArea.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(reportTextArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Report Output"));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Ready. Select a report type and click 'Generate Report'.");
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        bottomPanel.add(statusLabel, BorderLayout.WEST);

        mainPanel.add(topContainer, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);

        reportTypeCombo.addActionListener(e -> updateCaseIdFieldState());
        generateButton.addActionListener(e -> generateReport());
        exportButton.addActionListener(e -> exportReport());
        clearButton.addActionListener(e -> clearReport());
        closeButton.addActionListener(e -> dispose());

        updateCaseIdFieldState();
    }

    private void updateCaseIdFieldState() {
        String selected = (String) reportTypeCombo.getSelectedItem();
        boolean requiresCaseId = "Case Report".equals(selected) || "Vitals Report".equals(selected);

        caseIdField.setEnabled(requiresCaseId);
        if (requiresCaseId) {
            caseIdLabel.setText("Case ID (Required):");
            caseIdField.setBackground(Color.WHITE);
        } else {
            caseIdLabel.setText("Case ID (Not Applicable):");
            caseIdField.setBackground(new Color(238, 238, 238));
            caseIdField.setText("");
        }
    }

    private void generateReport() {
        String selected = (String) reportTypeCombo.getSelectedItem();
        if (selected == null) {
            return;
        }

        boolean requiresCaseId = "Case Report".equals(selected) || "Vitals Report".equals(selected);
        int caseId = -1;

        if (requiresCaseId) {
            String caseIdText = caseIdField.getText().trim();
            if (caseIdText.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Please enter a Case ID to generate " + selected + ".",
                        "Input Required",
                        JOptionPane.WARNING_MESSAGE
                );
                caseIdField.requestFocus();
                return;
            }

            try {
                caseId = Integer.parseInt(caseIdText);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Invalid Case ID. Please enter a valid integer ID.",
                        "Input Error",
                        JOptionPane.ERROR_MESSAGE
                );
                caseIdField.requestFocus();
                return;
            }

            if (!ValidationUtil.isValidId(caseId)) {
                JOptionPane.showMessageDialog(
                        this,
                        "Case ID must be a positive integer greater than zero.",
                        "Input Error",
                        JOptionPane.ERROR_MESSAGE
                );
                caseIdField.requestFocus();
                return;
            }
        }

        String reportText = "";

        switch (selected) {
            case "Case Report":
                reportText = reportService.getCaseReport(caseId);
                break;
            case "Vitals Report":
                reportText = reportService.getVitalsReport(caseId);
                break;
            case "Ambulance Utilization":
                reportText = reportService.getAmbulanceUtilizationReport();
                break;
            case "Hospital Readiness":
                reportText = reportService.getHospitalReadinessReport();
                break;
            case "Response Time":
                reportText = reportService.getResponseTimeReport();
                break;
            case "Daily Dashboard":
                reportText = reportService.getDailyDashboard();
                break;
            case "Monthly Report":
                reportText = reportService.getMonthlyReport();
                break;
            default:
                JOptionPane.showMessageDialog(
                        this,
                        "Unsupported report type: " + selected,
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
        }

        reportTextArea.setText(reportText);
        reportTextArea.setCaretPosition(0);

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        statusLabel.setText(selected + " generated successfully at " + timestamp + ".");
    }

    private void exportReport() {
        String content = reportTextArea.getText();
        if (content == null || content.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "No report content to export. Please generate a report first.",
                    "Export Report",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String selected = (String) reportTypeCombo.getSelectedItem();
        String defaultFileName = (selected != null ? selected.toLowerCase().replace(' ', '_') : "report")
                + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt";

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Report to Text File");
        fileChooser.setSelectedFile(new File(defaultFileName));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();

            if (!fileToSave.getName().toLowerCase().endsWith(".txt")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".txt");
            }

            try {
                FileUtil.writeTextFile(fileToSave.getAbsolutePath(), content);
                JOptionPane.showMessageDialog(
                        this,
                        "Report exported successfully to:\n" + fileToSave.getAbsolutePath(),
                        "Export Success",
                        JOptionPane.INFORMATION_MESSAGE
                );
                statusLabel.setText("Exported to " + fileToSave.getName() + ".");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Failed to export report: " + ex.getMessage(),
                        "Export Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private void clearReport() {
        reportTextArea.setText("");
        if (caseIdField.isEnabled()) {
            caseIdField.setText("");
            caseIdField.requestFocus();
        }
        statusLabel.setText("Ready.");
    }
}
