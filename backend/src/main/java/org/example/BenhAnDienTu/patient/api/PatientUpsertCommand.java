package org.example.BenhAnDienTu.patient.api;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Patient command for create/update operations at module boundary. */
public record PatientUpsertCommand(
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
    BigDecimal weightKg) {}
