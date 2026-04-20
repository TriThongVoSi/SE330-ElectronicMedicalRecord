package org.example.BenhAnDienTu.reporting.api;

/** Compact patient dashboard snapshot for patient portal home screen. */
public record PatientPortalDashboardView(
    int upcomingAppointments,
    PatientMedicalTimelineEntryView latestVisit,
    PatientMedicalTimelineEntryView latestPrescription) {}
