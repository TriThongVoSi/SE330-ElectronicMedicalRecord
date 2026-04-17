package org.example.BenhAnDienTu.appointment.api;

import java.time.Instant;
import java.util.List;

/** Command for patient self-booking flow. */
public record PatientPortalAppointmentCreateCommand(
    String doctorId, Instant appointmentTime, List<String> serviceIds, String note) {}
