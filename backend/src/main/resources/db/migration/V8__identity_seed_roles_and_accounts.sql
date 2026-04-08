INSERT INTO roles (code, name, description, priority, redirect_path)
VALUES
    ('ADMIN', 'Administrator', 'System administrator with full access.', 100, '/dashboard'),
    ('DOCTOR', 'Doctor', 'EMRal doctor account.', 50, '/dashboard'),
    ('USER', 'User', 'Standard authenticated account.', 10, '/dashboard')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    priority = VALUES(priority),
    redirect_path = VALUES(redirect_path);

INSERT INTO user_roles (user_id, role_id)
SELECT ua.id, r.role_id
FROM user_accounts ua
JOIN roles r ON r.code = UPPER(ua.role)
WHERE ua.role IS NOT NULL
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id);

INSERT INTO user_roles (user_id, role_id)
SELECT ua.id, r.role_id
FROM user_accounts ua
JOIN roles r ON r.code = 'USER'
WHERE NOT EXISTS (
    SELECT 1
    FROM user_roles ur
    WHERE ur.user_id = ua.id
);

UPDATE user_accounts ua
JOIN (
    SELECT
        ur.user_id,
        SUBSTRING_INDEX(
            GROUP_CONCAT(r.code ORDER BY r.priority DESC, r.role_id DESC SEPARATOR ','),
            ',',
            1
        ) AS primary_role
    FROM user_roles ur
    JOIN roles r ON r.role_id = ur.role_id
    GROUP BY ur.user_id
) role_resolution ON role_resolution.user_id = ua.id
SET ua.role = role_resolution.primary_role;

UPDATE user_accounts
SET joined_date = COALESCE(joined_date, created_at);

UPDATE user_accounts
SET password = '$2a$10$H7rJHg8LGuZmnYI34VNqiu2CowocW4S4LC5iCThpZtog0eFh5oqSK'
WHERE username = 'admin.local'
  AND password = 'admin123';

UPDATE user_accounts
SET password = '$2a$10$3Ft65HRoq6ra4r8blULP0.5kjiEA/4pZjhzRYB9Loq541SUgGm9ka'
WHERE username = 'doctor.local'
  AND password = 'doctor123';
