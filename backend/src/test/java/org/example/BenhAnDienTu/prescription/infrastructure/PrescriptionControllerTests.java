package org.example.BenhAnDienTu.prescription.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.example.BenhAnDienTu.prescription.api.PrescriptionApi;
import org.example.BenhAnDienTu.prescription.api.PrescriptionIssuanceCommand;
import org.example.BenhAnDienTu.prescription.api.PrescriptionItemView;
import org.example.BenhAnDienTu.prescription.api.PrescriptionView;
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
class PrescriptionControllerTests {

  @Mock private PrescriptionApi prescriptionApi;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    PrescriptionController controller = new PrescriptionController(prescriptionApi);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .addFilters(new RequestCorrelationFilter())
            .build();
    objectMapper = new ObjectMapper();
  }

  @Test
  void getPrescriptionShouldReturnNotFoundWhenPrescriptionMissing() throws Exception {
    when(prescriptionApi.findPrescription("rx-404")).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/prescriptions/rx-404"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Prescription does not exist: rx-404"));
  }

  @Test
  void createPrescriptionShouldDelegateToApiAndReturnPrescription() throws Exception {
    PrescriptionView created = prescription("rx-1");
    when(prescriptionApi.issuePrescription(any(PrescriptionIssuanceCommand.class)))
        .thenReturn(created);

    String request =
        objectMapper.writeValueAsString(
            new PrescriptionController.PrescriptionUpsertRequest(
                "RX-001",
                "patient-1",
                "doctor-1",
                "appointment-1",
                "CREATED",
                "Acute pharyngitis",
                "Hydration and rest",
                List.of(
                    new PrescriptionController.PrescriptionItemRequest(
                        "drug-1", 2, "After meals"))));

    mockMvc
        .perform(post("/api/prescriptions").contentType("application/json").content(request))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("rx-1"))
        .andExpect(jsonPath("$.prescriptionCode").value("RX-001"))
        .andExpect(jsonPath("$.items[0].drugId").value("drug-1"))
        .andExpect(jsonPath("$.items[0].quantity").value(2));

    ArgumentCaptor<PrescriptionIssuanceCommand> captor =
        ArgumentCaptor.forClass(PrescriptionIssuanceCommand.class);
    verify(prescriptionApi).issuePrescription(captor.capture());
    assertThat(captor.getValue().items()).hasSize(1);
    assertThat(captor.getValue().items().getFirst().drugId()).isEqualTo("drug-1");
  }

  private static PrescriptionView prescription(String id) {
    return new PrescriptionView(
        id,
        "RX-001",
        "patient-1",
        "Patient Local",
        "doctor-1",
        "Doctor Local",
        "appointment-1",
        "CREATED",
        "Acute pharyngitis",
        "Hydration and rest",
        List.of(new PrescriptionItemView("drug-1", "Drug 1", 2, "After meals")),
        Instant.parse("2026-04-04T04:00:00Z"));
  }
}
