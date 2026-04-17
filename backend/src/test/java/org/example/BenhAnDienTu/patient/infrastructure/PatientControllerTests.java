package org.example.BenhAnDienTu.patient.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.example.BenhAnDienTu.patient.api.PatientApi;
import org.example.BenhAnDienTu.patient.api.PatientPageView;
import org.example.BenhAnDienTu.patient.api.PatientUpsertCommand;
import org.example.BenhAnDienTu.patient.api.PatientView;
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
class PatientControllerTests {

  @Mock private PatientApi patientApi;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    PatientController controller = new PatientController(patientApi);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .addFilters(new RequestCorrelationFilter())
            .build();
  }

  @Test
  void getPatientShouldReturnNotFoundWhenPatientMissing() throws Exception {
    when(patientApi.findPatient("patient-404")).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/patients/patient-404"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Patient does not exist: patient-404"));
  }

  @Test
  void createPatientShouldDelegateAndReturnCreatedPatient() throws Exception {
    PatientView created = patient("patient-1", "PT-001");
    when(patientApi.createPatient(any(PatientUpsertCommand.class))).thenReturn(created);

    String request =
        """
        {
          "patientCode": "PT-001",
          "fullName": "John Smith",
          "dateOfBirth": "1990-01-01",
          "email": "john.smith@EMR.dev",
          "gender": "Male",
          "phone": "0900000000",
          "address": "HCMC",
          "diagnosis": "General anxiety",
          "drugAllergies": "Penicillin",
          "heightCm": 175,
          "weightKg": 70
        }
        """;

    mockMvc
        .perform(post("/api/patients").contentType("application/json").content(request))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("patient-1"))
        .andExpect(jsonPath("$.patientCode").value("PT-001"))
        .andExpect(jsonPath("$.fullName").value("John Smith"));

    ArgumentCaptor<PatientUpsertCommand> captor =
        ArgumentCaptor.forClass(PatientUpsertCommand.class);
    verify(patientApi).createPatient(captor.capture());
    assertThat(captor.getValue().patientCode()).isEqualTo("PT-001");
  }

  @Test
  void listPatientsShouldMapQueryParamsAndReturnPage() throws Exception {
    PatientPageView page =
        new PatientPageView(List.of(patient("patient-1", "PT-001")), 2, 5, 11, 3);
    when(patientApi.listPatients(any())).thenReturn(page);

    mockMvc
        .perform(
            get("/api/patients")
                .queryParam("page", "2")
                .queryParam("size", "5")
                .queryParam("search", "john")
                .queryParam("phone", "0900")
                .queryParam("code", "PT"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(2))
        .andExpect(jsonPath("$.size").value(5))
        .andExpect(jsonPath("$.totalItems").value(11))
        .andExpect(jsonPath("$.items[0].patientCode").value("PT-001"));

    verify(patientApi).listPatients(any());
  }

  private static PatientView patient(String id, String patientCode) {
    Instant now = Instant.parse("2026-04-04T04:00:00Z");
    return new PatientView(
        id,
        patientCode,
        "John Smith",
        LocalDate.parse("1990-01-01"),
        "john.smith@EMR.dev",
        "Male",
        "0900000000",
        "HCMC",
        "General anxiety",
        "Penicillin",
        BigDecimal.valueOf(175),
        BigDecimal.valueOf(70),
        now,
        now);
  }
}
