package com.abc.emergency.gui;

import com.abc.emergency.util.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class VitalFrame extends JFrame {

    private JTextField caseIdField;
    private JTextField heartRateField;
    private JTextField systolicBPField;
    private JTextField diastolicBPField;
    private JTextField spo2Field;
    private JTextField temperatureField;
    private JTextField respiratoryRateField;
    private JTextField glucoseField;
    private JTextArea symptomsArea;

    private JTable vitalTable;
    private DefaultTableModel tableModel;

    public VitalFrame() {

        setTitle("Vital Monitoring");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );

        JLabel titleLabel = new JLabel(
                "PATIENT VITAL MONITORING",
                SwingConstants.CENTER
        );

        titleLabel.setFont(
                new Font("Arial", Font.BOLD, 22)
        );

        mainPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(
                new GridLayout(5, 4, 10, 10)
        );

        caseIdField = new JTextField();
        heartRateField = new JTextField();
        systolicBPField = new JTextField();
        diastolicBPField = new JTextField();
        spo2Field = new JTextField();
        temperatureField = new JTextField();
        respiratoryRateField = new JTextField();
        glucoseField = new JTextField();

        symptomsArea = new JTextArea(3, 20);

        formPanel.add(
                new JLabel("Emergency Case ID:")
        );
        formPanel.add(caseIdField);

        formPanel.add(
                new JLabel("Heart Rate:")
        );
        formPanel.add(heartRateField);

        formPanel.add(
                new JLabel("Systolic BP:")
        );
        formPanel.add(systolicBPField);

        formPanel.add(
                new JLabel("Diastolic BP:")
        );
        formPanel.add(diastolicBPField);

        formPanel.add(
                new JLabel("SpO2:")
        );
        formPanel.add(spo2Field);

        formPanel.add(
                new JLabel("Temperature:")
        );
        formPanel.add(temperatureField);

        formPanel.add(
                new JLabel("Respiratory Rate:")
        );
        formPanel.add(respiratoryRateField);

        formPanel.add(
                new JLabel("Glucose:")
        );
        formPanel.add(glucoseField);

        formPanel.add(
                new JLabel("Symptoms:")
        );
        formPanel.add(
                new JScrollPane(symptomsArea)
        );

        formPanel.add(
                new JLabel("SpO2 Range:")
        );
        formPanel.add(
                new JLabel("0 - 100")
        );

        JPanel centerPanel = new JPanel(
                new BorderLayout(10, 10)
        );

        centerPanel.add(
                formPanel,
                BorderLayout.NORTH
        );

        tableModel = new DefaultTableModel(
                new String[]{
                        "Vital ID",
                        "Case ID",
                        "Heart Rate",
                        "Systolic BP",
                        "Diastolic BP",
                        "SpO2",
                        "Temperature",
                        "Respiratory Rate",
                        "Glucose",
                        "Symptoms",
                        "Recorded At"
                },
                0
        ) {
            @Override
            public boolean isCellEditable(
                    int row,
                    int column) {
                return false;
            }
        };

        vitalTable = new JTable(tableModel);

        centerPanel.add(
                new JScrollPane(vitalTable),
                BorderLayout.CENTER
        );

        mainPanel.add(
                centerPanel,
                BorderLayout.CENTER
        );

        JButton recordButton =
                new JButton("Record Vital");

        JButton refreshButton =
                new JButton("Refresh");

        JButton clearButton =
                new JButton("Clear");

        JPanel buttonPanel = new JPanel(
                new FlowLayout(
                        FlowLayout.CENTER,
                        10,
                        5
                )
        );

        buttonPanel.add(recordButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(clearButton);

        mainPanel.add(
                buttonPanel,
                BorderLayout.SOUTH
        );

        add(mainPanel);

        recordButton.addActionListener(
                e -> recordVital()
        );

        refreshButton.addActionListener(
                e -> loadVitals()
        );

        clearButton.addActionListener(
                e -> clearFields()
        );
    }

    private void recordVital() {

        int caseId;
        int heartRate;
        int systolicBP;
        int diastolicBP;
        double spo2;
        double temperature;
        int respiratoryRate;
        double glucose;

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

            temperature = Double.parseDouble(
                    temperatureField.getText().trim()
            );

            respiratoryRate = Integer.parseInt(
                    respiratoryRateField.getText().trim()
            );

            glucose = Double.parseDouble(
                    glucoseField.getText().trim()
            );

        } catch (NumberFormatException e) {

            showError(
                    "All vital values must be valid numbers."
            );

            return;
        }

        String symptoms =
                symptomsArea.getText().trim();

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

        if (temperature <= 0) {

            showError(
                    "Temperature must be greater than zero."
            );

            return;
        }

        if (respiratoryRate <= 0) {

            showError(
                    "Respiratory rate must be greater than zero."
            );

            return;
        }

        if (glucose < 0) {

            showError(
                    "Glucose cannot be negative."
            );

            return;
        }

        String sql = """
                INSERT INTO Patient_Vitals
                (
                    case_id,
                    heart_rate,
                    systolic_bp,
                    diastolic_bp,
                    spo2,
                    temperature,
                    respiratory_rate,
                    glucose,
                    symptoms
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection =
                     DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, caseId);
            statement.setInt(2, heartRate);
            statement.setInt(3, systolicBP);
            statement.setInt(4, diastolicBP);
            statement.setDouble(5, spo2);
            statement.setDouble(6, temperature);
            statement.setInt(7, respiratoryRate);
            statement.setDouble(8, glucose);
            statement.setString(9, symptoms);

            int result =
                    statement.executeUpdate();

            if (result > 0) {

                JOptionPane.showMessageDialog(
                        this,
                        "Vital record saved successfully.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                );

                clearVitalInputFields();

                caseIdField.setText(
                        String.valueOf(caseId)
                );

                loadVitals();
            }

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Failed to save vital record.\n"
                            + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void loadVitals() {

        tableModel.setRowCount(0);

        String caseIdText =
                caseIdField.getText().trim();

        if (caseIdText.isEmpty()) {
            return;
        }

        int caseId;

        try {

            caseId = Integer.parseInt(
                    caseIdText
            );

        } catch (NumberFormatException e) {

            return;
        }

        String sql = """
                SELECT vital_id,
                       case_id,
                       heart_rate,
                       systolic_bp,
                       diastolic_bp,
                       spo2,
                       temperature,
                       respiratory_rate,
                       glucose,
                       symptoms,
                       recorded_at
                FROM Patient_Vitals
                WHERE case_id = ?
                ORDER BY recorded_at DESC
                """;

        try (Connection connection =
                     DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, caseId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                while (resultSet.next()) {

                    tableModel.addRow(
                            new Object[]{
                                    resultSet.getInt(
                                            "vital_id"
                                    ),
                                    resultSet.getInt(
                                            "case_id"
                                    ),
                                    resultSet.getInt(
                                            "heart_rate"
                                    ),
                                    resultSet.getInt(
                                            "systolic_bp"
                                    ),
                                    resultSet.getInt(
                                            "diastolic_bp"
                                    ),
                                    resultSet.getDouble(
                                            "spo2"
                                    ),
                                    resultSet.getDouble(
                                            "temperature"
                                    ),
                                    resultSet.getInt(
                                            "respiratory_rate"
                                    ),
                                    resultSet.getDouble(
                                            "glucose"
                                    ),
                                    resultSet.getString(
                                            "symptoms"
                                    ),
                                    resultSet.getTimestamp(
                                            "recorded_at"
                                    )
                            }
                    );
                }
            }

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Failed to load vital records.\n"
                            + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void clearVitalInputFields() {

        heartRateField.setText("");
        systolicBPField.setText("");
        diastolicBPField.setText("");
        spo2Field.setText("");
        temperatureField.setText("");
        respiratoryRateField.setText("");
        glucoseField.setText("");
        symptomsArea.setText("");
    }

    private void clearFields() {

        caseIdField.setText("");

        clearVitalInputFields();

        tableModel.setRowCount(0);
    }

    private void showError(String message) {

        JOptionPane.showMessageDialog(
                this,
                message,
                "Validation Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
}