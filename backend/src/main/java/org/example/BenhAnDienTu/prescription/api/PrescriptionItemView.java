package org.example.BenhAnDienTu.prescription.api;

/** Prescription line projection. */
public record PrescriptionItemView(
    String drugId, String drugName, int quantity, String instructions) {}
