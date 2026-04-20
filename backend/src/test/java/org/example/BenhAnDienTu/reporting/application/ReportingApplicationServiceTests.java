package org.example.BenhAnDienTu.reporting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.example.BenhAnDienTu.patient.api.PatientApi;
import org.example.BenhAnDienTu.reporting.api.PatientMedicalTimelineEntryView;
import org.example.BenhAnDienTu.reporting.api.PatientMedicalTimelineView;
import org.example.BenhAnDienTu.reporting.infrastructure.ReportingProjectionAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ReportingApplicationServiceTests {

  @Mock private ReportingProjectionAdapter projectionAdapter;
  @Mock private PatientApi patientApi;

  private ReportingApplicationService service;

  @BeforeEach
  void setUp() {
    service = new ReportingApplicationService(projectionAdapter, patientApi);
  }

  @Test
  void getPatientMedicalTimelineShouldReturnTimelineWhenPatientExists() {
    PatientMedicalTimelineView timeline = sampleTimeline();
    when(projectionAdapter.loadPatientMedicalTimeline("patient-1", 50))
        .thenReturn(Optional.of(timeline));

    PatientMedicalTimelineView result = service.getPatientMedicalTimeline("patient-1", 50);

    assertThat(result.patientId()).isEqualTo("patient-1");
    assertThat(result.entries()).hasSize(1);
    assertThat(result.entries().getFirst().diagnosis()).isEqualTo("Acute pharyngitis");
  }

  @Test
  void getPatientMedicalTimelineShouldThrowNotFoundWhenPatientMissing() {
    when(projectionAdapter.loadPatientMedicalTimeline("patient-404", 50))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getPatientMedicalTimeline("patient-404", 50))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            throwable -> {
              ResponseStatusException exception = (ResponseStatusException) throwable;
              assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
              assertThat(exception.getReason()).isEqualTo("Patient does not exist: patient-404");
            });
  }

  private static PatientMedicalTimelineView sampleTimeline() {
    return new PatientMedicalTimelineView(
        "patient-1",
        "Patient Demo",
        List.of(
            new PatientMedicalTimelineEntryView(
                "appointment-1",
                "AP-001",
                Instant.parse("2026-03-10T03:00:00Z"),
                "FINISH",
                "doctor-1",
                "Doctor Demo",
                "prescription-1",
                "RX-001",
                "ISSUED",
                Instant.parse("2026-03-10T04:00:00Z"),
                "Acute pharyngitis",
                "Hydration and rest")));
  }
}
