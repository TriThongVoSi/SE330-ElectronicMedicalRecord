CREATE TABLE services (
    id CHAR(36) NOT NULL,
    service_code VARCHAR(30) NOT NULL,
    service_name VARCHAR(120) NOT NULL,
    service_type VARCHAR(30) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_services PRIMARY KEY (id),
    CONSTRAINT uk_services_service_code UNIQUE (service_code),
    CONSTRAINT uk_services_service_name UNIQUE (service_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE user_accounts (
    id CHAR(36) NOT NULL,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    avatar LONGBLOB NULL,
    gender VARCHAR(20) NULL,
    role VARCHAR(30) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_user_accounts PRIMARY KEY (id),
    CONSTRAINT uk_user_accounts_username UNIQUE (username),
    CONSTRAINT uk_user_accounts_email UNIQUE (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE admins (
    admin_id CHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_admins PRIMARY KEY (admin_id),
    CONSTRAINT fk_admins_admin_id
        FOREIGN KEY (admin_id)
            REFERENCES user_accounts (id)
            ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE doctors (
    doctor_id CHAR(36) NOT NULL,
    phone VARCHAR(20) NULL,
    service_id CHAR(36) NULL,
    address VARCHAR(255) NULL,
    is_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_doctors PRIMARY KEY (doctor_id),
    CONSTRAINT fk_doctors_doctor_id
        FOREIGN KEY (doctor_id)
            REFERENCES user_accounts (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_doctors_service_id
        FOREIGN KEY (service_id)
            REFERENCES services (id)
            ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE patients (
    patient_id CHAR(36) NOT NULL,
    patient_code VARCHAR(30) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NULL,
    email VARCHAR(100) NULL,
    gender VARCHAR(20) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address VARCHAR(255) NULL,
    diagnosis TEXT NULL,
    height_cm DECIMAL(5, 2) NULL,
    weight_kg DECIMAL(5, 2) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_patients PRIMARY KEY (patient_id),
    CONSTRAINT uk_patients_patient_code UNIQUE (patient_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE appointments (
    appointment_id CHAR(36) NOT NULL,
    appointment_code VARCHAR(30) NOT NULL,
    appointment_time DATETIME NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'COMING',
    cancel_reason TEXT NULL,
    doctor_id CHAR(36) NOT NULL,
    patient_id CHAR(36) NOT NULL,
    urgency_level TINYINT UNSIGNED NOT NULL DEFAULT 1,
    prescription_status VARCHAR(30) NOT NULL DEFAULT 'NONE',
    is_followup BOOLEAN NOT NULL DEFAULT FALSE,
    priority_score INT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_appointments PRIMARY KEY (appointment_id),
    CONSTRAINT uk_appointments_appointment_code UNIQUE (appointment_code),
    CONSTRAINT uk_appointments_doctor_id_appointment_time UNIQUE (doctor_id, appointment_time),
    CONSTRAINT fk_appointments_doctor_id
        FOREIGN KEY (doctor_id)
            REFERENCES doctors (doctor_id)
            ON DELETE RESTRICT,
    CONSTRAINT fk_appointments_patient_id
        FOREIGN KEY (patient_id)
            REFERENCES patients (patient_id)
            ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE appointment_services (
    appointment_id CHAR(36) NOT NULL,
    service_id CHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_appointment_services PRIMARY KEY (appointment_id, service_id),
    CONSTRAINT fk_appointment_services_appointment_id
        FOREIGN KEY (appointment_id)
            REFERENCES appointments (appointment_id)
            ON DELETE CASCADE,
    CONSTRAINT fk_appointment_services_service_id
        FOREIGN KEY (service_id)
            REFERENCES services (id)
            ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE available_slots (
    slot_id CHAR(36) NOT NULL,
    doctor_id CHAR(36) NOT NULL,
    slot_date DATE NOT NULL,
    slot_time TIME NOT NULL,
    duration_minutes SMALLINT UNSIGNED NOT NULL DEFAULT 15,
    is_booked BOOLEAN NOT NULL DEFAULT FALSE,
    appointment_id CHAR(36) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_available_slots PRIMARY KEY (slot_id),
    CONSTRAINT uk_available_slots_doctor_id_slot_date_slot_time UNIQUE (doctor_id, slot_date, slot_time),
    CONSTRAINT fk_available_slots_doctor_id
        FOREIGN KEY (doctor_id)
            REFERENCES doctors (doctor_id)
            ON DELETE CASCADE,
    CONSTRAINT fk_available_slots_appointment_id
        FOREIGN KEY (appointment_id)
            REFERENCES appointments (appointment_id)
            ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE drugs (
    drug_id CHAR(36) NOT NULL,
    drug_code VARCHAR(30) NOT NULL,
    drug_name VARCHAR(100) NOT NULL,
    manufacturer VARCHAR(120) NOT NULL,
    expiry_date DATE NOT NULL,
    unit VARCHAR(30) NOT NULL,
    price DECIMAL(12, 2) NOT NULL,
    stock_quantity INT UNSIGNED NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_drugs PRIMARY KEY (drug_id),
    CONSTRAINT uk_drugs_drug_code UNIQUE (drug_code),
    CONSTRAINT uk_drugs_drug_name_manufacturer UNIQUE (drug_name, manufacturer)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE prescriptions (
    prescription_id CHAR(36) NOT NULL,
    prescription_code VARCHAR(30) NOT NULL,
    patient_id CHAR(36) NOT NULL,
    doctor_id CHAR(36) NOT NULL,
    appointment_id CHAR(36) NOT NULL,
    diagnosis TEXT NULL,
    advice TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_prescriptions PRIMARY KEY (prescription_id),
    CONSTRAINT uk_prescriptions_prescription_code UNIQUE (prescription_code),
    CONSTRAINT uk_prescriptions_appointment_id UNIQUE (appointment_id),
    CONSTRAINT fk_prescriptions_patient_id
        FOREIGN KEY (patient_id)
            REFERENCES patients (patient_id)
            ON DELETE RESTRICT,
    CONSTRAINT fk_prescriptions_doctor_id
        FOREIGN KEY (doctor_id)
            REFERENCES doctors (doctor_id)
            ON DELETE RESTRICT,
    CONSTRAINT fk_prescriptions_appointment_id
        FOREIGN KEY (appointment_id)
            REFERENCES appointments (appointment_id)
            ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE prescription_items (
    prescription_id CHAR(36) NOT NULL,
    drug_id CHAR(36) NOT NULL,
    quantity INT UNSIGNED NOT NULL,
    instructions TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_prescription_items PRIMARY KEY (prescription_id, drug_id),
    CONSTRAINT fk_prescription_items_prescription_id
        FOREIGN KEY (prescription_id)
            REFERENCES prescriptions (prescription_id)
            ON DELETE CASCADE,
    CONSTRAINT fk_prescription_items_drug_id
        FOREIGN KEY (drug_id)
            REFERENCES drugs (drug_id)
            ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
