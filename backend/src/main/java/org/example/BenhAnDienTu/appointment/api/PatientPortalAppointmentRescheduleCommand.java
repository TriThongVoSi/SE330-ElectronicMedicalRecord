package org.example.BenhAnDienTu.appointment.api;

import java.time.Instant;
import java.util.List;

/** Command for patient self-reschedule flow. */
public record PatientPortalAppointmentRescheduleCommand(
    String doctorId, Instant appointmentTime, List<String> serviceIds) {}
