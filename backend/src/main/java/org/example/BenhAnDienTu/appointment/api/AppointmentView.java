package org.example.BenhAnDienTu.appointment.api;

import java.time.Instant;
import java.util.List;

/** Read-only appointment view exposed outside appointment module. */
public record AppointmentView(
    String id,
    String appointmentCode,
    Instant appointmentTime,
    String status,
    String cancelReason,
    String doctorId,
    String doctorName,
    String patientId,
    String patientName,
    int urgencyLevel,
    String prescriptionStatus,
    boolean isFollowup,
    Integer priorityScore,
    List<String> serviceIds,
    Instant createdAt,
    Instant updatedAt) {}
