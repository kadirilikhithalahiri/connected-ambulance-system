-- =============================================================================
-- DRAFT ONLY - FOR STATIC CODE AUDIT AND REVIEW ONLY
-- DO NOT EXECUTE AGAINST ANY MYSQL INSTANCE OR DATABASE
-- NON-EXECUTABLE DRAFT: AUTHENTICATION CREDENTIALS ARE UNRESOLVED
-- =============================================================================
-- Phase 5I.19: Draft baseline seed dataset for connected_ambulance_test.
-- Inventory:
--   - 8 Users (6 active role personnel, 1 standby driver, 1 inactive account)
--   - 2 Patients (Patient Alpha undispatched; Patient Beta linked to Cases 2 & 3)
--   - 3 Ambulances (Amb 1 AVAILABLE, Amb 2 DISPATCHED, Amb 3 MAINTENANCE)
--   - 5 Crew Assignments (4 active assignments, 1 ended assignment on Amb 3)
--   - 3 Emergency Cases (Case 1 REGISTERED, Case 2 DISPATCHED, Case 3 COMPLETED)
--   - 2 Dispatch Records (Disp 1 DISPATCHED on Amb 2, Disp 2 COMPLETED on Amb 2)
--   - 0 Ambulance_Tracking records (initialized empty)
--   - 0 Login_Log records (initialized empty)
--
-- SECURITY & COMPLIANCE REQUIREMENTS (Requirement 4):
--   1. Plaintext passwords and placeholder dummy hashes are strictly omitted.
--   2. Authentication seed values are explicitly marked UNRESOLVED.
--   3. The draft contains an intentional non-executable sentinel below so that
--      accidental command-line or GUI pipe execution fails immediately.
--   4. Valid BCrypt hashes matching test credentials (e.g. AdminPass123!) must
--      be generated via com.abc.emergency.util.PasswordUtil.hashPassword()
--      during an explicitly approved credential provisioning step.
-- =============================================================================

-- Non-executable sentinel: deliberate syntax stop to prevent accidental execution
DO_NOT_EXECUTE_UNAPPROVED_DRAFT_SEED;

-- -----------------------------------------------------------------------------
-- 1. Table: Users (8 Seed Rows)
-- Target: Users(user_id, username, password_hash, full_name, role, phone, active)
-- NOTE: password_hash values are left as non-executable tokens. They MUST be
--       replaced by approved BCrypt hashes prior to Gate 1 execution.
-- -----------------------------------------------------------------------------
INSERT INTO Users (user_id, username, password_hash, full_name, role, phone, active) VALUES
(101, 'admin_test',          <UNRESOLVED_BCRYPT_HASH_ADMIN_TEST>,          'System Administrator',         'ADMIN',            '9000000001', TRUE),
(102, 'dispatcher_test',     <UNRESOLVED_BCRYPT_HASH_DISPATCHER_TEST>,     'Primary Dispatcher',           'DISPATCHER',       '9000000002', TRUE),
(103, 'paramedic_test',      <UNRESOLVED_BCRYPT_HASH_PARAMEDIC_TEST>,      'Lead Paramedic',               'PARAMEDIC',        '9000000003', TRUE),
(104, 'doctor_test',         <UNRESOLVED_BCRYPT_HASH_DOCTOR_TEST>,         'Dr. Robert Smith',             'EMERGENCY_DOCTOR', '9000000004', TRUE),
(105, 'driver_active',       <UNRESOLVED_BCRYPT_HASH_DRIVER_ACTIVE>,       'Active Mission Driver',        'AMBULANCE_DRIVER', '9000000005', TRUE),
(106, 'hospital_admin_test', <UNRESOLVED_BCRYPT_HASH_HOSPITAL_ADMIN_TEST>, 'Hospital Operations Director', 'HOSPITAL_ADMIN',   '9000000006', TRUE),
(107, 'driver_standby',      <UNRESOLVED_BCRYPT_HASH_DRIVER_STANDBY>,      'Standby Fleet Driver',         'AMBULANCE_DRIVER', '9000000007', TRUE),
(108, 'inactive_user',       <UNRESOLVED_BCRYPT_HASH_INACTIVE_USER>,       'Deactivated Account',          'PARAMEDIC',        '9000000008', FALSE);

-- -----------------------------------------------------------------------------
-- 2. Table: Patients (2 Seed Rows)
-- Target: Patients(patient_id, patient_name, age, gender, blood_group, medical_history, allergies, emergency_contact)
-- -----------------------------------------------------------------------------
INSERT INTO Patients (patient_id, patient_name, age, gender, blood_group, medical_history, allergies, emergency_contact) VALUES
(1, 'Patient Alpha', 45, 'Male',   'A+', 'Hypertension (managed with ACE inhibitors)', 'None known', '9876543210'),
(2, 'Patient Beta',  62, 'Female', 'O+', 'Type 2 Diabetes, Coronary Artery Disease',   'Penicillin', '9876543211');

