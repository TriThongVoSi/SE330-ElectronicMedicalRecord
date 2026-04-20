package org.example.BenhAnDienTu.reporting.infrastructure;

import jakarta.validation.constraints.Min;
import org.example.BenhAnDienTu.reporting.api.DashboardSummaryView;
import org.example.BenhAnDienTu.reporting.api.PatientMedicalTimelineView;
import org.example.BenhAnDienTu.reporting.api.ReportingApi;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/dashboard")
public class ReportingController {

  private final ReportingApi reportingApi;

  public ReportingController(ReportingApi reportingApi) {
    this.reportingApi = reportingApi;
  }

  @GetMapping("/summary")
  public DashboardSummaryView getSummary() {
    return reportingApi.getDashboardSummary();
  }

  @GetMapping("/patients/{patientId}/timeline")
  public PatientMedicalTimelineView getPatientMedicalTimeline(
      @PathVariable("patientId") String patientId,
      @RequestParam(defaultValue = "50") @Min(1) int limit) {
    return reportingApi.getPatientMedicalTimeline(patientId, limit);
  }
}
