package org.example.BenhAnDienTu.appointment.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.example.BenhAnDienTu.appointment.api.AppointmentApi;
import org.example.BenhAnDienTu.appointment.api.AppointmentBookingCommand;
import org.example.BenhAnDienTu.appointment.api.AppointmentPageView;
import org.example.BenhAnDienTu.appointment.api.AppointmentView;
import org.example.BenhAnDienTu.shared.error.GlobalExceptionHandler;
import org.example.BenhAnDienTu.shared.logging.RequestCorrelationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AppointmentControllerTests {

  @Mock private AppointmentApi appointmentApi;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    AppointmentController controller = new AppointmentController(appointmentApi);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .addFilters(new RequestCorrelationFilter())
            .build();
  }

  @Test
  void getAppointmentShouldReturnNotFoundWhenAppointmentMissing() throws Exception {
    when(appointmentApi.findAppointment("appointment-404")).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/appointments/appointment-404"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Appointment does not exist: appointment-404"));
  }

  @Test
  void createAppointmentShouldForwardServiceIdsAndReturnCreatedData() throws Exception {
    AppointmentView created = appointment("appointment-1");
    when(appointmentApi.bookAppointment(any(AppointmentBookingCommand.class))).thenReturn(created);

    String request =
        """
        {
          "appointmentCode": "AP-001",
          "appointmentTime": "2026-04-04T09:00:00Z",
          "status": "COMING",
          "doctorId": "doctor-1",
          "patientId": "patient-1",
          "urgencyLevel": 2,
          "prescriptionStatus": "NONE",
          "isFollowup": false,
          "priorityScore": 6,
          "serviceIds": ["service-1", "service-2"]
        }
        """;

    mockMvc
        .perform(post("/api/appointments").contentType("application/json").content(request))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("appointment-1"))
        .andExpect(jsonPath("$.appointmentCode").value("AP-001"))
        .andExpect(jsonPath("$.serviceIds[0]").value("service-1"))
        .andExpect(jsonPath("$.serviceIds[1]").value("service-2"));

    ArgumentCaptor<AppointmentBookingCommand> captor =
        ArgumentCaptor.forClass(AppointmentBookingCommand.class);
    verify(appointmentApi).bookAppointment(captor.capture());
    assertThat(captor.getValue().serviceIds()).containsExactly("service-1", "service-2");
  }

  @Test
  void listAppointmentsShouldReturnPagedPayload() throws Exception {
    AppointmentPageView page =
        new AppointmentPageView(List.of(appointment("appointment-1")), 2, 5, 21, 5);
    when(appointmentApi.listAppointments(any())).thenReturn(page);

    mockMvc
        .perform(
            get("/api/appointments")
                .queryParam("page", "2")
                .queryParam("size", "5")
                .queryParam("search", "AP-001")
                .queryParam("status", "COMING")
                .queryParam("doctorId", "doctor-1")
                .queryParam("patientId", "patient-1")
                .queryParam("date", "2026-04-04"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(2))
        .andExpect(jsonPath("$.size").value(5))
        .andExpect(jsonPath("$.totalItems").value(21))
        .andExpect(jsonPath("$.items[0].appointmentCode").value("AP-001"));

    verify(appointmentApi).listAppointments(any());
  }

  private static AppointmentView appointment(String appointmentId) {
    Instant now = Instant.parse("2026-04-04T04:00:00Z");
    return new AppointmentView(
        appointmentId,
        "AP-001",
        Instant.parse("2026-04-04T09:00:00Z"),
        "COMING",
        null,
        "doctor-1",
        "Doctor Local",
        "patient-1",
        "Patient Local",
        2,
        "NONE",
        false,
        6,
        List.of("service-1", "service-2"),
        now,
        now);
  }
}
