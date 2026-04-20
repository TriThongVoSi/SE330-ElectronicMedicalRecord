package org.example.BenhAnDienTu.reporting.api;

import java.time.Instant;

/** Short upcoming appointment projection for dashboard table. */
public record UpcomingAppointmentView(
    String id, Instant appointmentTime, String patientName, String doctorName, String status) {}
