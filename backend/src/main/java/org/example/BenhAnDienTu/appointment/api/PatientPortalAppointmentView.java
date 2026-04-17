package org.example.BenhAnDienTu.appointment.api;

import java.time.Instant;
import java.util.List;

/** Self-scoped appointment projection for patient portal endpoints. */
public record PatientPortalAppointmentView(
    String id,
    String appointmentCode,
    Instant appointmentTime,
    String status,
    String normalizedStatus,
    String cancelReason,
    String doctorId,
    String doctorName,
    int urgencyLevel,
    String prescriptionStatus,
    boolean followup,
    Integer priorityScore,
    List<String> serviceIds) {}
