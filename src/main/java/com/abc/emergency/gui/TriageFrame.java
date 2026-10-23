package com.abc.emergency.gui;

import com.abc.emergency.model.PatientVital;
import com.abc.emergency.service.TriageService;

import javax.swing.*;
import java.awt.*;

public class TriageFrame extends JFrame {

    private JTextField caseIdField;
    private JTextField heartRateField;
    private JTextField systolicBPField;
    private JTextField diastolicBPField;
    private JTextField spo2Field;
    private JTextField respiratoryRateField;
    private JTextField temperatureField;
    private JTextField glucoseField;
    private JTextField symptomsField;
    private JTextField hospitalIdField;
    private JTextField etaField;

    private JLabel priorityLabel;
    private JLabel specialtyLabel;
    private JLabel priorityCodeLabel;
    private JLabel recommendationLabel;

    private TriageService triageService;

    public TriageFrame() {

        triageService = new TriageService();

        setTitle("AI Triage");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(
                new BorderLayout(10, 10)
        );

        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(
                        15, 15, 15, 15
                )
        );

        JLabel titleLabel = new JLabel(
                "AI-ASSISTED EMERGENCY TRIAGE",
                SwingConstants.CENTER
        );

        titleLabel.setFont(
                new Font("Arial", Font.BOLD, 22)
        );

        mainPanel.add(
                titleLabel,
                BorderLayout.NORTH
        );

        JPanel inputPanel = new JPanel(
                new GridLayout(6, 4, 10, 10)
        );

        caseIdField = new JTextField();
        heartRateField = new JTextField();
        systolicBPField = new JTextField();
        diastolicBPField = new JTextField();
        spo2Field = new JTextField();
        respiratoryRateField = new JTextField();
        temperatureField = new JTextField();
        glucoseField = new JTextField();
        symptomsField = new JTextField();
        hospitalIdField = new JTextField();
        etaField = new JTextField("15");

        inputPanel.add(new JLabel("Emergency Case ID:"));
        inputPanel.add(caseIdField);

        inputPanel.add(new JLabel("Heart Rate:"));
        inputPanel.add(heartRateField);

        inputPanel.add(new JLabel("Systolic BP:"));
        inputPanel.add(systolicBPField);

        inputPanel.add(new JLabel("Diastolic BP:"));
        inputPanel.add(diastolicBPField);

        inputPanel.add(new JLabel("SpO2:"));
        inputPanel.add(spo2Field);

        inputPanel.add(new JLabel("Respiratory Rate:"));
        inputPanel.add(respiratoryRateField);

        inputPanel.add(new JLabel("Temperature:"));
        inputPanel.add(temperatureField);

        inputPanel.add(new JLabel("Glucose:"));
        inputPanel.add(glucoseField);

        inputPanel.add(new JLabel("Symptoms:"));
        inputPanel.add(symptomsField);

        inputPanel.add(new JLabel("Hospital ID:"));
        inputPanel.add(hospitalIdField);

        inputPanel.add(new JLabel("Estimated Arrival (min):"));
        inputPanel.add(etaField);

        inputPanel.add(new JLabel("Triage Method:"));
        inputPanel.add(
                new JLabel("Rule-Based AI Simulation")
        );

        JPanel resultPanel = new JPanel(
                new GridLayout(4, 2, 10, 10)
        );

        resultPanel.setBorder(
                BorderFactory.createTitledBorder(
                        "AI Triage Result"
                )
        );

        priorityLabel = new JLabel("-");
        specialtyLabel = new JLabel("-");
        priorityCodeLabel = new JLabel("-");
        recommendationLabel = new JLabel("-");

        resultPanel.add(
                new JLabel("Priority Level:")
        );
        resultPanel.add(priorityLabel);

        resultPanel.add(
                new JLabel("Suggested Specialty:")
        );
        resultPanel.add(specialtyLabel);

        resultPanel.add(
                new JLabel("Priority Code:")
        );
        resultPanel.add(priorityCodeLabel);

        resultPanel.add(
                new JLabel("Recommendation:")
        );
        resultPanel.add(recommendationLabel);

        JPanel centerPanel = new JPanel(
                new BorderLayout(10, 10)
        );

        centerPanel.add(
                inputPanel,
                BorderLayout.NORTH
        );

        centerPanel.add(
                resultPanel,
                BorderLayout.CENTER
        );

        mainPanel.add(
                centerPanel,
                BorderLayout.CENTER
        );

        JButton triageButton =
                new JButton("Run AI Triage");

        JButton clearButton =
                new JButton("Clear");

        JPanel buttonPanel = new JPanel(
                new FlowLayout(
                        FlowLayout.CENTER,
                        10,
                        5
                )
        );

        buttonPanel.add(triageButton);
        buttonPanel.add(clearButton);

        mainPanel.add(
                buttonPanel,
                BorderLayout.SOUTH
        );

        add(mainPanel);

        triageButton.addActionListener(
                e -> performTriage()
        );

