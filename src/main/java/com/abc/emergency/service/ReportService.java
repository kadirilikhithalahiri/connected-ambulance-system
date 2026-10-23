package com.abc.emergency.service;

import com.abc.emergency.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ReportService {

    public void generateCaseReport(int caseId) {

        String sql = """
                SELECT ec.case_id, p.patient_name, ec.incident_location,
                       ec.emergency_type, ec.priority_level, ec.case_status,
                       ec.reported_at
                FROM Emergency_Cases ec
                JOIN Patients p ON ec.patient_id = p.patient_id
                WHERE ec.case_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, caseId);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    System.out.println("===== CASE REPORT =====");
                    System.out.println("Case ID: " + resultSet.getInt("case_id"));
                    System.out.println("Patient: " + resultSet.getString("patient_name"));
                    System.out.println("Location: " + resultSet.getString("incident_location"));
                    System.out.println("Emergency Type: " + resultSet.getString("emergency_type"));
                    System.out.println("Priority: " + resultSet.getString("priority_level"));
                    System.out.println("Status: " + resultSet.getString("case_status"));
                    System.out.println("Reported At: " + resultSet.getTimestamp("reported_at"));
                } else {
                    System.out.println("Case not found.");
                }
            }

        } catch (Exception e) {
            System.out.println("Error generating case report: " + e.getMessage());
        }
    }

    public void generateVitalsReport(int caseId) {

        String sql = """
                SELECT vital_id, heart_rate, systolic_bp, diastolic_bp,
                       spo2, temperature, respiratory_rate, glucose, recorded_at
                FROM Patient_Vitals
                WHERE case_id = ?
                ORDER BY recorded_at
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, caseId);

            try (ResultSet resultSet = statement.executeQuery()) {

                System.out.println("===== VITALS REPORT =====");

                boolean found = false;

                while (resultSet.next()) {
                    found = true;

                    System.out.println(
                            "Vital ID: " + resultSet.getInt("vital_id")
                            + " | HR: " + resultSet.getInt("heart_rate")
                            + " | BP: " + resultSet.getInt("systolic_bp")
                            + "/" + resultSet.getInt("diastolic_bp")
                            + " | SpO2: " + resultSet.getDouble("spo2")
                            + " | Temp: " + resultSet.getDouble("temperature")
                            + " | RR: " + resultSet.getInt("respiratory_rate")
                            + " | Glucose: " + resultSet.getDouble("glucose")
                            + " | Time: " + resultSet.getTimestamp("recorded_at")
                    );
                }

                if (!found) {
                    System.out.println("No vitals found for this case.");
                }
            }

        } catch (Exception e) {
            System.out.println("Error generating vitals report: " + e.getMessage());
        }
    }

    public void generateAmbulanceUtilizationReport() {

        String sql = """
                SELECT availability, COUNT(*) AS total
                FROM Ambulances
                GROUP BY availability
                ORDER BY availability
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            System.out.println("===== AMBULANCE UTILIZATION REPORT =====");

            while (resultSet.next()) {
                System.out.println(
                        resultSet.getString("availability")
                        + ": "
                        + resultSet.getInt("total")
                );
            }

        } catch (Exception e) {
            System.out.println("Error generating ambulance report: " + e.getMessage());
        }
    }

    public void generateHospitalReadinessReport() {

        String sql = """
                SELECT hospital_name, icu_beds, emergency_beds,
                       ventilators, availability
                FROM Hospitals
                ORDER BY hospital_name
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            System.out.println("===== HOSPITAL READINESS REPORT =====");

            while (resultSet.next()) {
                System.out.println(
                        resultSet.getString("hospital_name")
                        + " | ICU: " + resultSet.getInt("icu_beds")
                        + " | Emergency Beds: " + resultSet.getInt("emergency_beds")
                        + " | Ventilators: " + resultSet.getInt("ventilators")
                        + " | Status: " + resultSet.getString("availability")
                );
            }

        } catch (Exception e) {
            System.out.println("Error generating hospital report: " + e.getMessage());
        }
    }

    public void generateResponseTimeReport() {

        String sql = """
                SELECT COUNT(*) AS total_dispatches,
                       AVG(estimated_arrival_minutes) AS average_eta
                FROM Dispatch_Records
                WHERE dispatch_status IN ('ARRIVED', 'COMPLETED')
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            System.out.println("===== RESPONSE TIME REPORT =====");

            if (resultSet.next()) {
                System.out.println(
                        "Completed Dispatches: "
                        + resultSet.getInt("total_dispatches")
                );

                System.out.println(
                        "Average Estimated Arrival Time: "
                        + resultSet.getDouble("average_eta")
                        + " minutes"
                );
            }

        } catch (Exception e) {
            System.out.println("Error generating response time report: " + e.getMessage());
        }
    }

    public void generateDailyDashboard() {

        String sql = """
                SELECT
                    (SELECT COUNT(*) FROM Emergency_Cases
                     WHERE DATE(reported_at) = CURDATE()) AS cases_today,
                    (SELECT COUNT(*) FROM Dispatch_Records
                     WHERE DATE(dispatch_time) = CURDATE()) AS dispatches_today,
                    (SELECT COUNT(*) FROM Ambulances
                     WHERE availability = 'AVAILABLE') AS available_ambulances,
                    (SELECT COUNT(*) FROM Hospitals
                     WHERE availability = 'AVAILABLE') AS available_hospitals
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            System.out.println("===== DAILY DASHBOARD =====");

            if (resultSet.next()) {
                System.out.println("Cases Today: " + resultSet.getInt("cases_today"));
                System.out.println("Dispatches Today: " + resultSet.getInt("dispatches_today"));
                System.out.println("Available Ambulances: " + resultSet.getInt("available_ambulances"));
                System.out.println("Available Hospitals: " + resultSet.getInt("available_hospitals"));
            }

        } catch (Exception e) {
            System.out.println("Error generating daily dashboard: " + e.getMessage());
        }
    }

    public void generateMonthlyReport() {

        String sql = """
                SELECT
                    COUNT(*) AS total_cases,
                    SUM(CASE WHEN priority_level = 'CRITICAL' THEN 1 ELSE 0 END) AS critical_cases,
                    SUM(CASE WHEN priority_level = 'HIGH' THEN 1 ELSE 0 END) AS high_cases,
                    SUM(CASE WHEN priority_level = 'MODERATE' THEN 1 ELSE 0 END) AS moderate_cases,
                    SUM(CASE WHEN priority_level = 'LOW' THEN 1 ELSE 0 END) AS low_cases
                FROM Emergency_Cases
                WHERE YEAR(reported_at) = YEAR(CURDATE())
                  AND MONTH(reported_at) = MONTH(CURDATE())
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            System.out.println("===== MONTHLY EMERGENCY REPORT =====");

            if (resultSet.next()) {
                System.out.println("Total Cases: "
                        + resultSet.getInt("total_cases"));

                System.out.println("Critical Cases: "
                        + resultSet.getInt("critical_cases"));

                System.out.println("High Priority Cases: "
                        + resultSet.getInt("high_cases"));

                System.out.println("Moderate Cases: "
                        + resultSet.getInt("moderate_cases"));

                System.out.println("Low Priority Cases: "
                        + resultSet.getInt("low_cases"));
            }

        } catch (Exception e) {
            System.out.println("Error generating monthly report: " + e.getMessage());
        }
    }

    public String getCaseReport(int caseId) {

        StringBuilder sb = new StringBuilder();

        String sql = """
                SELECT ec.case_id, p.patient_name, ec.incident_location,
                       ec.emergency_type, ec.priority_level, ec.case_status,
                       ec.reported_at
                FROM Emergency_Cases ec
                JOIN Patients p ON ec.patient_id = p.patient_id
                WHERE ec.case_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, caseId);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    sb.append("========================================\n");
                    sb.append("             CASE REPORT                \n");
                    sb.append("========================================\n");
                    sb.append("Case ID        : ").append(resultSet.getInt("case_id")).append("\n");
                    sb.append("Patient        : ").append(resultSet.getString("patient_name")).append("\n");
                    sb.append("Location       : ").append(resultSet.getString("incident_location")).append("\n");
                    sb.append("Emergency Type : ").append(resultSet.getString("emergency_type")).append("\n");
                    sb.append("Priority       : ").append(resultSet.getString("priority_level")).append("\n");
                    sb.append("Status         : ").append(resultSet.getString("case_status")).append("\n");
                    sb.append("Reported At    : ").append(resultSet.getTimestamp("reported_at")).append("\n");
                    sb.append("========================================\n");
                } else {
                    sb.append("Case not found for Case ID: ").append(caseId).append("\n");
                }
            }

        } catch (Exception e) {
            sb.append("Error generating case report: ").append(e.getMessage()).append("\n");
        }

        return sb.toString();
    }

    public String getVitalsReport(int caseId) {

        StringBuilder sb = new StringBuilder();

        String sql = """
                SELECT vital_id, heart_rate, systolic_bp, diastolic_bp,
                       spo2, temperature, respiratory_rate, glucose, recorded_at
                FROM Patient_Vitals
                WHERE case_id = ?
                ORDER BY recorded_at
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, caseId);

            try (ResultSet resultSet = statement.executeQuery()) {

                sb.append("========================================================================================\n");
                sb.append("                                    VITALS REPORT                                       \n");
                sb.append("========================================================================================\n");
                sb.append(String.format("%-10s %-8s %-12s %-8s %-10s %-8s %-10s %-20s\n",
                        "Vital ID", "HR", "BP", "SpO2", "Temp", "RR", "Glucose", "Recorded At"));
                sb.append("----------------------------------------------------------------------------------------\n");

                boolean found = false;

                while (resultSet.next()) {
                    found = true;
                    String bp = resultSet.getInt("systolic_bp") + "/" + resultSet.getInt("diastolic_bp");
                    sb.append(String.format("%-10d %-8d %-12s %-8.1f %-10.1f %-8d %-10.1f %-20s\n",
                            resultSet.getInt("vital_id"),
                            resultSet.getInt("heart_rate"),
                            bp,
                            resultSet.getDouble("spo2"),
                            resultSet.getDouble("temperature"),
                            resultSet.getInt("respiratory_rate"),
                            resultSet.getDouble("glucose"),
                            resultSet.getTimestamp("recorded_at")));
                }

                if (!found) {
                    sb.append("No vitals found for Case ID: ").append(caseId).append("\n");
                }
                sb.append("========================================================================================\n");
            }

        } catch (Exception e) {
            sb.append("Error generating vitals report: ").append(e.getMessage()).append("\n");
        }

        return sb.toString();
    }

    public String getAmbulanceUtilizationReport() {

        StringBuilder sb = new StringBuilder();

        String sql = """
                SELECT availability, COUNT(*) AS total
                FROM Ambulances
                GROUP BY availability
                ORDER BY availability
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            sb.append("========================================\n");
            sb.append("     AMBULANCE UTILIZATION REPORT       \n");
            sb.append("========================================\n");
            sb.append(String.format("%-20s %-10s\n", "Availability Status", "Total"));
            sb.append("----------------------------------------\n");

            int grandTotal = 0;
            while (resultSet.next()) {
                String status = resultSet.getString("availability");
                int total = resultSet.getInt("total");
                grandTotal += total;
                sb.append(String.format("%-20s %-10d\n", status, total));
            }
            sb.append("----------------------------------------\n");
            sb.append(String.format("%-20s %-10d\n", "Total Ambulances", grandTotal));
            sb.append("========================================\n");

        } catch (Exception e) {
            sb.append("Error generating ambulance report: ").append(e.getMessage()).append("\n");
        }

        return sb.toString();
    }

    public String getHospitalReadinessReport() {

        StringBuilder sb = new StringBuilder();

        String sql = """
                SELECT hospital_name, icu_beds, emergency_beds,
                       ventilators, availability
                FROM Hospitals
                ORDER BY hospital_name
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            sb.append("========================================================================================\n");
            sb.append("                               HOSPITAL READINESS REPORT                                \n");
            sb.append("========================================================================================\n");
            sb.append(String.format("%-30s %-12s %-16s %-14s %-14s\n",
                    "Hospital Name", "ICU Beds", "Emergency Beds", "Ventilators", "Status"));
            sb.append("----------------------------------------------------------------------------------------\n");

            while (resultSet.next()) {
                sb.append(String.format("%-30s %-12d %-16d %-14d %-14s\n",
                        resultSet.getString("hospital_name"),
                        resultSet.getInt("icu_beds"),
                        resultSet.getInt("emergency_beds"),
                        resultSet.getInt("ventilators"),
                        resultSet.getString("availability")));
            }
            sb.append("========================================================================================\n");

        } catch (Exception e) {
            sb.append("Error generating hospital report: ").append(e.getMessage()).append("\n");
        }

        return sb.toString();
    }

    public String getResponseTimeReport() {

        StringBuilder sb = new StringBuilder();

        String sql = """
                SELECT COUNT(*) AS total_dispatches,
                       AVG(estimated_arrival_minutes) AS average_eta
                FROM Dispatch_Records
                WHERE dispatch_status IN ('ARRIVED', 'COMPLETED')
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            sb.append("========================================\n");
            sb.append("          RESPONSE TIME REPORT          \n");
            sb.append("========================================\n");

            if (resultSet.next()) {
                int total = resultSet.getInt("total_dispatches");
                double avgEta = resultSet.getDouble("average_eta");
                sb.append("Completed Dispatches           : ").append(total).append("\n");
                sb.append(String.format("Average Estimated Arrival Time : %.2f minutes\n", avgEta));
            }
            sb.append("========================================\n");

        } catch (Exception e) {
            sb.append("Error generating response time report: ").append(e.getMessage()).append("\n");
        }

        return sb.toString();
    }

    public String getDailyDashboard() {

        StringBuilder sb = new StringBuilder();

        String sql = """
                SELECT
                    (SELECT COUNT(*) FROM Emergency_Cases
                     WHERE DATE(reported_at) = CURDATE()) AS cases_today,
                    (SELECT COUNT(*) FROM Dispatch_Records
                     WHERE DATE(dispatch_time) = CURDATE()) AS dispatches_today,
                    (SELECT COUNT(*) FROM Ambulances
                     WHERE availability = 'AVAILABLE') AS available_ambulances,
                    (SELECT COUNT(*) FROM Hospitals
                     WHERE availability = 'AVAILABLE') AS available_hospitals
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            sb.append("========================================\n");
            sb.append("            DAILY DASHBOARD             \n");
            sb.append("========================================\n");

            if (resultSet.next()) {
                sb.append("Cases Today          : ").append(resultSet.getInt("cases_today")).append("\n");
                sb.append("Dispatches Today     : ").append(resultSet.getInt("dispatches_today")).append("\n");
                sb.append("Available Ambulances : ").append(resultSet.getInt("available_ambulances")).append("\n");
                sb.append("Available Hospitals  : ").append(resultSet.getInt("available_hospitals")).append("\n");
            }
            sb.append("========================================\n");

        } catch (Exception e) {
            sb.append("Error generating daily dashboard: ").append(e.getMessage()).append("\n");
        }

        return sb.toString();
    }

    public String getMonthlyReport() {

        StringBuilder sb = new StringBuilder();

        String sql = """
                SELECT
                    COUNT(*) AS total_cases,
                    SUM(CASE WHEN priority_level = 'CRITICAL' THEN 1 ELSE 0 END) AS critical_cases,
                    SUM(CASE WHEN priority_level = 'HIGH' THEN 1 ELSE 0 END) AS high_cases,
                    SUM(CASE WHEN priority_level = 'MODERATE' THEN 1 ELSE 0 END) AS moderate_cases,
                    SUM(CASE WHEN priority_level = 'LOW' THEN 1 ELSE 0 END) AS low_cases
                FROM Emergency_Cases
                WHERE YEAR(reported_at) = YEAR(CURDATE())
                  AND MONTH(reported_at) = MONTH(CURDATE())
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            sb.append("========================================\n");
            sb.append("        MONTHLY EMERGENCY REPORT        \n");
            sb.append("========================================\n");

            if (resultSet.next()) {
                sb.append("Total Cases          : ").append(resultSet.getInt("total_cases")).append("\n");
                sb.append("Critical Cases       : ").append(resultSet.getInt("critical_cases")).append("\n");
                sb.append("High Priority Cases  : ").append(resultSet.getInt("high_cases")).append("\n");
                sb.append("Moderate Cases       : ").append(resultSet.getInt("moderate_cases")).append("\n");
                sb.append("Low Priority Cases   : ").append(resultSet.getInt("low_cases")).append("\n");
            }
            sb.append("========================================\n");

        } catch (Exception e) {
            sb.append("Error generating monthly report: ").append(e.getMessage()).append("\n");
        }

        return sb.toString();
    }
}
