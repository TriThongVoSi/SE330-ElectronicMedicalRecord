CREATE TABLE audit_logs (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    event_id CHAR(36) NOT NULL,
    request_id VARCHAR(64) NULL,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor_username VARCHAR(100) NULL,
    actor_role VARCHAR(40) NULL,
    module_name VARCHAR(50) NOT NULL,
    action_name VARCHAR(120) NOT NULL,
    target_type VARCHAR(80) NULL,
    target_id VARCHAR(80) NULL,
    outcome VARCHAR(20) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    request_path VARCHAR(255) NOT NULL,
    status_code INT NOT NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(255) NULL,
    details TEXT NULL,
    CONSTRAINT pk_audit_logs PRIMARY KEY (id),
    CONSTRAINT uk_audit_logs_event_id UNIQUE (event_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_audit_logs_occurred_at
    ON audit_logs (occurred_at);

CREATE INDEX idx_audit_logs_actor_username_occurred_at
    ON audit_logs (actor_username, occurred_at);

CREATE INDEX idx_audit_logs_module_name_action_name_occurred_at
    ON audit_logs (module_name, action_name, occurred_at);

CREATE INDEX idx_audit_logs_outcome_occurred_at
    ON audit_logs (outcome, occurred_at);

CREATE INDEX idx_audit_logs_request_id
    ON audit_logs (request_id);

CREATE INDEX idx_patients_created_at
    ON patients (created_at);

CREATE INDEX idx_appointments_doctor_id_status_appointment_time
    ON appointments (doctor_id, status, appointment_time);

CREATE INDEX idx_appointments_patient_id_status_appointment_time
    ON appointments (patient_id, status, appointment_time);

CREATE INDEX idx_prescriptions_status_created_at
    ON prescriptions (status, created_at);
