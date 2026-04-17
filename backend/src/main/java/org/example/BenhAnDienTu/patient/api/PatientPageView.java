package org.example.BenhAnDienTu.patient.api;

import java.util.List;

/** Generic page response for patient list endpoint. */
public record PatientPageView(
    List<PatientView> items, int page, int size, long totalItems, int totalPages) {}
