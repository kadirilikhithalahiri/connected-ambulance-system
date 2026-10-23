package com.abc.emergency.gui;

import com.abc.emergency.dao.PatientDAO;
import com.abc.emergency.model.Patient;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class PatientFrame extends JFrame {

    private JTextField nameField;
    private JTextField ageField;
    private JComboBox<String> genderBox;
    private JTextField bloodGroupField;
    private JTextField contactField;
    private JTextArea historyArea;
    private JTextArea allergiesArea;
    private JTable patientTable;
    private DefaultTableModel tableModel;

    private PatientDAO patientDAO;
    private int selectedPatientId = -1;

    public PatientFrame() {

        patientDAO = new PatientDAO();

        setTitle("Patient Management");
        setSize(1000, 650);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );

        JLabel titleLabel = new JLabel(
                "PATIENT MANAGEMENT",
                SwingConstants.CENTER
        );
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));

        mainPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(
                new GridLayout(4, 4, 10, 10)
        );

        nameField = new JTextField();
        ageField = new JTextField();
        genderBox = new JComboBox<>(
                new String[]{"MALE", "FEMALE", "OTHER"}
        );
        bloodGroupField = new JTextField();
        contactField = new JTextField();

        historyArea = new JTextArea(3, 20);
        allergiesArea = new JTextArea(3, 20);

        formPanel.add(new JLabel("Patient Name:"));
        formPanel.add(nameField);

        formPanel.add(new JLabel("Age:"));
        formPanel.add(ageField);

        formPanel.add(new JLabel("Gender:"));
        formPanel.add(genderBox);

        formPanel.add(new JLabel("Blood Group:"));
        formPanel.add(bloodGroupField);

        formPanel.add(new JLabel("Emergency Contact:"));
        formPanel.add(contactField);

        formPanel.add(new JLabel("Medical History:"));
        formPanel.add(new JScrollPane(historyArea));

        formPanel.add(new JLabel("Allergies:"));
        formPanel.add(new JScrollPane(allergiesArea));

        formPanel.add(new JLabel("Selected Patient ID:"));
        formPanel.add(new JLabel("Select a row to edit or delete"));

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.add(formPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[]{
                        "ID",
                        "Name",
                        "Age",
                        "Gender",
                        "Blood Group",
                        "Emergency Contact"
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

        patientTable = new JTable(tableModel);
        patientTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        centerPanel.add(
                new JScrollPane(patientTable),
                BorderLayout.CENTER
        );

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        JButton addButton = new JButton("Add");
        JButton updateButton = new JButton("Update");
        JButton deleteButton = new JButton("Delete");
        JButton clearButton = new JButton("Clear");
        JButton refreshButton = new JButton("Refresh");

        JPanel buttonPanel = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 10, 5)
        );

        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(refreshButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);

        addButton.addActionListener(e -> addPatient());

        updateButton.addActionListener(e -> updatePatient());

        deleteButton.addActionListener(e -> deletePatient());

        clearButton.addActionListener(e -> clearFields());

        refreshButton.addActionListener(e -> loadPatients());

        patientTable.getSelectionModel().addListSelectionListener(
                e -> loadSelectedPatient()
        );

        loadPatients();
    }

    private void addPatient() {

        Patient patient = readPatientFromForm();

        if (patient == null) {
            return;
        }

        boolean result = patientDAO.addPatient(patient);

        if (result) {
            JOptionPane.showMessageDialog(
                    this,
                    "Patient added successfully.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
            );

            clearFields();
            loadPatients();

        } else {
            JOptionPane.showMessageDialog(
                    this,
                    "Failed to add patient.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void updatePatient() {

        if (selectedPatientId == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "Select a patient first.",
                    "Update Error",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        Patient patient = readPatientFromForm();

        if (patient == null) {
            return;
        }

        patient.setPatientId(selectedPatientId);

        boolean result = patientDAO.updatePatient(patient);

        if (result) {
            JOptionPane.showMessageDialog(
                    this,
                    "Patient updated successfully.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
            );

            clearFields();
            loadPatients();

        } else {
            JOptionPane.showMessageDialog(
                    this,
                    "Failed to update patient.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void deletePatient() {

        if (selectedPatientId == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "Select a patient first.",
                    "Delete Error",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Delete selected patient?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        boolean result =
                patientDAO.deletePatient(selectedPatientId);

        if (result) {
            JOptionPane.showMessageDialog(
                    this,
                    "Patient deleted successfully.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
            );

            clearFields();
            loadPatients();

        } else {
            JOptionPane.showMessageDialog(
                    this,
                    "Failed to delete patient.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private Patient readPatientFromForm() {

        String name = nameField.getText().trim();
        String ageText = ageField.getText().trim();
        String gender =
                (String) genderBox.getSelectedItem();
        String bloodGroup =
                bloodGroupField.getText().trim();
        String contact =
                contactField.getText().trim();
        String history =
                historyArea.getText().trim();
        String allergies =
                allergiesArea.getText().trim();

        if (name.isEmpty()) {
            showError("Patient name cannot be empty.");
            return null;
        }

        if (ageText.isEmpty()) {
            showError("Age cannot be empty.");
            return null;
        }

        int age;

        try {
            age = Integer.parseInt(ageText);
        } catch (NumberFormatException e) {
            showError("Age must be a valid number.");
            return null;
        }

        if (age <= 0 || age > 120) {
            showError("Age must be between 1 and 120.");
            return null;
        }

        if (contact.isEmpty()) {
            showError("Emergency contact cannot be empty.");
            return null;
        }

        Patient patient = new Patient();

        patient.setPatientName(name);
        patient.setAge(age);
        patient.setGender(gender);
        patient.setBloodGroup(bloodGroup);
        patient.setMedicalHistory(history);
        patient.setAllergies(allergies);
        patient.setEmergencyContact(contact);

        return patient;
    }

    private void loadPatients() {

        tableModel.setRowCount(0);

        List<Patient> patients =
                patientDAO.getAllPatients();

        for (Patient patient : patients) {

            tableModel.addRow(
                    new Object[]{
                            patient.getPatientId(),
                            patient.getPatientName(),
                            patient.getAge(),
                            patient.getGender(),
                            patient.getBloodGroup(),
                            patient.getEmergencyContact()
                    }
            );
        }
    }

    private void loadSelectedPatient() {

        int row = patientTable.getSelectedRow();

        if (row == -1) {
            return;
        }

        selectedPatientId =
                (int) tableModel.getValueAt(row, 0);

        Patient patient =
                patientDAO.getPatientById(selectedPatientId);

        if (patient == null) {
            return;
        }

        nameField.setText(patient.getPatientName());
        ageField.setText(
                String.valueOf(patient.getAge())
        );
        genderBox.setSelectedItem(patient.getGender());
        bloodGroupField.setText(
                patient.getBloodGroup()
        );
        contactField.setText(
                patient.getEmergencyContact()
        );
        historyArea.setText(
                patient.getMedicalHistory()
        );
        allergiesArea.setText(
                patient.getAllergies()
        );
    }

    private void clearFields() {

        selectedPatientId = -1;

        nameField.setText("");
        ageField.setText("");
        genderBox.setSelectedIndex(0);
        bloodGroupField.setText("");
        contactField.setText("");
        historyArea.setText("");
        allergiesArea.setText("");

        patientTable.clearSelection();
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