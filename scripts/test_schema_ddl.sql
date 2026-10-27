-- =============================================================================
-- DRAFT ONLY - FOR STATIC CODE AUDIT AND REVIEW ONLY
-- DO NOT EXECUTE AGAINST ANY MYSQL INSTANCE OR DATABASE
-- THIS SCRIPT HAS NOT BEEN RUNTIME-VALIDATED
-- =============================================================================
-- Phase 5I.19: Draft isolated test schema DDL for connected_ambulance_test.
-- Derived strictly from static inspection of Java DAO SQL and model structures:
--   - Users: UserDAO.java / User.java
--   - Patients: PatientDAO.java / Patient.java
--   - Ambulances: AmbulanceDAO.java / Ambulance.java
--   - Ambulance_Crew: AmbulanceCrewDAO.java
--   - Emergency_Cases: EmergencyCaseDAO.java / EmergencyCase.java
--   - Dispatch_Records: DispatchRecordDAO.java / DispatchRecord.java
--   - Ambulance_Tracking: AmbulanceTrackingDAO.java
--   - Login_Log: LoginService.java
--
-- Concurrency & Safety Controls:
--   1. ENGINE=InnoDB explicitly declared on every table to support multi-table
--      transactions (connection.setAutoCommit(false), commit, rollback).
--   2. Explicit primary keys defined for deterministic seed row identification.
--   3. Foreign key constraints enforce relational integrity; nullable foreign
--      keys permit unassigned vehicle assignments without constraint violations.
--   4. Execution is strictly prohibited until Gate 1 approval is granted.
-- =============================================================================

-- Non-executable sentinel: deliberate syntax stop to prevent accidental execution
DO_NOT_EXECUTE_UNAPPROVED_DRAFT_DDL;

