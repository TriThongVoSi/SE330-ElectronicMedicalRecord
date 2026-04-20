package org.example.BenhAnDienTu.reporting.api;

/** Medication item included in patient portal visit detail. */
public record PatientMedicalVisitMedicationView(
    String drugId, String drugName, String dosage, int quantity, String instructions) {}
