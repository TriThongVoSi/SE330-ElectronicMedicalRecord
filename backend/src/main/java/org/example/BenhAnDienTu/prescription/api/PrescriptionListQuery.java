package org.example.BenhAnDienTu.prescription.api;

/** Paged prescription list query. */
public record PrescriptionListQuery(
    int page, int size, String search, String status, String patientId) {}
