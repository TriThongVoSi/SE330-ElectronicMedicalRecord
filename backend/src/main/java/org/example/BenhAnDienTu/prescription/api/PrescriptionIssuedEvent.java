package org.example.BenhAnDienTu.prescription.api;

import java.time.Instant;

/** Event emitted after a prescription has been issued. */
public record PrescriptionIssuedEvent(
    String prescriptionId, String appointmentId, Instant occurredAt) {}
