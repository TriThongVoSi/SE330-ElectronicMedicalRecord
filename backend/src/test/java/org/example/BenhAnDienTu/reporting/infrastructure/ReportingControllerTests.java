package org.example.BenhAnDienTu.reporting.infrastructure;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.example.BenhAnDienTu.reporting.api.PatientMedicalTimelineEntryView;
import org.example.BenhAnDienTu.reporting.api.PatientMedicalTimelineView;
import org.example.BenhAnDienTu.reporting.api.ReportingApi;
import org.example.BenhAnDienTu.shared.error.GlobalExceptionHandler;
import org.example.BenhAnDienTu.shared.logging.RequestCorrelationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ReportingControllerTests {

  @Mock private ReportingApi reportingApi;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    ReportingController controller = new ReportingController(reportingApi);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .addFilters(new RequestCorrelationFilter())
            .build();
  }

  @Test
  void getPatientMedicalTimelineShouldReturnTimelineForDemo() throws Exception {
    PatientMedicalTimelineView timeline = sampleTimeline();
    when(reportingApi.getPatientMedicalTimeline("patient-1", 50)).thenReturn(timeline);

    mockMvc
        .perform(get("/api/dashboard/patients/patient-1/timeline"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.patientId").value("patient-1"))
        .andExpect(jsonPath("$.patientName").value("Patient Demo"))
        .andExpect(jsonPath("$.entries[0].appointmentId").value("appointment-1"))
        .andExpect(jsonPath("$.entries[0].prescriptionCode").value("RX-001"))
        .andExpect(jsonPath("$.entries[0].diagnosis").value("Acute pharyngitis"));

    verify(reportingApi).getPatientMedicalTimeline("patient-1", 50);
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
