ALTER TABLE user_accounts
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' AFTER role;

ALTER TABLE user_accounts
    ADD COLUMN locked_until DATETIME NULL AFTER status;

ALTER TABLE user_accounts
    ADD COLUMN google_id VARCHAR(128) NULL AFTER locked_until;

ALTER TABLE user_accounts
    ADD COLUMN joined_date DATETIME NULL AFTER google_id;

UPDATE user_accounts
SET status = CASE
    WHEN is_active = TRUE THEN 'ACTIVE'
    ELSE 'INACTIVE'
END;

UPDATE user_accounts
SET joined_date = COALESCE(joined_date, created_at);

CREATE UNIQUE INDEX uk_user_accounts_google_id
    ON user_accounts (google_id);

CREATE INDEX idx_user_accounts_status
    ON user_accounts (status);

CREATE INDEX idx_user_accounts_joined_date
    ON user_accounts (joined_date);

CREATE TABLE IF NOT EXISTS roles (
    role_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    priority INT NOT NULL DEFAULT 0,
    redirect_path VARCHAR(120) NOT NULL DEFAULT '/dashboard',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_roles PRIMARY KEY (role_id),
    CONSTRAINT uk_roles_code UNIQUE (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_roles (
    user_id CHAR(36) NOT NULL,
    role_id BIGINT UNSIGNED NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user_id
        FOREIGN KEY (user_id)
            REFERENCES user_accounts (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role_id
        FOREIGN KEY (role_id)
            REFERENCES roles (role_id)
            ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_user_roles_role_id
    ON user_roles (role_id);

CREATE TABLE IF NOT EXISTS invalidated_tokens (
    token_id VARCHAR(128) NOT NULL,
    expiry_time DATETIME NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_invalidated_tokens PRIMARY KEY (token_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_invalidated_tokens_expiry_time
    ON invalidated_tokens (expiry_time);
