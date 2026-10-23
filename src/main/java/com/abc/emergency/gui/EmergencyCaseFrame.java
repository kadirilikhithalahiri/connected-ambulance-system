package com.abc.emergency.gui;

import com.abc.emergency.dao.EmergencyCaseDAO;
import com.abc.emergency.model.EmergencyCase;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class EmergencyCaseFrame extends JFrame {

    private JTextField patientIdField;
    private JTextField locationField;
    private JTextField emergencyTypeField;
    private JTextArea symptomsArea;
    private JComboBox<String> priorityBox;
    private JComboBox<String> statusBox;

    private JTable caseTable;
    private DefaultTableModel tableModel;

    private EmergencyCaseDAO caseDAO;
    private int selectedCaseId = -1;

    public EmergencyCaseFrame() {

        caseDAO = new EmergencyCaseDAO();

        setTitle("Emergency Case Management");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );

        JLabel titleLabel = new JLabel(
                "EMERGENCY CASE MANAGEMENT",
                SwingConstants.CENTER
        );
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));

        mainPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(
                new GridLayout(4, 4, 10, 10)
        );

        patientIdField = new JTextField();
        locationField = new JTextField();
        emergencyTypeField = new JTextField();

        symptomsArea = new JTextArea(3, 20);

        priorityBox = new JComboBox<>(
                new String[]{
                        "CRITICAL",
                        "HIGH",
                        "MODERATE",
                        "LOW"
                }
        );

        statusBox = new JComboBox<>(
                new String[]{
                        "REGISTERED",
                        "DISPATCHED",
                        "IN_TRANSIT",
                        "ARRIVED",
                        "COMPLETED",
                        "CANCELLED"
                }
        );

        formPanel.add(new JLabel("Patient ID:"));
        formPanel.add(patientIdField);

        formPanel.add(new JLabel("Incident Location:"));
        formPanel.add(locationField);

        formPanel.add(new JLabel("Emergency Type:"));
        formPanel.add(emergencyTypeField);

        formPanel.add(new JLabel("Priority:"));
        formPanel.add(priorityBox);

        formPanel.add(new JLabel("Case Status:"));
        formPanel.add(statusBox);

        formPanel.add(new JLabel("Symptoms:"));
        formPanel.add(new JScrollPane(symptomsArea));

        formPanel.add(new JLabel("Selected Case ID:"));
        formPanel.add(new JLabel("Select a row to edit or delete"));

        JPanel centerPanel = new JPanel(
                new BorderLayout(10, 10)
        );

        centerPanel.add(formPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[]{
                        "Case ID",
                        "Patient ID",
                        "Location",
                        "Emergency Type",
                        "Priority",
                        "Status"
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

        caseTable = new JTable(tableModel);

        caseTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        centerPanel.add(
                new JScrollPane(caseTable),
                BorderLayout.CENTER
        );

        mainPanel.add(
                centerPanel,
                BorderLayout.CENTER
        );

        JButton addButton = new JButton("Register");
        JButton updateButton = new JButton("Update");
        JButton deleteButton = new JButton("Delete");
        JButton clearButton = new JButton("Clear");
        JButton refreshButton = new JButton("Refresh");

        JPanel buttonPanel = new JPanel(
                new FlowLayout(
                        FlowLayout.CENTER,
                        10,
                        5
                )
        );

        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(refreshButton);

        mainPanel.add(
                buttonPanel,
                BorderLayout.SOUTH
        );

        add(mainPanel);

        addButton.addActionListener(
                e -> addEmergencyCase()
        );

        updateButton.addActionListener(
                e -> updateEmergencyCase()
        );

        deleteButton.addActionListener(
                e -> deleteEmergencyCase()
        );

        clearButton.addActionListener(
                e -> clearFields()
        );

        refreshButton.addActionListener(
                e -> loadEmergencyCases()
        );

        caseTable.getSelectionModel()
                .addListSelectionListener(
                        e -> loadSelectedCase()
                );

        loadEmergencyCases();
    }

    private void addEmergencyCase() {

        EmergencyCase emergencyCase =
                readCaseFromForm();

        if (emergencyCase == null) {
            return;
        }

        boolean result =
                caseDAO.addEmergencyCase(
                        emergencyCase
                );

        if (result) {

            JOptionPane.showMessageDialog(
                    this,
                    "Emergency case registered successfully.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
            );

            clearFields();
            loadEmergencyCases();

        } else {

            JOptionPane.showMessageDialog(
                    this,
                    "Failed to register emergency case.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void updateEmergencyCase() {

        if (selectedCaseId == -1) {

            JOptionPane.showMessageDialog(
                    this,
                    "Select an emergency case first.",
                    "Update Error",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        EmergencyCase emergencyCase =
                readCaseFromForm();

        if (emergencyCase == null) {
            return;
        }

        emergencyCase.setCaseId(
                selectedCaseId
        );

        boolean result =
                caseDAO.updateEmergencyCase(
                        emergencyCase
                );

        if (result) {

            JOptionPane.showMessageDialog(
                    this,
                    "Emergency case updated successfully.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
            );

            clearFields();
            loadEmergencyCases();

        } else {

            JOptionPane.showMessageDialog(
                    this,
                    "Failed to update emergency case.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void deleteEmergencyCase() {

        if (selectedCaseId == -1) {

            JOptionPane.showMessageDialog(
                    this,
                    "Select an emergency case first.",
                    "Delete Error",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        int choice =
                JOptionPane.showConfirmDialog(
                        this,
                        "Delete selected emergency case?",
                        "Confirm Delete",
                        JOptionPane.YES_NO_OPTION
                );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        boolean result =
                caseDAO.deleteEmergencyCase(
                        selectedCaseId
                );

        if (result) {

            JOptionPane.showMessageDialog(
                    this,
                    "Emergency case deleted successfully.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
            );

            clearFields();
            loadEmergencyCases();

        } else {

            JOptionPane.showMessageDialog(
                    this,
                    "Failed to delete emergency case.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private EmergencyCase readCaseFromForm() {

        String patientIdText =
                patientIdField.getText().trim();

        String location =
                locationField.getText().trim();

        String emergencyType =
                emergencyTypeField.getText().trim();

        String symptoms =
                symptomsArea.getText().trim();

        String priority =
                (String) priorityBox.getSelectedItem();

        String status =
                (String) statusBox.getSelectedItem();

        if (patientIdText.isEmpty()) {

            showError(
                    "Patient ID cannot be empty."
            );

            return null;
        }

        int patientId;

        try {

            patientId =
                    Integer.parseInt(patientIdText);

        } catch (NumberFormatException e) {

            showError(
                    "Patient ID must be a valid number."
            );

            return null;
        }

        if (patientId <= 0) {

            showError(
                    "Patient ID must be greater than zero."
            );

            return null;
        }

        if (location.isEmpty()) {

            showError(
                    "Incident location cannot be empty."
            );

            return null;
        }

        if (emergencyType.isEmpty()) {

            showError(
                    "Emergency type cannot be empty."
            );

            return null;
        }

        if (symptoms.isEmpty()) {

            showError(
                    "Symptoms cannot be empty."
            );

            return null;
        }

        EmergencyCase emergencyCase =
                new EmergencyCase();

        emergencyCase.setPatientId(
                patientId
        );

        emergencyCase.setIncidentLocation(
                location
        );

        emergencyCase.setEmergencyType(
                emergencyType
        );

        emergencyCase.setSymptoms(
                symptoms
        );

        emergencyCase.setPriorityLevel(
                priority
        );

        emergencyCase.setCaseStatus(
                status
        );

        return emergencyCase;
    }

    private void loadEmergencyCases() {

        tableModel.setRowCount(0);

        List<EmergencyCase> cases =
                caseDAO.getAllEmergencyCases();

        for (EmergencyCase emergencyCase : cases) {

            tableModel.addRow(
                    new Object[]{
                            emergencyCase.getCaseId(),
                            emergencyCase.getPatientId(),
                            emergencyCase.getIncidentLocation(),
                            emergencyCase.getEmergencyType(),
                            emergencyCase.getPriorityLevel(),
                            emergencyCase.getCaseStatus()
                    }
            );
        }
    }

    private void loadSelectedCase() {

        int row =
                caseTable.getSelectedRow();

        if (row == -1) {
            return;
        }

        selectedCaseId =
                (int) tableModel.getValueAt(
                        row,
                        0
                );

        EmergencyCase emergencyCase =
                caseDAO.getEmergencyCaseById(
                        selectedCaseId
                );

        if (emergencyCase == null) {
            return;
        }

        patientIdField.setText(
                String.valueOf(
                        emergencyCase.getPatientId()
                )
        );

        locationField.setText(
                emergencyCase.getIncidentLocation()
        );

        emergencyTypeField.setText(
                emergencyCase.getEmergencyType()
        );

        symptomsArea.setText(
                emergencyCase.getSymptoms()
        );

        priorityBox.setSelectedItem(
                emergencyCase.getPriorityLevel()
        );

        statusBox.setSelectedItem(
                emergencyCase.getCaseStatus()
        );
    }

    private void clearFields() {

        selectedCaseId = -1;

        patientIdField.setText("");
        locationField.setText("");
        emergencyTypeField.setText("");
        symptomsArea.setText("");

        priorityBox.setSelectedIndex(0);
        statusBox.setSelectedIndex(0);

        caseTable.clearSelection();
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