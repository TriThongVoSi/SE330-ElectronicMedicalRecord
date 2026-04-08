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
    password_changed_at,
    first_login_completed_at,
    linked_patient_id
)
VALUES (
    '20000000-0000-0000-0000-000000000003',
    'patient.local',
    '$2a$10$P6Rz3QVygmwWEDUOOmY61OTTM3Fb6L3.ZFMjxcDERQuEg3tkneToC',
    'john.smith@EMR.dev',
    'John Smith',
    'Male',
    'PATIENT',
    TRUE,
    'ACTIVE',
    NOW(),
    FALSE,
    NOW(),
    NOW(),
    '30000000-0000-0000-0000-000000000001'
)
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    role = 'PATIENT',
    is_active = TRUE,
    status = 'ACTIVE',
    must_change_password = FALSE,
    temp_password_issued_at = NULL,
    password_changed_at = NOW(),
    first_login_completed_at = NOW(),
    linked_patient_id = COALESCE(linked_patient_id, VALUES(linked_patient_id));

INSERT INTO user_roles (user_id, role_id)
SELECT '20000000-0000-0000-0000-000000000003', role_id
FROM roles
WHERE code = 'PATIENT'
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id);
