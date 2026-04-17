package org.example.BenhAnDienTu.prescription.api;

/** Prescription line command. */
public record PrescriptionItemCommand(String drugId, int quantity, String instructions) {}
