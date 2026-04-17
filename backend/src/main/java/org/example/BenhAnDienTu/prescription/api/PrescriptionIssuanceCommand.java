package org.example.BenhAnDienTu.prescription.api;

import java.util.List;

/** Boundary command for issuing a prescription linked to an appointment. */
public record PrescriptionIssuanceCommand(
    String prescriptionCode,
    String patientId,
    String doctorId,
    String appointmentId,
    String status,
    String diagnosis,
    String advice,
    List<PrescriptionItemCommand> items) {}
