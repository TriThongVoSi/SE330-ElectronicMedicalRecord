package org.example.BenhAnDienTu.appointment.api;

/** Command for patient self-cancel flow. */
public record PatientPortalAppointmentCancelCommand(String cancelReason) {}
