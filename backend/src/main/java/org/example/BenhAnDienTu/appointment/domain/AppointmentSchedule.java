package org.example.BenhAnDienTu.appointment.domain;

import java.time.Instant;

/** Domain root placeholder for appointment lifecycle ownership. */
public record AppointmentSchedule(
    String appointmentId,
    String patientId,
    String staffId,
    String serviceCode,
    Instant scheduledAt,
    String status) {}
