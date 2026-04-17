package org.example.BenhAnDienTu.appointment.api;

import java.time.LocalDate;

/** Paged appointment list query. */
public record AppointmentListQuery(
    int page,
    int size,
    String search,
    String status,
    String doctorId,
    String patientId,
    LocalDate date) {}
