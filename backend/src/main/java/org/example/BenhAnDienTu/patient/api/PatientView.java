package org.example.BenhAnDienTu.patient.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Read-only patient projection exposed outside the patient module. */
public record PatientView(
    String id,
    String patientCode,
    String fullName,
    LocalDate dateOfBirth,
    String email,
    String gender,
    String phone,
    String address,
    String diagnosis,
    String drugAllergies,
    BigDecimal heightCm,
    BigDecimal weightKg,
    Instant createdAt,
    Instant updatedAt) {}