        clearButton.addActionListener(
                e -> clearFields()
        );
    }

    private void performTriage() {

        int caseId;
        int heartRate;
        int systolicBP;
        int diastolicBP;
        double spo2;
        int respiratoryRate;
        double temperature;
        double glucose;
        int hospitalId;
        int estimatedArrivalMinutes;

        try {

            caseId = Integer.parseInt(
                    caseIdField.getText().trim()
            );

            heartRate = Integer.parseInt(
                    heartRateField.getText().trim()
            );

            systolicBP = Integer.parseInt(
                    systolicBPField.getText().trim()
            );

            diastolicBP = Integer.parseInt(
                    diastolicBPField.getText().trim()
            );

            spo2 = Double.parseDouble(
                    spo2Field.getText().trim()
            );

            respiratoryRate = Integer.parseInt(
                    respiratoryRateField.getText().trim()
            );

            temperature = Double.parseDouble(
                    temperatureField.getText().trim()
            );

            glucose = Double.parseDouble(
                    glucoseField.getText().trim()
            );

            hospitalId = Integer.parseInt(
                    hospitalIdField.getText().trim()
            );

            estimatedArrivalMinutes = Integer.parseInt(
                    etaField.getText().trim()
            );

        } catch (NumberFormatException e) {

            showError(
                    "Enter valid numeric values in all fields."
            );

            return;
        }

        String symptoms =
                symptomsField.getText().trim();

        if (caseId <= 0) {
            showError(
                    "Emergency Case ID must be greater than zero."
            );
            return;
        }

        if (heartRate <= 0) {
            showError(
                    "Heart rate must be greater than zero."
            );
            return;
        }

        if (systolicBP <= 0 ||
                diastolicBP <= 0) {

            showError(
                    "Blood pressure values must be greater than zero."
            );
            return;
        }

        if (spo2 < 0 || spo2 > 100) {

            showError(
                    "SpO2 must be between 0 and 100."
            );
            return;
        }

        if (respiratoryRate <= 0) {

            showError(
                    "Respiratory rate must be greater than zero."
            );
            return;
        }

        if (temperature <= 0) {

            showError(
                    "Temperature must be greater than zero."
            );
            return;
        }

        if (glucose < 0) {

            showError(
                    "Glucose cannot be negative."
            );
            return;
        }

        if (hospitalId <= 0) {

            showError(
                    "Hospital ID must be greater than zero."
            );
            return;
        }

        if (estimatedArrivalMinutes < 0) {

            showError(
                    "Estimated arrival time cannot be negative."
            );
            return;
        }

        PatientVital vital =
                new PatientVital();

        vital.setCaseId(caseId);
        vital.setHeartRate(heartRate);
        vital.setSystolicBp(systolicBP);
        vital.setDiastolicBp(diastolicBP);
        vital.setSpo2(spo2);
        vital.setTemperature(temperature);
        vital.setRespiratoryRate(respiratoryRate);
        vital.setGlucose(glucose);
        vital.setSymptoms(symptoms);

        String priority =
                triageService.determinePriority(vital);

        String specialty =
                triageService.determineSpecialty(symptoms);

        String priorityCode =
                getPriorityCode(priority);

        String recommendation =
                getRecommendation(
                        priority,
                        specialty
                );

        boolean saved =
                triageService.performTriage(
                        caseId,
                        vital,
                        symptoms,
                        hospitalId,
                        estimatedArrivalMinutes
                );

        if (!saved) {

            showError(
                    "AI Triage could not be saved."
            );

            return;
        }

        priorityLabel.setText(priority);
        specialtyLabel.setText(specialty);
        priorityCodeLabel.setText(priorityCode);
        recommendationLabel.setText(
                "<html><body width='350'>"
                        + recommendation
                        + "</body></html>"
        );

        JOptionPane.showMessageDialog(
                this,
                "AI Triage completed and saved successfully.",
                "Success",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private String getPriorityCode(
            String priority) {

        return switch (priority) {
            case "CRITICAL" -> "P1";
            case "HIGH" -> "P2";
            case "MODERATE" -> "P3";
            default -> "P4";
        };
    }

    private String getRecommendation(
            String priority,
            String specialty) {

        return switch (priority) {
            case "CRITICAL" ->
                    "Immediate emergency care required. Route patient to "
                            + specialty
                            + " facility without delay.";

            case "HIGH" ->
                    "Urgent medical attention required. Prefer "
                            + specialty
                            + " emergency services.";

            case "MODERATE" ->
                    "Medical evaluation required. Monitor patient during transport.";

            default ->
                    "Stable condition. Continue routine monitoring during transport.";
        };
    }

    private void clearFields() {

        caseIdField.setText("");
        heartRateField.setText("");
        systolicBPField.setText("");
        diastolicBPField.setText("");
        spo2Field.setText("");
        respiratoryRateField.setText("");
        temperatureField.setText("");
        glucoseField.setText("");
        symptomsField.setText("");
        hospitalIdField.setText("");
        etaField.setText("15");

        priorityLabel.setText("-");
        specialtyLabel.setText("-");
        priorityCodeLabel.setText("-");
        recommendationLabel.setText("-");
    }

    private void showError(
            String message) {

        JOptionPane.showMessageDialog(
                this,
                message,
                "Validation Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
}