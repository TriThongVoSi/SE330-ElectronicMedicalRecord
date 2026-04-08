ALTER TABLE user_accounts
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE AFTER status;

ALTER TABLE user_accounts
    ADD COLUMN temp_password_issued_at DATETIME NULL AFTER must_change_password;

ALTER TABLE user_accounts
    ADD COLUMN password_changed_at DATETIME NULL AFTER temp_password_issued_at;

ALTER TABLE user_accounts
    ADD COLUMN first_login_completed_at DATETIME NULL AFTER password_changed_at;

ALTER TABLE user_accounts
    ADD COLUMN linked_patient_id CHAR(36) NULL AFTER first_login_completed_at;

ALTER TABLE user_accounts
    ADD CONSTRAINT fk_user_accounts_linked_patient_id
        FOREIGN KEY (linked_patient_id)
            REFERENCES patients (patient_id)
            ON DELETE SET NULL;

CREATE UNIQUE INDEX uk_user_accounts_linked_patient_id
    ON user_accounts (linked_patient_id);

ALTER TABLE patients
    ADD COLUMN drug_allergies TEXT NULL AFTER diagnosis;

UPDATE roles
SET code = 'PATIENT',
    name = 'Patient',
    description = 'Patient portal account.',
    priority = 20
WHERE code = 'USER'
  AND NOT EXISTS (
      SELECT 1
      FROM (SELECT code FROM roles WHERE code = 'PATIENT') AS role_guard
  );

INSERT INTO roles (code, name, description, priority, redirect_path)
VALUES ('PATIENT', 'Patient', 'Patient portal account.', 20, '/patient/dashboard')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    priority = VALUES(priority),
    redirect_path = VALUES(redirect_path);

UPDATE user_accounts
SET role = 'PATIENT'
WHERE role = 'USER';

INSERT INTO user_roles (user_id, role_id)
SELECT ua.id, r.role_id
FROM user_accounts ua
JOIN roles r ON r.code = 'PATIENT'
WHERE ua.role = 'PATIENT'
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id);

INSERT INTO user_accounts (
    id,
    username,
    password,
    email,
    full_name,
    gender,
    role,
    is_active,
    status,
    joined_date,
    must_change_password,
    temp_password_issued_at,
    linked_patient_id
)
VALUES (
    '20000000-0000-0000-0000-000000000003',
    'patient.local',
    '$2a$10$3Ft65HRoq6ra4r8blULP0.5kjiEA/4pZjhzRYB9Loq541SUgGm9ka',
    'john.smith@EMR.dev',
    'John Smith',
    'Male',
    'PATIENT',
    TRUE,
    'ACTIVE',
    NOW(),
    TRUE,
    NOW(),
    '30000000-0000-0000-0000-000000000001'
)
ON DUPLICATE KEY UPDATE
    role = VALUES(role),
    is_active = VALUES(is_active),
    status = VALUES(status),
    must_change_password = VALUES(must_change_password),
    temp_password_issued_at = VALUES(temp_password_issued_at),
    linked_patient_id = VALUES(linked_patient_id);

INSERT INTO user_roles (user_id, role_id)
SELECT '20000000-0000-0000-0000-000000000003', role_id
FROM roles
WHERE code = 'PATIENT'
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id);
