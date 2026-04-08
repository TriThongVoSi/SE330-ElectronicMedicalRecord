INSERT INTO services (id, service_code, service_name, service_type, is_active)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'SRV-GEN-CHECK', 'General Psychological Checkup', 'EXAMINATION', TRUE),
    ('10000000-0000-0000-0000-000000000002', 'SRV-CBT', 'Cognitive Behavioral Therapy', 'EXAMINATION', TRUE),
    ('10000000-0000-0000-0000-000000000003', 'SRV-BLOOD', 'Basic Blood Test', 'TEST', TRUE);

INSERT INTO user_accounts (id, username, password, email, full_name, gender, role, is_active)
VALUES
    ('20000000-0000-0000-0000-000000000001', 'admin.local', 'admin123', 'admin.local@EMR.dev', 'Local Admin', 'Male', 'ADMIN', TRUE),
    ('20000000-0000-0000-0000-000000000002', 'doctor.local', 'doctor123', 'doctor.local@EMR.dev', 'Dr. Local Demo', 'Female', 'DOCTOR', TRUE);

INSERT INTO admins (admin_id)
VALUES ('20000000-0000-0000-0000-000000000001');

INSERT INTO doctors (doctor_id, phone, service_id, address, is_confirmed)
VALUES ('20000000-0000-0000-0000-000000000002', '0901000002', '10000000-0000-0000-0000-000000000001', '12 Medical Lane, District 1', TRUE);

INSERT INTO patients (patient_id, patient_code, full_name, date_of_birth, email, gender, phone, address, diagnosis, height_cm, weight_kg)
VALUES
    ('30000000-0000-0000-0000-000000000001', 'PT-0001', 'John Smith', '1980-04-15', 'john.smith@EMR.dev', 'Male', '0902000001', '123 Main St', 'General anxiety', 178.00, 74.50),
    ('30000000-0000-0000-0000-000000000002', 'PT-0002', 'Emily Johnson', '1990-07-22', 'emily.johnson@EMR.dev', 'Female', '0902000002', '456 Park Ave', 'Sleep disorder', 165.50, 60.20);

INSERT INTO appointments (
    appointment_id,
    appointment_code,
    appointment_time,
    status,
    cancel_reason,
    doctor_id,
    patient_id,
    urgency_level,
    prescription_status,
    is_followup,
    priority_score
)
VALUES
    ('40000000-0000-0000-0000-000000000001', 'AP-20260401-0001', '2026-04-03 09:00:00', 'COMING', NULL, '20000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000001', 2, 'NONE', FALSE, 8),
    ('40000000-0000-0000-0000-000000000002', 'AP-20260401-0002', '2026-04-02 14:30:00', 'FINISH', NULL, '20000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002', 1, 'CREATED', FALSE, 6);

INSERT INTO appointment_services (appointment_id, service_id)
VALUES
    ('40000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001'),
    ('40000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002');

INSERT INTO available_slots (slot_id, doctor_id, slot_date, slot_time, duration_minutes, is_booked, appointment_id)
VALUES
    ('50000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', '2026-04-03', '09:00:00', 15, TRUE, '40000000-0000-0000-0000-000000000001'),
    ('50000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', '2026-04-03', '09:15:00', 15, FALSE, NULL);

INSERT INTO drugs (drug_id, drug_code, drug_name, manufacturer, expiry_date, unit, price, stock_quantity)
VALUES
    ('60000000-0000-0000-0000-000000000001', 'DRUG-PARACETAMOL-500', 'Paracetamol 500mg', 'ABC Pharma', '2027-12-31', 'TABLET', 1500.00, 200),
    ('60000000-0000-0000-0000-000000000002', 'DRUG-AMOX-250', 'Amoxicillin 250mg', 'XYZ Healthcare', '2027-06-30', 'CAPSULE', 2500.00, 120);

INSERT INTO prescriptions (
    prescription_id,
    prescription_code,
    patient_id,
    doctor_id,
    appointment_id,
    diagnosis,
    advice,
    status
)
VALUES (
    '70000000-0000-0000-0000-000000000001',
    'RX-20260401-0001',
    '30000000-0000-0000-0000-000000000002',
    '20000000-0000-0000-0000-000000000002',
    '40000000-0000-0000-0000-000000000002',
    'Mild respiratory infection',
    'Take medicines after meals and return in 3 days if symptoms persist.',
    'CREATED'
);

INSERT INTO prescription_items (prescription_id, drug_id, quantity, instructions)
VALUES
    ('70000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', 2, 'Take 1 tablet twice daily after meals.'),
    ('70000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000002', 2, 'Take 1 capsule twice daily for 5 days.');
