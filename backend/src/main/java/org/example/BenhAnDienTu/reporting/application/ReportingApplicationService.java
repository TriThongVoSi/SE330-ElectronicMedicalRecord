package org.example.BenhAnDienTu.reporting.application;

import org.example.BenhAnDienTu.patient.api.PatientApi;
import org.example.BenhAnDienTu.reporting.api.DashboardSummaryView;
import org.example.BenhAnDienTu.reporting.api.PatientMedicalTimelineView;
import org.example.BenhAnDienTu.reporting.api.PatientMedicalVisitDetailView;
import org.example.BenhAnDienTu.reporting.api.PatientPortalDashboardView;
import org.example.BenhAnDienTu.reporting.api.ReportingApi;
import org.example.BenhAnDienTu.reporting.api.ReportingSnapshotQuery;
import org.example.BenhAnDienTu.reporting.api.ReportingSnapshotView;
import org.example.BenhAnDienTu.reporting.infrastructure.ReportingProjectionAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReportingApplicationService implements ReportingApi {

  private final ReportingProjectionAdapter projectionAdapter;
  private final PatientApi patientApi;

  public ReportingApplicationService(
      ReportingProjectionAdapter projectionAdapter, PatientApi patientApi) {
    this.projectionAdapter = projectionAdapter;
    this.patientApi = patientApi;
  }

  @Override
  public DashboardSummaryView getDashboardSummary() {
    return projectionAdapter.loadDashboardSummary();
  }

  @Override
  public ReportingSnapshotView buildOperationalSnapshot(ReportingSnapshotQuery query) {
    return new ReportingSnapshotView(
        "Operational Snapshot", projectionAdapter.collectHighlights(query));
  }

  @Override
  public PatientMedicalTimelineView getPatientMedicalTimeline(String patientId, int limit) {
    return projectionAdapter
        .loadPatientMedicalTimeline(patientId, limit)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Patient does not exist: " + patientId));
  }

  @Override
  public PatientMedicalTimelineView getCurrentPatientMedicalTimeline(String actorId, int limit) {
    String patientId = resolvePatientId(actorId);
    return getPatientMedicalTimeline(patientId, limit);
  }

  @Override
  public PatientMedicalVisitDetailView getCurrentPatientVisitDetail(
      String actorId, String appointmentId) {
    String patientId = resolvePatientId(actorId);
    return projectionAdapter
        .loadPatientVisitDetail(patientId, appointmentId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Visit detail does not exist: " + appointmentId));
  }

  @Override
  public PatientPortalDashboardView getCurrentPatientDashboard(String actorId) {
    String patientId = resolvePatientId(actorId);
    return projectionAdapter.loadPatientDashboard(patientId);
  }

  private String resolvePatientId(String actorId) {
    return patientApi
        .findPatientIdByActorId(actorId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Current account is not linked to a patient profile."));
  }
}
