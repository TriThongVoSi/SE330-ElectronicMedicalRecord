CREATE INDEX idx_user_accounts_role_is_active
    ON user_accounts (role, is_active);

CREATE INDEX idx_patients_phone
    ON patients (phone);

CREATE INDEX idx_patients_full_name
    ON patients (full_name);

CREATE INDEX idx_appointments_patient_id_appointment_time
    ON appointments (patient_id, appointment_time);

CREATE INDEX idx_appointments_status_appointment_time
    ON appointments (status, appointment_time);

CREATE INDEX idx_available_slots_doctor_id_slot_date_is_booked
    ON available_slots (doctor_id, slot_date, is_booked);

CREATE INDEX idx_prescriptions_patient_id_status
    ON prescriptions (patient_id, status);

CREATE INDEX idx_prescriptions_doctor_id_status
    ON prescriptions (doctor_id, status);

CREATE INDEX idx_drugs_drug_name
    ON drugs (drug_name);