-- -----------------------------------------------------------------------------
-- 1. Table: Users
-- Source: UserDAO.java (lines 16-18, 42-45), User.java (lines 5-12)
-- Columns confirmed: user_id, username, password_hash, full_name, role, phone, active
-- Note: Uses 'phone' (not phone_number), 'active' (not is_active), no email column.
-- -----------------------------------------------------------------------------
CREATE TABLE Users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL,
    phone VARCHAR(20) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    INDEX idx_users_role_active (role, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 2. Table: Patients
-- Source: PatientDAO.java (lines 28-30, 114-123, 176-178), Patient.java (lines 5-13)
-- Columns confirmed: patient_id, patient_name, age, gender, blood_group,
--                    medical_history, allergies, emergency_contact
-- Note: Uses 'emergency_contact' (not contact_number). No registered_at column.
-- -----------------------------------------------------------------------------
CREATE TABLE Patients (
    patient_id INT AUTO_INCREMENT PRIMARY KEY,
    patient_name VARCHAR(100) NOT NULL,
    age INT NOT NULL,
    gender VARCHAR(10) NOT NULL,
    blood_group VARCHAR(10) NULL,
    medical_history TEXT NULL,
    allergies TEXT NULL,
    emergency_contact VARCHAR(20) NULL,
    INDEX idx_patients_name (patient_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 3. Table: Ambulances
-- Source: AmbulanceDAO.java (lines 15-18, 110-118, 165, 177), Ambulance.java (lines 5-13)
-- Columns confirmed: ambulance_id, vehicle_number, driver_id, paramedic_id,
--                    gps_latitude, gps_longitude, equipment, availability
-- Note: Uses 'vehicle_number' (not registration_number). No model column.
--       driver_id and paramedic_id are nullable to permit unassigned vehicles.
-- -----------------------------------------------------------------------------
CREATE TABLE Ambulances (
    ambulance_id INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_number VARCHAR(50) NOT NULL UNIQUE,
    driver_id INT NULL,
    paramedic_id INT NULL,
    gps_latitude DOUBLE NOT NULL DEFAULT 0.0,
    gps_longitude DOUBLE NOT NULL DEFAULT 0.0,
    equipment VARCHAR(255) NULL,
    availability VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    INDEX idx_ambulances_availability (availability),
    INDEX idx_ambulances_driver (driver_id),
    INDEX idx_ambulances_paramedic (paramedic_id),
    CONSTRAINT fk_ambulances_driver FOREIGN KEY (driver_id)
        REFERENCES Users(user_id) ON DELETE SET NULL,
    CONSTRAINT fk_ambulances_paramedic FOREIGN KEY (paramedic_id)
        REFERENCES Users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 4. Table: Ambulance_Crew
-- Source: AmbulanceCrewDAO.java (lines 12, 36, 63, 82-87, 112)
-- Columns confirmed: crew_id, ambulance_id, user_id, crew_role, assigned_from, assigned_to
-- Note: active crew has assigned_to IS NULL. Ended crew has assigned_to set.
-- -----------------------------------------------------------------------------
CREATE TABLE Ambulance_Crew (
    crew_id INT AUTO_INCREMENT PRIMARY KEY,
    ambulance_id INT NOT NULL,
    user_id INT NOT NULL,
    crew_role VARCHAR(30) NOT NULL,
    assigned_from TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_to TIMESTAMP NULL DEFAULT NULL,
    INDEX idx_crew_ambulance_active (ambulance_id, assigned_to),
    INDEX idx_crew_user_role_active (user_id, crew_role, assigned_to),
    CONSTRAINT fk_crew_ambulance FOREIGN KEY (ambulance_id)
        REFERENCES Ambulances(ambulance_id) ON DELETE RESTRICT,
    CONSTRAINT fk_crew_user FOREIGN KEY (user_id)
        REFERENCES Users(user_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 5. Table: Emergency_Cases
-- Source: EmergencyCaseDAO.java (lines 28, 85, 120-123, 140-142), EmergencyCase.java
-- Columns confirmed: case_id, patient_id, incident_location, emergency_type,
--                    symptoms, priority_level, case_status, reported_at
-- Note: Uses 'reported_at' (not created_at) per ORDER BY reported_at DESC in DAO.
-- -----------------------------------------------------------------------------
CREATE TABLE Emergency_Cases (
    case_id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    incident_location VARCHAR(255) NOT NULL,
    emergency_type VARCHAR(50) NOT NULL,
    symptoms TEXT NULL,
    priority_level VARCHAR(20) NOT NULL,
    case_status VARCHAR(20) NOT NULL DEFAULT 'REGISTERED',
    reported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_cases_patient (patient_id),
    INDEX idx_cases_status (case_status),
    INDEX idx_cases_reported (reported_at),
    CONSTRAINT fk_cases_patient FOREIGN KEY (patient_id)
        REFERENCES Patients(patient_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 6. Table: Dispatch_Records
-- Source: DispatchRecordDAO.java (lines 37, 85, 206, 239, 252), DispatchRecord.java
-- Columns confirmed: dispatch_id, case_id, ambulance_id, estimated_arrival_minutes,
--                    dispatch_status, dispatch_time, actual_arrival_time
-- Note: Uses 'dispatch_time' (not dispatched_at) and 'actual_arrival_time' (not arrived_at).
--       No completed_at column exists in DAO or entity.
-- -----------------------------------------------------------------------------
CREATE TABLE Dispatch_Records (
    dispatch_id INT AUTO_INCREMENT PRIMARY KEY,
    case_id INT NOT NULL,
    ambulance_id INT NOT NULL,
    estimated_arrival_minutes INT NOT NULL,
    dispatch_status VARCHAR(20) NOT NULL DEFAULT 'DISPATCHED',
    dispatch_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actual_arrival_time TIMESTAMP NULL DEFAULT NULL,
    INDEX idx_dispatches_case (case_id),
    INDEX idx_dispatches_amb_status (ambulance_id, dispatch_status),
    INDEX idx_dispatches_time (dispatch_time),
    CONSTRAINT fk_dispatches_case FOREIGN KEY (case_id)
        REFERENCES Emergency_Cases(case_id) ON DELETE RESTRICT,
    CONSTRAINT fk_dispatches_ambulance FOREIGN KEY (ambulance_id)
        REFERENCES Ambulances(ambulance_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 7. Table: Ambulance_Tracking
-- Source: AmbulanceTrackingDAO.java (lines 14-16, 37-40), AmbulanceService.java (line 280)
-- Columns confirmed: tracking_id, ambulance_id, latitude, longitude, speed, status, recorded_at
-- -----------------------------------------------------------------------------
CREATE TABLE Ambulance_Tracking (
    tracking_id INT AUTO_INCREMENT PRIMARY KEY,
    ambulance_id INT NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    speed DOUBLE NOT NULL,
    status VARCHAR(50) NULL,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tracking_amb_recorded (ambulance_id, recorded_at),
    CONSTRAINT fk_tracking_ambulance FOREIGN KEY (ambulance_id)
        REFERENCES Ambulances(ambulance_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 8. Table: Login_Log
-- Source: LoginService.java (lines 108-115, 145-148)
-- Columns confirmed: log_id, user_id, login_status, login_time, logout_time
-- Note: Stores user_id (not username). Populated dynamically on login/logout.
-- -----------------------------------------------------------------------------
CREATE TABLE Login_Log (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    login_status VARCHAR(20) NOT NULL,
    login_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    logout_time TIMESTAMP NULL DEFAULT NULL,
    INDEX idx_login_log_user_status (user_id, login_status, logout_time),
    CONSTRAINT fk_login_log_user FOREIGN KEY (user_id)
        REFERENCES Users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
