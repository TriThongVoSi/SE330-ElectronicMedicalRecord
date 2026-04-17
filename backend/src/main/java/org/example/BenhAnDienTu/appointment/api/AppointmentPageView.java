package org.example.BenhAnDienTu.appointment.api;

import java.util.List;

/** Paged appointment response for HTTP adapters. */
public record AppointmentPageView(
    List<AppointmentView> items, int page, int size, long totalItems, int totalPages) {}
