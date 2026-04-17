package org.example.BenhAnDienTu.patient.api;

import java.math.BigDecimal;

/** Self-service command for updating current patient profile fields. */
public record PatientSelfProfileUpdateCommand(
    String phone, String address, BigDecimal heightCm, BigDecimal weightKg, String drugAllergies) {}
