package org.example.BenhAnDienTu.prescription.api;

import java.time.Instant;
import java.util.List;

/** Read-only prescription view exposed outside the module. */
public record PrescriptionView(
    String id,
    String prescriptionCode,
    String patientId,
    String patientName,
    String doctorId,
    String doctorName,
    String appointmentId,
    String status,
    String diagnosis,
    String advice,
    List<PrescriptionItemView> items,
    Instant createdAt) {}
