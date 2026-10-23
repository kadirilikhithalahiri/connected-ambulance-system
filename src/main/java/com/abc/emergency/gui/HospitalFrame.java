package com.abc.emergency.gui;

import com.abc.emergency.dao.HospitalDAO;
import com.abc.emergency.model.Hospital;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class HospitalFrame extends JFrame {

    private JTextField idField;
    private JTextField nameField;
    private JTextField locationField;
    private JTextField icuField;
    private JTextField emergencyBedsField;
    private JTextField ventilatorsField;

    private JCheckBox traumaCheckBox;
    private JCheckBox cardioCheckBox;
    private JCheckBox neuroCheckBox;

    private JComboBox<String> availabilityComboBox;

    private JTable hospitalTable;
    private DefaultTableModel tableModel;

    private HospitalDAO hospitalDAO;

    public HospitalFrame() {

        hospitalDAO = new HospitalDAO();

        setTitle("Hospital Management");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );

        JLabel titleLabel = new JLabel(
                "HOSPITAL MANAGEMENT",
                SwingConstants.CENTER
        );

        titleLabel.setFont(
                new Font("Arial", Font.BOLD, 22)
        );

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));

        topPanel.add(
                titleLabel,
                BorderLayout.NORTH
        );

        JPanel inputPanel = new JPanel(
                new GridLayout(7, 4, 10, 10)
        );

        idField = new JTextField();
        nameField = new JTextField();
        locationField = new JTextField();
        icuField = new JTextField();
        emergencyBedsField = new JTextField();
        ventilatorsField = new JTextField();

        traumaCheckBox = new JCheckBox("Trauma");
        cardioCheckBox = new JCheckBox("Cardiology");
        neuroCheckBox = new JCheckBox("Neurology");

        availabilityComboBox = new JComboBox<>(
                new String[]{
                        "AVAILABLE",
                        "LIMITED",
                        "FULL"
                }
        );

        inputPanel.add(new JLabel("Hospital ID:"));
        inputPanel.add(idField);

        inputPanel.add(new JLabel("Hospital Name:"));
        inputPanel.add(nameField);

        inputPanel.add(new JLabel("Location:"));
        inputPanel.add(locationField);

        inputPanel.add(new JLabel("ICU Beds:"));
        inputPanel.add(icuField);

        inputPanel.add(new JLabel("Emergency Beds:"));
        inputPanel.add(emergencyBedsField);

        inputPanel.add(new JLabel("Ventilators:"));
        inputPanel.add(ventilatorsField);

        inputPanel.add(new JLabel("Availability:"));
        inputPanel.add(availabilityComboBox);

        inputPanel.add(new JLabel("Facilities:"));

        JPanel facilityPanel = new JPanel(
                new FlowLayout(FlowLayout.LEFT)
        );

        facilityPanel.add(traumaCheckBox);
        facilityPanel.add(cardioCheckBox);
        facilityPanel.add(neuroCheckBox);

        inputPanel.add(facilityPanel);

        for (int i = 0; i < 8; i++) {
            inputPanel.add(new JLabel(""));
        }

        topPanel.add(
                inputPanel,
                BorderLayout.CENTER
        );

        mainPanel.add(
                topPanel,
                BorderLayout.NORTH
        );

        tableModel = new DefaultTableModel(
                new String[]{
                        "ID",
                        "Name",
                        "Location",
                        "ICU",
                        "Emergency Beds",
                        "Ventilators",
                        "Trauma",
                        "Cardio",
                        "Neuro",
                        "Availability"
                },
                0
        ) {
            @Override
            public boolean isCellEditable(
                    int row,
                    int column
            ) {
                return false;
            }
        };

        hospitalTable = new JTable(tableModel);

        hospitalTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        hospitalTable.setRowHeight(22);

        hospitalTable.setAutoResizeMode(
                JTable.AUTO_RESIZE_ALL_COLUMNS
        );

        JScrollPane scrollPane =
                new JScrollPane(hospitalTable);

        mainPanel.add(
                scrollPane,
                BorderLayout.CENTER
        );

        JButton addButton =
                new JButton("Add Hospital");

        JButton viewButton =
                new JButton("View Hospitals");

        JButton updateButton =
                new JButton("Update Hospital");

        JButton deleteButton =
                new JButton("Delete Hospital");

        JButton availabilityButton =
                new JButton("Update Availability");

        JButton clearButton =
                new JButton("Clear");

        JPanel buttonPanel = new JPanel(
                new FlowLayout(
                        FlowLayout.CENTER,
                        10,
                        5
                )
        );

        buttonPanel.add(addButton);
        buttonPanel.add(viewButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(availabilityButton);
        buttonPanel.add(clearButton);

        mainPanel.add(
                buttonPanel,
                BorderLayout.SOUTH
        );

        add(mainPanel);

        addButton.addActionListener(
                e -> addHospital()
        );

        viewButton.addActionListener(e -> {

            loadHospitals();

            JOptionPane.showMessageDialog(
                    this,
                    "Hospital records loaded successfully.",
                    "View Hospitals",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });

        updateButton.addActionListener(
                e -> updateHospital()
        );

        deleteButton.addActionListener(
                e -> deleteHospital()
        );

        availabilityButton.addActionListener(
                e -> updateAvailability()
        );

        clearButton.addActionListener(
                e -> clearFields()
        );

        hospitalTable.addMouseListener(
                new MouseAdapter() {

                    @Override
                    public void mouseClicked(MouseEvent e) {

                        if (e.getClickCount() == 1) {
                            loadSelectedHospital();
                        }
                    }
                }
        );

        loadHospitals();
    }

    private void addHospital() {

        try {

            String name =
                    nameField.getText().trim();

            String location =
                    locationField.getText().trim();

            int icuBeds =
                    Integer.parseInt(
                            icuField.getText().trim()
                    );

            int emergencyBeds =
                    Integer.parseInt(
                            emergencyBedsField.getText().trim()
                    );

            int ventilators =
                    Integer.parseInt(
                            ventilatorsField.getText().trim()
                    );

            if (name.isEmpty() ||
                    location.isEmpty()) {

                showError(
                        "Hospital name and location are required."
                );

                return;
            }

            if (icuBeds < 0 ||
                    emergencyBeds < 0 ||
                    ventilators < 0) {

                showError(
                        "Bed and ventilator values cannot be negative."
                );

                return;
            }

            Hospital hospital =
                    new Hospital();

            hospital.setHospitalName(name);
            hospital.setLocation(location);
            hospital.setIcuBeds(icuBeds);
            hospital.setEmergencyBeds(emergencyBeds);
            hospital.setVentilators(ventilators);

            hospital.setTraumaAvailable(
                    traumaCheckBox.isSelected()
            );

            hospital.setCardioAvailable(
                    cardioCheckBox.isSelected()
            );

            hospital.setNeuroAvailable(
                    neuroCheckBox.isSelected()
            );

            hospital.setAvailability(
                    (String) availabilityComboBox.getSelectedItem()
            );

            boolean result =
                    hospitalDAO.addHospital(hospital);

            if (result) {

                JOptionPane.showMessageDialog(
                        this,
                        "Hospital registered successfully."
                );

                clearFields();
                loadHospitals();

            } else {

                showError(
                        "Hospital registration failed."
                );
            }

        } catch (NumberFormatException e) {

            showError(
                    "Enter valid numeric values."
            );
        }
    }

    private void loadHospitals() {

        tableModel.setRowCount(0);

        List<Hospital> hospitals =
                hospitalDAO.getAllHospitals();

        for (Hospital hospital : hospitals) {

            tableModel.addRow(
                    new Object[]{
                            hospital.getHospitalId(),
                            hospital.getHospitalName(),
                            hospital.getLocation(),
                            hospital.getIcuBeds(),
                            hospital.getEmergencyBeds(),
                            hospital.getVentilators(),
                            hospital.isTraumaAvailable(),
                            hospital.isCardioAvailable(),
                            hospital.isNeuroAvailable(),
                            hospital.getAvailability()
                    }
            );
        }

        hospitalTable.clearSelection();
    }

    private void loadSelectedHospital() {

        int selectedRow =
                hospitalTable.getSelectedRow();

        if (selectedRow < 0) {
            return;
        }

        idField.setText(
                tableModel.getValueAt(
                        selectedRow,
                        0
                ).toString()
        );

        nameField.setText(
                tableModel.getValueAt(
                        selectedRow,
                        1
                ).toString()
        );

        locationField.setText(
                tableModel.getValueAt(
                        selectedRow,
                        2
                ).toString()
        );

        icuField.setText(
                tableModel.getValueAt(
                        selectedRow,
                        3
                ).toString()
        );

        emergencyBedsField.setText(
                tableModel.getValueAt(
                        selectedRow,
                        4
                ).toString()
        );

        ventilatorsField.setText(
                tableModel.getValueAt(
                        selectedRow,
                        5
                ).toString()
        );

        traumaCheckBox.setSelected(
                Boolean.parseBoolean(
                        tableModel.getValueAt(
                                selectedRow,
                                6
                        ).toString()
                )
        );

        cardioCheckBox.setSelected(
                Boolean.parseBoolean(
                        tableModel.getValueAt(
                                selectedRow,
                                7
                        ).toString()
                )
        );

        neuroCheckBox.setSelected(
                Boolean.parseBoolean(
                        tableModel.getValueAt(
                                selectedRow,
                                8
                        ).toString()
                )
        );

        availabilityComboBox.setSelectedItem(
                tableModel.getValueAt(
                        selectedRow,
                        9
                ).toString()
        );
    }

    private void updateHospital() {

        try {

            int hospitalId =
                    Integer.parseInt(
                            idField.getText().trim()
                    );

            String name =
                    nameField.getText().trim();

            String location =
                    locationField.getText().trim();

            int icuBeds =
                    Integer.parseInt(
                            icuField.getText().trim()
                    );

            int emergencyBeds =
                    Integer.parseInt(
                            emergencyBedsField.getText().trim()
                    );

            int ventilators =
                    Integer.parseInt(
                            ventilatorsField.getText().trim()
                    );

            if (name.isEmpty() ||
                    location.isEmpty()) {

                showError(
                        "Hospital name and location are required."
                );

                return;
            }

            if (icuBeds < 0 ||
                    emergencyBeds < 0 ||
                    ventilators < 0) {

                showError(
                        "Bed and ventilator values cannot be negative."
                );

                return;
            }

            String availability =
                    (String) availabilityComboBox.getSelectedItem();

            Hospital hospital =
                    new Hospital();

            hospital.setHospitalId(hospitalId);
            hospital.setHospitalName(name);
            hospital.setLocation(location);
            hospital.setIcuBeds(icuBeds);
            hospital.setEmergencyBeds(emergencyBeds);
            hospital.setVentilators(ventilators);

            hospital.setTraumaAvailable(
                    traumaCheckBox.isSelected()
            );

            hospital.setCardioAvailable(
                    cardioCheckBox.isSelected()
            );

            hospital.setNeuroAvailable(
                    neuroCheckBox.isSelected()
            );

            hospital.setAvailability(
                    availability
            );

            boolean result =
                    hospitalDAO.updateHospital(hospital);

            if (result) {

                JOptionPane.showMessageDialog(
                        this,
                        "Hospital updated successfully."
                );

                loadHospitals();

            } else {

                showError(
                        "Hospital update failed."
                );
            }

        } catch (NumberFormatException e) {

            showError(
                    "Enter valid hospital ID and numeric values."
            );
        }
    }

    private void deleteHospital() {

        try {

            int hospitalId =
                    Integer.parseInt(
                            idField.getText().trim()
                    );

            int choice =
                    JOptionPane.showConfirmDialog(
                            this,
                            "Are you sure you want to delete this hospital?",
                            "Confirm Delete",
                            JOptionPane.YES_NO_OPTION
                    );

            if (choice != JOptionPane.YES_OPTION) {
                return;
            }

            boolean result =
                    hospitalDAO.deleteHospital(
                            hospitalId
                    );

            if (result) {

                JOptionPane.showMessageDialog(
                        this,
                        "Hospital deleted successfully."
                );

                clearFields();
                loadHospitals();

            } else {

                showError(
                        "Hospital deletion failed."
                );
            }

        } catch (NumberFormatException e) {

            showError(
                    "Enter a valid hospital ID."
            );
        }
    }

    private void updateAvailability() {

        try {

            int hospitalId =
                    Integer.parseInt(
                            idField.getText().trim()
                    );

            String availability =
                    (String) availabilityComboBox.getSelectedItem();

            boolean result =
                    hospitalDAO.updateAvailability(
                            hospitalId,
                            availability
                    );

            if (result) {

                JOptionPane.showMessageDialog(
                        this,
                        "Hospital availability updated successfully."
                );

                loadHospitals();

            } else {

                showError(
                        "Availability update failed."
                );
            }

        } catch (NumberFormatException e) {

            showError(
                    "Enter a valid hospital ID."
            );
        }
    }

    private void clearFields() {

        idField.setText("");
        nameField.setText("");
        locationField.setText("");
        icuField.setText("");
        emergencyBedsField.setText("");
        ventilatorsField.setText("");

        traumaCheckBox.setSelected(false);
        cardioCheckBox.setSelected(false);
        neuroCheckBox.setSelected(false);

        availabilityComboBox.setSelectedIndex(0);

        hospitalTable.clearSelection();
    }

    private void showError(String message) {

        JOptionPane.showMessageDialog(
                this,
                message,
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
}