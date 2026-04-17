package org.example.BenhAnDienTu.prescription.domain;

import java.time.Instant;

/** Domain root placeholder for prescription ownership. */
public record PrescriptionDocument(
    String prescriptionId, String appointmentId, String notes, Instant issuedAt, String status) {}
