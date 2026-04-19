package org.example.BenhAnDienTu.reporting.api;

import java.time.Instant;

/** Timeline item that combines visit data and prescription outcome for a patient. */
public record PatientMedicalTimelineEntryView(
    String appointmentId,
    String appointmentCode,
    Instant appointmentTime,
    String appointmentStatus,
    String doctorId,
    String doctorName,
    String prescriptionId,
    String prescriptionCode,
    String prescriptionStatus,
    Instant prescriptionIssuedAt,
    String diagnosis,
    String advice) {}
