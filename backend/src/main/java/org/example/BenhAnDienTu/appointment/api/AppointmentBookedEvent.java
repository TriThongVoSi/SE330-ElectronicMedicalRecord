package org.example.BenhAnDienTu.appointment.api;

import java.time.Instant;

/** Event emitted when an appointment has been booked. */
public record AppointmentBookedEvent(String appointmentId, String patientId, Instant occurredAt) {}
