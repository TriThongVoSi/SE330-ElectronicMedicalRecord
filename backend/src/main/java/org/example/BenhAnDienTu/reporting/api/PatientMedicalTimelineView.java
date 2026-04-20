package org.example.BenhAnDienTu.reporting.api;

import java.util.List;

/** Aggregated timeline for patient medical history demo use case. */
public record PatientMedicalTimelineView(
    String patientId, String patientName, List<PatientMedicalTimelineEntryView> entries) {}
