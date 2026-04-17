package org.example.BenhAnDienTu.prescription.api;

import java.util.List;

/** Paged prescription response. */
public record PrescriptionPageView(
    List<PrescriptionView> items, int page, int size, long totalItems, int totalPages) {}
