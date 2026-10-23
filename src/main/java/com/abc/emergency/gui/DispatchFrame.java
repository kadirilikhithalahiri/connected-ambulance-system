package com.abc.emergency.gui;

import com.abc.emergency.model.DispatchRecord;
import com.abc.emergency.service.DispatchService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class DispatchFrame extends JFrame {

    private JTextField caseIdField;
    private JTextField ambulanceIdField;
    private JTextField etaField;
    private JTextField dispatchIdField;

    private JComboBox<String> statusCombo;

    private JTable dispatchTable;
    private DefaultTableModel tableModel;

    private final DispatchService dispatchService;

    public DispatchFrame() {

        dispatchService = new DispatchService();

        setTitle("Emergency Dispatch Management");
        setSize(980, 620);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );

        JLabel titleLabel = new JLabel(
                "EMERGENCY DISPATCH MANAGEMENT",
                SwingConstants.CENTER
        );
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));

        JPanel formPanel = new JPanel(
                new GridLayout(2, 4, 10, 10)
        );

        formPanel.add(new JLabel("Emergency Case ID:"));
        caseIdField = new JTextField();
        formPanel.add(caseIdField);

        formPanel.add(new JLabel("Ambulance ID:"));
        ambulanceIdField = new JTextField();
        formPanel.add(ambulanceIdField);

        formPanel.add(new JLabel("ETA (minutes):"));
        etaField = new JTextField();
        formPanel.add(etaField);

        formPanel.add(new JLabel("Dispatch ID:"));
        dispatchIdField = new JTextField();
        formPanel.add(dispatchIdField);

        JPanel statusPanel = new JPanel(
                new FlowLayout(FlowLayout.LEFT)
        );

        statusPanel.add(new JLabel("Status:"));

        statusCombo = new JComboBox<>(
                new String[]{
                        "DISPATCHED",
                        "EN_ROUTE",
                        "ARRIVED",
                        "COMPLETED",
                        "CANCELLED"
                }
        );

        statusPanel.add(statusCombo);

        JButton dispatchButton = new JButton("Dispatch Ambulance");
        JButton viewButton = new JButton("View Dispatch");
        JButton viewAllButton = new JButton("View All Dispatches");
        JButton updateButton = new JButton("Update Status");
        JButton arrivedButton = new JButton("Mark Arrived");
        JButton completeButton = new JButton("Complete Dispatch");
        JButton clearButton = new JButton("Clear");

        JPanel buttonPanel = new JPanel(
                new GridLayout(2, 4, 10, 10)
        );

        buttonPanel.add(dispatchButton);
        buttonPanel.add(viewButton);
        buttonPanel.add(viewAllButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(arrivedButton);
        buttonPanel.add(completeButton);
        buttonPanel.add(new JLabel("")); // spacer

        JPanel topPanel = new JPanel(
                new BorderLayout(10, 10)
        );

        JPanel inputPanel = new JPanel(
                new BorderLayout(10, 10)
        );

        inputPanel.add(formPanel, BorderLayout.CENTER);
        inputPanel.add(statusPanel, BorderLayout.SOUTH);

        topPanel.add(inputPanel, BorderLayout.NORTH);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        tableModel = new DefaultTableModel(
                new String[]{
                        "Dispatch ID",
                        "Case ID",
                        "Ambulance ID",
                        "ETA",
                        "Status"
                },
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        dispatchTable = new JTable(tableModel);
        dispatchTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dispatchTable.setRowHeight(22);

        JScrollPane tableScrollPane = new JScrollPane(dispatchTable);

        JPanel headerPanel = new JPanel(
                new BorderLayout(10, 10)
        );

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(topPanel, BorderLayout.CENTER);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(tableScrollPane, BorderLayout.CENTER);

        add(mainPanel);

        dispatchButton.addActionListener(e -> dispatchAmbulance());
        viewButton.addActionListener(e -> viewDispatch());
        viewAllButton.addActionListener(e -> viewAllDispatches());
        updateButton.addActionListener(e -> updateStatus());
        arrivedButton.addActionListener(e -> markArrived());
        completeButton.addActionListener(e -> completeDispatch());
        clearButton.addActionListener(e -> clearFields());

        dispatchTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedDispatch();
            }
        });
    }

    private void dispatchAmbulance() {

        try {
            String caseText = caseIdField.getText().trim();
            String ambText = ambulanceIdField.getText().trim();
            String etaText = etaField.getText().trim();

            if (caseText.isEmpty() || ambText.isEmpty() || etaText.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Please enter Case ID, Ambulance ID, and ETA.",
                        "Input Required",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            int caseId = Integer.parseInt(caseText);
            int ambulanceId = Integer.parseInt(ambText);
            int eta = Integer.parseInt(etaText);

            boolean success = dispatchService.dispatchAmbulance(
                    caseId,
                    ambulanceId,
                    eta
            );

            if (success) {
                int generatedId = dispatchService.getLastGeneratedDispatchId();
                dispatchIdField.setText(String.valueOf(generatedId));

                JOptionPane.showMessageDialog(
                        this,
                        "Ambulance dispatched successfully!\nGenerated Dispatch ID: " + generatedId,
                        "Dispatch Success",
                        JOptionPane.INFORMATION_MESSAGE
                );

                viewDispatch();

            } else {
                String error = dispatchService.getLastErrorMessage();
                JOptionPane.showMessageDialog(
                        this,
                        error != null ? error : "Ambulance dispatch failed.",
                        "Dispatch Failed",
                        JOptionPane.ERROR_MESSAGE
                );
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter valid numeric values for Case ID, Ambulance ID, and ETA.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE
            );
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Dispatch Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void viewDispatch() {

        try {
            String idText = dispatchIdField.getText().trim();
            if (idText.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Please enter a Dispatch ID to view.",
                        "Input Required",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            int dispatchId = Integer.parseInt(idText);

            DispatchRecord record = dispatchService.getDispatchById(dispatchId);

            tableModel.setRowCount(0);

            if (record != null) {
                tableModel.addRow(
                        new Object[]{
                                record.getDispatchId(),
                                record.getCaseId(),
                                record.getAmbulanceId(),
                                record.getEstimatedArrivalMinutes(),
                                record.getDispatchStatus()
                        }
                );
                statusCombo.setSelectedItem(record.getDispatchStatus());
                caseIdField.setText(String.valueOf(record.getCaseId()));
                ambulanceIdField.setText(String.valueOf(record.getAmbulanceId()));
                etaField.setText(String.valueOf(record.getEstimatedArrivalMinutes()));

            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Dispatch record ID " + dispatchId + " not found.",
                        "View Dispatch",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Enter a valid numeric Dispatch ID.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE
            );
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void viewAllDispatches() {
        List<DispatchRecord> records = dispatchService.getAllDispatchRecords();
        tableModel.setRowCount(0);

        for (DispatchRecord record : records) {
            tableModel.addRow(
                    new Object[]{
                            record.getDispatchId(),
                            record.getCaseId(),
                            record.getAmbulanceId(),
                            record.getEstimatedArrivalMinutes(),
                            record.getDispatchStatus()
                    }
            );
        }

        JOptionPane.showMessageDialog(
                this,
                records.size() + " dispatch record(s) loaded.",
                "All Dispatches",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void updateStatus() {

        try {
            String idText = dispatchIdField.getText().trim();
            if (idText.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Please select or enter a Dispatch ID.",
                        "Input Required",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            int dispatchId = Integer.parseInt(idText);
            String status = (String) statusCombo.getSelectedItem();

            boolean success = dispatchService.updateDispatchStatus(dispatchId, status);

            if (success) {
                JOptionPane.showMessageDialog(
                        this,
                        "Dispatch status updated successfully to " + status + ".",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                );

                viewDispatch();

            } else {
                String error = dispatchService.getLastErrorMessage();
                JOptionPane.showMessageDialog(
                        this,
                        error != null ? error : "Dispatch status update failed.",
                        "Update Failed",
                        JOptionPane.ERROR_MESSAGE
                );
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Enter a valid numeric Dispatch ID.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE
            );
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Update Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void markArrived() {

        try {
            String idText = dispatchIdField.getText().trim();
            if (idText.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Please select or enter a Dispatch ID.",
                        "Input Required",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            int dispatchId = Integer.parseInt(idText);

            boolean success = dispatchService.markAsArrived(dispatchId);

            if (success) {
                statusCombo.setSelectedItem("ARRIVED");

                JOptionPane.showMessageDialog(
                        this,
                        "Ambulance arrival recorded successfully.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                );

                viewDispatch();

            } else {
                String error = dispatchService.getLastErrorMessage();
                JOptionPane.showMessageDialog(
                        this,
                        error != null ? error : "Unable to mark ambulance as arrived.",
                        "Arrival Failed",
                        JOptionPane.ERROR_MESSAGE
                );
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Enter a valid numeric Dispatch ID.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE
            );
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Arrival Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void completeDispatch() {

        try {
            String idText = dispatchIdField.getText().trim();
            if (idText.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Please select or enter a Dispatch ID.",
                        "Input Required",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            int dispatchId = Integer.parseInt(idText);

            int ambulanceId = -1;
            String ambText = ambulanceIdField.getText().trim();
            if (!ambText.isEmpty()) {
                ambulanceId = Integer.parseInt(ambText);
            }

            boolean success = dispatchService.completeDispatch(dispatchId, ambulanceId);

            if (success) {
                statusCombo.setSelectedItem("COMPLETED");

                JOptionPane.showMessageDialog(
                        this,
                        "Dispatch completed successfully.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                );

                viewDispatch();

            } else {
                String error = dispatchService.getLastErrorMessage();
                JOptionPane.showMessageDialog(
                        this,
                        error != null ? error : "Unable to complete dispatch.",
                        "Completion Failed",
                        JOptionPane.ERROR_MESSAGE
                );
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter valid numeric IDs.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE
            );
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Completion Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void loadSelectedDispatch() {

        int row = dispatchTable.getSelectedRow();

        if (row == -1) {
            return;
        }

        dispatchIdField.setText(
                tableModel.getValueAt(row, 0).toString()
        );

        caseIdField.setText(
                tableModel.getValueAt(row, 1).toString()
        );

        ambulanceIdField.setText(
                tableModel.getValueAt(row, 2).toString()
        );

        etaField.setText(
                tableModel.getValueAt(row, 3).toString()
        );

        statusCombo.setSelectedItem(
                tableModel.getValueAt(row, 4).toString()
        );
    }

    private void clearFields() {

        caseIdField.setText("");
        ambulanceIdField.setText("");
        etaField.setText("");
        dispatchIdField.setText("");

        statusCombo.setSelectedIndex(0);

        tableModel.setRowCount(0);
    }
}
