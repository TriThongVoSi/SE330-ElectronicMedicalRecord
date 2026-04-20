package org.example.BenhAnDienTu.reporting.infrastructure;

import jakarta.validation.constraints.Min;
import org.example.BenhAnDienTu.reporting.api.PatientMedicalTimelineView;
import org.example.BenhAnDienTu.reporting.api.PatientMedicalVisitDetailView;
import org.example.BenhAnDienTu.reporting.api.PatientPortalDashboardView;
import org.example.BenhAnDienTu.reporting.api.ReportingApi;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/patient-portal")
public class PatientPortalReportingController {

  private final ReportingApi reportingApi;

  public PatientPortalReportingController(ReportingApi reportingApi) {
    this.reportingApi = reportingApi;
  }

  @GetMapping("/dashboard")
  public PatientPortalDashboardView getDashboard(
      @RequestAttribute(name = "actorId") String actorId) {
    return reportingApi.getCurrentPatientDashboard(actorId);
  }

  @GetMapping("/medical-history")
  public PatientMedicalTimelineView getCurrentPatientMedicalTimeline(
      @RequestAttribute(name = "actorId") String actorId,
      @RequestParam(defaultValue = "50") @Min(1) int limit) {
    return reportingApi.getCurrentPatientMedicalTimeline(actorId, limit);
  }

  @GetMapping("/medical-history/{appointmentId}")
  public PatientMedicalVisitDetailView getCurrentPatientVisitDetail(
      @RequestAttribute(name = "actorId") String actorId,
      @PathVariable("appointmentId") String appointmentId) {
    return reportingApi.getCurrentPatientVisitDetail(actorId, appointmentId);
  }
}
