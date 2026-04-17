package org.example.BenhAnDienTu.appointment.api;

import java.time.Instant;
import java.util.List;

/** Boundary command for creating an appointment slot assignment. */
public record AppointmentBookingCommand(
    String appointmentCode,
    Instant appointmentTime,
    String status,
    String cancelReason,
    String doctorId,
    String patientId,
    int urgencyLevel,
    String prescriptionStatus,
    boolean isFollowup,
    Integer priorityScore,
    List<String> serviceIds) {}
