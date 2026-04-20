package org.example.BenhAnDienTu.staff.infrastructure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.example.BenhAnDienTu.shared.error.GlobalExceptionHandler;
import org.example.BenhAnDienTu.shared.logging.RequestCorrelationFilter;
import org.example.BenhAnDienTu.staff.api.StaffApi;
import org.example.BenhAnDienTu.staff.api.StaffDoctorCreateCommand;
import org.example.BenhAnDienTu.staff.api.StaffProfileUpdateCommand;
import org.example.BenhAnDienTu.staff.api.StaffProfileView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class StaffControllerTests {

  @Mock private StaffApi staffApi;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    StaffController controller = new StaffController(staffApi);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .addFilters(new RequestCorrelationFilter())
            .build();
    objectMapper = new ObjectMapper();
  }

  @Test
  void getDoctorShouldReturnNotFoundWhenDoctorMissing() throws Exception {
    when(staffApi.findDoctorProfile("doctor-404")).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/staff/doctors/doctor-404"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Doctor does not exist: doctor-404"));
  }

  @Test
  void createDoctorShouldDelegateAndReturnProfile() throws Exception {
    StaffProfileView profile = profile("doctor-1");
    when(staffApi.createDoctor(any(StaffDoctorCreateCommand.class))).thenReturn(profile);

    String request =
        """
        {
          "username": "doctor.local",
          "password": "doctor123",
          "email": "doctor.local@EMR.dev",
          "fullName": "Doctor Local",
          "gender": "Female",
          "phone": "0900000000",
          "address": "HCMC",
          "isConfirmed": true,
          "isActive": true
        }
        """;

    mockMvc
        .perform(post("/api/staff/doctors").contentType("application/json").content(request))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("doctor-1"))
        .andExpect(jsonPath("$.username").value("doctor.local"))
        .andExpect(jsonPath("$.role").value("DOCTOR"));

    verify(staffApi).createDoctor(any(StaffDoctorCreateCommand.class));
  }

  @Test
  void updateMyProfileShouldUseActorIdFromRequestAttribute() throws Exception {
    StaffProfileView updated = profile("doctor-1");
    when(staffApi.updateMyProfile(any(), any(StaffProfileUpdateCommand.class))).thenReturn(updated);

    String request =
        objectMapper.writeValueAsString(
            new StaffController.UpdateMyProfileRequest(
                "Doctor Local Updated", "doctor.local@EMR.dev", "Female", "0900000001", "Hanoi"));

    mockMvc
        .perform(
            put("/api/staff/profile")
                .requestAttr("actorId", "doctor-1")
                .contentType("application/json")
                .content(request))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("doctor-1"))
        .andExpect(jsonPath("$.email").value("doctor.local@EMR.dev"));

    verify(staffApi).updateMyProfile(any(), any(StaffProfileUpdateCommand.class));
  }

  private static StaffProfileView profile(String doctorId) {
    return new StaffProfileView(
        doctorId,
        "doctor.local",
        "Doctor Local",
        "doctor.local@EMR.dev",
        "0900000000",
        "Female",
        "HCMC",
        "DOCTOR",
        "ACTIVE",
        true,
        true,
        null);
  }
}
