CREATE TABLE otp_verifications (
    otp_id CHAR(36) NOT NULL,
    user_id CHAR(36) NULL,
    email VARCHAR(100) NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    attempts INT UNSIGNED NOT NULL DEFAULT 0,
    max_attempts INT UNSIGNED NOT NULL DEFAULT 5,
    consumed_at DATETIME NULL,
    last_sent_at DATETIME NULL,
    resend_count INT UNSIGNED NOT NULL DEFAULT 1,
    metadata TEXT NULL,
    CONSTRAINT pk_otp_verifications PRIMARY KEY (otp_id),
    CONSTRAINT fk_otp_verifications_user_id
        FOREIGN KEY (user_id)
            REFERENCES user_accounts (id)
            ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_otp_verifications_email_purpose_consumed_created
    ON otp_verifications (email, purpose, consumed_at, created_at);

CREATE INDEX idx_otp_verifications_user_purpose_consumed_created
    ON otp_verifications (user_id, purpose, consumed_at, created_at);

CREATE INDEX idx_otp_verifications_expires_at
    ON otp_verifications (expires_at);
