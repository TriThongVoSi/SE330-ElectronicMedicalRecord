package org.example.BenhAnDienTu.reporting.api;

/** Public contract for reporting use cases shared with other modules. */
public interface ReportingApi {

  DashboardSummaryView getDashboardSummary();

  ReportingSnapshotView buildOperationalSnapshot(ReportingSnapshotQuery query);

  PatientMedicalTimelineView getPatientMedicalTimeline(String patientId, int limit);

  PatientMedicalTimelineView getCurrentPatientMedicalTimeline(String actorId, int limit);

  PatientMedicalVisitDetailView getCurrentPatientVisitDetail(String actorId, String appointmentId);

  PatientPortalDashboardView getCurrentPatientDashboard(String actorId);
}
