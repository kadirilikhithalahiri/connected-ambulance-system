package com.abc.emergency.gui;

import com.abc.emergency.dao.AmbulanceDAO;
import com.abc.emergency.model.Ambulance;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class AmbulanceFrame extends JFrame {

    private JTextField vehicleNumberField;
    private JTextField driverIdField;
    private JTextField paramedicIdField;
    private JTextField latitudeField;
    private JTextField longitudeField;
    private JTextArea equipmentArea;
    private JComboBox<String> availabilityBox;

    private JTable ambulanceTable;
    private DefaultTableModel tableModel;

    private AmbulanceDAO ambulanceDAO;
    private int selectedAmbulanceId = -1;

    public AmbulanceFrame() {

        ambulanceDAO = new AmbulanceDAO();

        setTitle("Ambulance Management");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );

        JLabel titleLabel = new JLabel(
                "AMBULANCE MANAGEMENT",
                SwingConstants.CENTER
        );
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));

        mainPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(
                new GridLayout(5, 4, 10, 10)
        );

        vehicleNumberField = new JTextField();
        driverIdField = new JTextField();
        paramedicIdField = new JTextField();
        latitudeField = new JTextField();
        longitudeField = new JTextField();

        equipmentArea = new JTextArea(3, 20);

        availabilityBox = new JComboBox<>(
                new String[]{
                        "AVAILABLE",
                        "DISPATCHED",
                        "MAINTENANCE"
                }
        );

        formPanel.add(new JLabel("Vehicle Number:"));
        formPanel.add(vehicleNumberField);

        formPanel.add(new JLabel("Driver ID:"));
        formPanel.add(driverIdField);

        formPanel.add(new JLabel("Paramedic ID:"));
        formPanel.add(paramedicIdField);

        formPanel.add(new JLabel("GPS Latitude:"));
        formPanel.add(latitudeField);

        formPanel.add(new JLabel("GPS Longitude:"));
        formPanel.add(longitudeField);

        formPanel.add(new JLabel("Availability:"));
        formPanel.add(availabilityBox);

        formPanel.add(new JLabel("Equipment:"));
        formPanel.add(new JScrollPane(equipmentArea));

        formPanel.add(new JLabel("Selected Ambulance ID:"));
        formPanel.add(new JLabel("Select a row to edit or delete"));

        JPanel centerPanel = new JPanel(
                new BorderLayout(10, 10)
        );

        centerPanel.add(formPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[]{
                        "ID",
                        "Vehicle Number",
                        "Driver ID",
                        "Paramedic ID",
                        "Latitude",
                        "Longitude",
                        "Availability"
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

        ambulanceTable = new JTable(tableModel);

        ambulanceTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        centerPanel.add(
                new JScrollPane(ambulanceTable),
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
                e -> addAmbulance()
        );

        updateButton.addActionListener(
                e -> updateAmbulance()
        );

        deleteButton.addActionListener(
                e -> deleteAmbulance()
        );

        clearButton.addActionListener(
                e -> clearFields()
        );

        refreshButton.addActionListener(
                e -> loadAmbulances()
        );

        ambulanceTable.getSelectionModel()
                .addListSelectionListener(
                        e -> loadSelectedAmbulance()
                );

        loadAmbulances();
    }

    private void addAmbulance() {

        Ambulance ambulance =
                readAmbulanceFromForm();

        if (ambulance == null) {
            return;
        }

        boolean result =
                ambulanceDAO.addAmbulance(ambulance);

        if (result) {

            JOptionPane.showMessageDialog(
                    this,
                    "Ambulance registered successfully.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
            );

            clearFields();
            loadAmbulances();

        } else {

            JOptionPane.showMessageDialog(
                    this,
                    "Failed to register ambulance.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void updateAmbulance() {

        if (selectedAmbulanceId == -1) {

            JOptionPane.showMessageDialog(
                    this,
                    "Select an ambulance first.",
                    "Update Error",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        Ambulance ambulance =
                readAmbulanceFromForm();

        if (ambulance == null) {
            return;
        }

        ambulance.setAmbulanceId(
                selectedAmbulanceId
        );

        boolean result =
                ambulanceDAO.updateAmbulance(ambulance);

        if (result) {

            JOptionPane.showMessageDialog(
                    this,
                    "Ambulance updated successfully.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
            );

            clearFields();
            loadAmbulances();

        } else {

            JOptionPane.showMessageDialog(
                    this,
                    "Failed to update ambulance.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void deleteAmbulance() {

        if (selectedAmbulanceId == -1) {

            JOptionPane.showMessageDialog(
                    this,
                    "Select an ambulance first.",
                    "Delete Error",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        int choice =
                JOptionPane.showConfirmDialog(
                        this,
                        "Delete selected ambulance?",
                        "Confirm Delete",
                        JOptionPane.YES_NO_OPTION
                );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        boolean result =
                ambulanceDAO.deleteAmbulance(
                        selectedAmbulanceId
                );

        if (result) {

            JOptionPane.showMessageDialog(
                    this,
                    "Ambulance deleted successfully.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
            );

            clearFields();
            loadAmbulances();

        } else {

            JOptionPane.showMessageDialog(
                    this,
                    "Failed to delete ambulance.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private Ambulance readAmbulanceFromForm() {

        String vehicleNumber =
                vehicleNumberField.getText().trim();

        String driverIdText =
                driverIdField.getText().trim();

        String paramedicIdText =
                paramedicIdField.getText().trim();

        String latitudeText =
                latitudeField.getText().trim();

        String longitudeText =
                longitudeField.getText().trim();

        String equipment =
                equipmentArea.getText().trim();

        String availability =
                (String) availabilityBox.getSelectedItem();

        if (vehicleNumber.isEmpty()) {
            showError("Vehicle number cannot be empty.");
            return null;
        }

        int driverId;

        try {
            driverId = Integer.parseInt(driverIdText);
        } catch (NumberFormatException e) {
            showError("Driver ID must be a valid number.");
            return null;
        }

        int paramedicId;

        try {
            paramedicId = Integer.parseInt(paramedicIdText);
        } catch (NumberFormatException e) {
            showError("Paramedic ID must be a valid number.");
            return null;
        }

        double latitude;

        try {
            latitude = Double.parseDouble(latitudeText);
        } catch (NumberFormatException e) {
            showError("Latitude must be a valid number.");
            return null;
        }

        double longitude;

        try {
            longitude = Double.parseDouble(longitudeText);
        } catch (NumberFormatException e) {
            showError("Longitude must be a valid number.");
            return null;
        }

        if (driverId <= 0 || paramedicId <= 0) {
            showError("Driver ID and Paramedic ID must be greater than zero.");
            return null;
        }

        if (latitude < -90 || latitude > 90) {
            showError("Latitude must be between -90 and 90.");
            return null;
        }

        if (longitude < -180 || longitude > 180) {
            showError("Longitude must be between -180 and 180.");
            return null;
        }

        if (equipment.isEmpty()) {
            showError("Equipment cannot be empty.");
            return null;
        }

        Ambulance ambulance =
                new Ambulance();

        ambulance.setVehicleNumber(
                vehicleNumber
        );

        ambulance.setDriverId(
                driverId
        );

        ambulance.setParamedicId(
                paramedicId
        );

        ambulance.setGpsLatitude(
                latitude
        );

        ambulance.setGpsLongitude(
                longitude
        );

        ambulance.setEquipment(
                equipment
        );

        ambulance.setAvailability(
                availability
        );

        return ambulance;
    }

    private void loadAmbulances() {

        tableModel.setRowCount(0);

        List<Ambulance> ambulances =
                ambulanceDAO.getAllAmbulances();

        for (Ambulance ambulance : ambulances) {

            tableModel.addRow(
                    new Object[]{
                            ambulance.getAmbulanceId(),
                            ambulance.getVehicleNumber(),
                            ambulance.getDriverId(),
                            ambulance.getParamedicId(),
                            ambulance.getGpsLatitude(),
                            ambulance.getGpsLongitude(),
                            ambulance.getAvailability()
                    }
            );
        }
    }

    private void loadSelectedAmbulance() {

        int row =
                ambulanceTable.getSelectedRow();

        if (row == -1) {
            return;
        }

        selectedAmbulanceId =
                (int) tableModel.getValueAt(
                        row,
                        0
                );

        Ambulance ambulance =
                ambulanceDAO.getAmbulanceById(
                        selectedAmbulanceId
                );

        if (ambulance == null) {
            return;
        }

        vehicleNumberField.setText(
                ambulance.getVehicleNumber()
        );

        driverIdField.setText(
                String.valueOf(
                        ambulance.getDriverId()
                )
        );

        paramedicIdField.setText(
                String.valueOf(
                        ambulance.getParamedicId()
                )
        );

        latitudeField.setText(
                String.valueOf(
                        ambulance.getGpsLatitude()
                )
        );

        longitudeField.setText(
                String.valueOf(
                        ambulance.getGpsLongitude()
                )
        );

        equipmentArea.setText(
                ambulance.getEquipment()
        );

        availabilityBox.setSelectedItem(
                ambulance.getAvailability()
        );
    }

    private void clearFields() {

        selectedAmbulanceId = -1;

        vehicleNumberField.setText("");
        driverIdField.setText("");
        paramedicIdField.setText("");
        latitudeField.setText("");
        longitudeField.setText("");
        equipmentArea.setText("");

        availabilityBox.setSelectedIndex(0);

        ambulanceTable.clearSelection();
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