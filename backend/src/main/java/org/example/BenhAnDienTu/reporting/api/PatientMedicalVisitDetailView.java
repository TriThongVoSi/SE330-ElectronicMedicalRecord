package org.example.BenhAnDienTu.reporting.api;

import java.time.Instant;
import java.util.List;

/** Patient-facing visit detail payload (read-only). */
public record PatientMedicalVisitDetailView(
    String appointmentId,
    String appointmentCode,
    Instant visitTime,
    String appointmentStatus,
    String doctorId,
    String doctorName,
    String diagnosis,
    String notes,
    String prescriptionId,
    String prescriptionCode,
    String prescriptionStatus,
    List<PatientMedicalVisitMedicationView> medications) {}
