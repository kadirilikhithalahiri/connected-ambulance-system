package com.abc.emergency.gui;

import com.abc.emergency.dao.AlertDAO;
import com.abc.emergency.model.Alert;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class AlertFrame extends JFrame {

    private JTextField alertIdField;
    private JTextField caseIdField;
    private JComboBox<String> roleCombo;
    private JTable alertTable;
    private DefaultTableModel tableModel;

    private final AlertDAO alertDAO;

    public AlertFrame() {

        alertDAO = new AlertDAO();

        setTitle("Emergency Alert Management");
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel(
                "Emergency Alert Management",
                SwingConstants.CENTER
        );
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));

        alertIdField = new JTextField();
        caseIdField = new JTextField();

        roleCombo = new JComboBox<>(new String[]{
                "DOCTOR",
                "DISPATCHER",
                "HOSPITAL"
        });

        JPanel formPanel = new JPanel(new GridLayout(2, 4, 10, 10));

        formPanel.add(new JLabel("Alert ID"));
        formPanel.add(alertIdField);

        formPanel.add(new JLabel("Case ID"));
        formPanel.add(caseIdField);

        formPanel.add(new JLabel("Recipient Role"));
        formPanel.add(roleCombo);

        formPanel.add(new JLabel(""));

        JPanel buttonPanel = new JPanel(new GridLayout(2, 3, 10, 10));

        JButton viewByIdButton = new JButton("View Alert");
        JButton viewCaseButton = new JButton("View Case Alerts");
        JButton viewUnreadButton = new JButton("View Unread");
        JButton viewRoleButton = new JButton("View By Role");
        JButton markReadButton = new JButton("Mark As Read");
        JButton deleteButton = new JButton("Delete Alert");

        buttonPanel.add(viewByIdButton);
        buttonPanel.add(viewCaseButton);
        buttonPanel.add(viewUnreadButton);
        buttonPanel.add(viewRoleButton);
        buttonPanel.add(markReadButton);
        buttonPanel.add(deleteButton);

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.add(formPanel, BorderLayout.NORTH);
        topPanel.add(buttonPanel, BorderLayout.CENTER);

        JPanel headerPanel = new JPanel(
                new BorderLayout(10, 10)
        );

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(topPanel, BorderLayout.CENTER);

        tableModel = new DefaultTableModel(
                new Object[]{
                        "Alert ID",
                        "Case ID",
                        "Alert Type",
                        "Message",
                        "Severity",
                        "Recipient Role",
                        "Read"
                }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        alertTable = new JTable(tableModel);
        alertTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        alertTable.setRowHeight(24);
        alertTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JScrollPane tableScrollPane = new JScrollPane(alertTable);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(tableScrollPane, BorderLayout.CENTER);

        add(mainPanel);

        viewByIdButton.addActionListener(e -> viewAlertById());
        viewCaseButton.addActionListener(e -> viewAlertsByCase());
        viewUnreadButton.addActionListener(e -> viewUnreadAlerts());
        viewRoleButton.addActionListener(e -> viewAlertsByRole());
        markReadButton.addActionListener(e -> markAlertAsRead());
        deleteButton.addActionListener(e -> deleteAlert());

        alertTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedAlert();
            }
        });
    }

    private void viewAlertById() {

        try {

            int alertId = Integer.parseInt(alertIdField.getText().trim());

            Alert alert = alertDAO.getAlertById(alertId);

            tableModel.setRowCount(0);

            if (alert == null) {
                JOptionPane.showMessageDialog(
                        this,
                        "Alert not found.",
                        "View Alert",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            addAlertToTable(alert);

        } catch (NumberFormatException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Enter a valid Alert ID.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void viewAlertsByCase() {

        try {

            int caseId = Integer.parseInt(caseIdField.getText().trim());

            List<Alert> alerts = alertDAO.getAlertsByCaseId(caseId);

            loadAlerts(alerts);

            JOptionPane.showMessageDialog(
                    this,
                    alerts.size() + " alert(s) loaded for Case ID " + caseId + ".",
                    "Case Alerts",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (NumberFormatException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Enter a valid Case ID.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void viewUnreadAlerts() {

        List<Alert> alerts = alertDAO.getUnreadAlerts();

        loadAlerts(alerts);

        JOptionPane.showMessageDialog(
                this,
                alerts.size() + " unread alert(s) loaded.",
                "Unread Alerts",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void viewAlertsByRole() {

        String role = (String) roleCombo.getSelectedItem();

        List<Alert> alerts =
                alertDAO.getAlertsByRecipientRole(role);

        loadAlerts(alerts);

        JOptionPane.showMessageDialog(
                this,
                alerts.size() + " alert(s) loaded for " + role + ".",
                "Role Alerts",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void markAlertAsRead() {

        try {

            int alertId = Integer.parseInt(alertIdField.getText().trim());

            boolean success =
                    alertDAO.markAlertAsRead(alertId);

            if (success) {

                JOptionPane.showMessageDialog(
                        this,
                        "Alert marked as read successfully.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                );

                viewAlertById();

            } else {

                JOptionPane.showMessageDialog(
                        this,
                        "Alert could not be marked as read.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }

        } catch (NumberFormatException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Enter a valid Alert ID.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void deleteAlert() {

        try {

            int alertId = Integer.parseInt(alertIdField.getText().trim());

            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to delete Alert ID "
                            + alertId + "?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION
            );

            if (choice != JOptionPane.YES_OPTION) {
                return;
            }

            boolean success =
                    alertDAO.deleteAlert(alertId);

            if (success) {

                JOptionPane.showMessageDialog(
                        this,
                        "Alert deleted successfully.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                );

                tableModel.setRowCount(0);

            } else {

                JOptionPane.showMessageDialog(
                        this,
                        "Alert could not be deleted.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }

        } catch (NumberFormatException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Enter a valid Alert ID.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void loadAlerts(List<Alert> alerts) {

        tableModel.setRowCount(0);

        for (Alert alert : alerts) {
            addAlertToTable(alert);
        }
    }

    private void addAlertToTable(Alert alert) {

        tableModel.addRow(new Object[]{
                alert.getAlertId(),
                alert.getCaseId(),
                alert.getAlertType(),
                alert.getAlertMessage(),
                alert.getSeverity(),
                alert.getRecipientRole(),
                alert.isRead() ? "READ" : "UNREAD"
        });
    }

    private void loadSelectedAlert() {

        int selectedRow = alertTable.getSelectedRow();

        if (selectedRow == -1) {
            return;
        }

        alertIdField.setText(
                tableModel.getValueAt(selectedRow, 0).toString()
        );

        caseIdField.setText(
                tableModel.getValueAt(selectedRow, 1).toString()
        );

        roleCombo.setSelectedItem(
                tableModel.getValueAt(selectedRow, 5).toString()
        );
    }
}