-- -----------------------------------------------------------------------------
-- 3. Table: Ambulances (3 Seed Rows)
-- Target: Ambulances(ambulance_id, vehicle_number, driver_id, paramedic_id, gps_latitude, gps_longitude, equipment, availability)
-- Relational consistency:
--   - Ambulance 1: AVAILABLE. Primary driver 107 (driver_standby), paramedic 103. 0 active dispatches.
--   - Ambulance 2: DISPATCHED. Primary driver 105 (driver_active), paramedic 103. 1 active dispatch (Disp 1).
--   - Ambulance 3: MAINTENANCE. driver_id and paramedic_id are NULL (unassigned).
-- -----------------------------------------------------------------------------
INSERT INTO Ambulances (ambulance_id, vehicle_number, driver_id, paramedic_id, gps_latitude, gps_longitude, equipment, availability) VALUES
(1, 'KA-01-EA-1001', 107, 103, 12.9716, 77.5946, 'Defibrillator, Ventilator, Oxygen Kit, Advanced Life Support', 'AVAILABLE'),
(2, 'KA-01-EA-1002', 105, 103, 12.9352, 77.6245, 'Defibrillator, Basic Life Support, First Aid Kit',            'DISPATCHED'),
(3, 'KA-01-EA-1003', NULL, NULL, 12.9279, 77.6271, 'Patient Transport Equipment',                                'MAINTENANCE');

-- -----------------------------------------------------------------------------
-- 4. Table: Ambulance_Crew (5 Seed Rows)
-- Target: Ambulance_Crew(crew_id, ambulance_id, user_id, crew_role, assigned_from, assigned_to)
-- Relational consistency:
--   - Amb 1 active crew: User 103 (PARAMEDIC), User 107 (DRIVER).
--   - Amb 2 active crew: User 103 (PARAMEDIC), User 105 (DRIVER).
--   - Amb 3 historical crew: User 103 (PARAMEDIC) ended (assigned_to populated).
-- -----------------------------------------------------------------------------
INSERT INTO Ambulance_Crew (crew_id, ambulance_id, user_id, crew_role, assigned_from, assigned_to) VALUES
(1, 1, 103, 'PARAMEDIC', '2026-10-25 06:00:00', NULL),
(2, 1, 107, 'DRIVER',    '2026-10-25 06:00:00', NULL),
(3, 2, 103, 'PARAMEDIC', '2026-10-25 06:00:00', NULL),
(4, 2, 105, 'DRIVER',    '2026-10-25 06:00:00', NULL),
(5, 3, 103, 'PARAMEDIC', '2026-10-01 08:00:00', '2026-10-01 18:00:00');

-- -----------------------------------------------------------------------------
-- 5. Table: Emergency_Cases (3 Seed Rows)
-- Target: Emergency_Cases(case_id, patient_id, incident_location, emergency_type, symptoms, priority_level, case_status, reported_at)
-- Relational consistency:
--   - Case 1: Linked to Patient 1. REGISTERED. Undispatched (anti-enumeration check).
--   - Case 2: Linked to Patient 2. DISPATCHED. Active mission on Ambulance 2.
--   - Case 3: Linked to Patient 2. COMPLETED. Resolved mission on Ambulance 2.
-- -----------------------------------------------------------------------------
INSERT INTO Emergency_Cases (case_id, patient_id, incident_location, emergency_type, symptoms, priority_level, case_status, reported_at) VALUES
(1, 1, '100 Feet Rd, Indiranagar, Bengaluru',  'Cardiac Emergency',              'Sudden chest pressure radiating to left arm, shortness of breath', 'HIGH',     'REGISTERED', '2026-10-25 08:30:00'),
(2, 2, 'MG Road Metro Station, Bengaluru',      'Trauma / Road Traffic Accident', 'Blunt abdominal trauma, hypotensive, altered sensorium',           'CRITICAL', 'DISPATCHED', '2026-10-25 08:45:00'),
(3, 2, 'Whitefield Main Road, Bengaluru',       'Respiratory Distress',           'Acute exacerbation of asthma, audible wheezing, tachypnea',        'MODERATE', 'COMPLETED',  '2026-10-25 06:30:00');

-- -----------------------------------------------------------------------------
-- 6. Table: Dispatch_Records (2 Seed Rows)
-- Target: Dispatch_Records(dispatch_id, case_id, ambulance_id, estimated_arrival_minutes, dispatch_status, dispatch_time, actual_arrival_time)
-- Relational consistency:
--   - Dispatch 1: Case 2, Ambulance 2. DISPATCHED. Active mission. actual_arrival_time is NULL.
--   - Dispatch 2: Case 3, Ambulance 2. COMPLETED. Historical mission. actual_arrival_time recorded.
-- -----------------------------------------------------------------------------
INSERT INTO Dispatch_Records (dispatch_id, case_id, ambulance_id, estimated_arrival_minutes, dispatch_status, dispatch_time, actual_arrival_time) VALUES
(1, 2, 2, 12, 'DISPATCHED', '2026-10-25 08:47:00', NULL),
(2, 3, 2, 15, 'COMPLETED',  '2026-10-25 06:35:00', '2026-10-25 06:48:00');

-- -----------------------------------------------------------------------------
-- 7. Table: Ambulance_Tracking (0 Seed Rows)
-- Target: Ambulance_Tracking
-- Starts strictly empty (0 rows). Populated dynamically on location updates.
-- -----------------------------------------------------------------------------
-- (No initial seed rows)

-- -----------------------------------------------------------------------------
-- 8. Table: Login_Log (0 Seed Rows)
-- Target: Login_Log
-- Starts strictly empty (0 rows). Populated dynamically on user login/logout.
-- -----------------------------------------------------------------------------
-- (No initial seed rows)
