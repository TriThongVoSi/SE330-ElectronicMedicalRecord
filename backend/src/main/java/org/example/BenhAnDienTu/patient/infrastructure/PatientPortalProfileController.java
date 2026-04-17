package org.example.BenhAnDienTu.patient.infrastructure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import org.example.BenhAnDienTu.patient.api.PatientApi;
import org.example.BenhAnDienTu.patient.api.PatientSelfProfileUpdateCommand;
import org.example.BenhAnDienTu.patient.api.PatientView;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Validated
@RestController
@RequestMapping("/api/v1/patient-portal/profile")
public class PatientPortalProfileController {

  private final PatientApi patientApi;

  public PatientPortalProfileController(PatientApi patientApi) {
    this.patientApi = patientApi;
  }

  @GetMapping
  public PatientView getCurrentProfile(@RequestAttribute(name = "actorId") String actorId) {
    return patientApi
        .findPatientByActorId(actorId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    NOT_FOUND, "Patient profile does not exist for current account."));
  }

  @PutMapping
  public PatientView updateCurrentProfile(
      @RequestAttribute(name = "actorId") String actorId,
      @Valid @RequestBody PatientProfileUpdateRequest request) {
    return patientApi.updateCurrentPatientProfile(actorId, request.toCommand());
  }

  public record PatientProfileUpdateRequest(
      @NotBlank String phone,
      String address,
      @DecimalMin(value = "0.0", inclusive = false) BigDecimal heightCm,
      @DecimalMin(value = "0.0", inclusive = false) BigDecimal weightKg,
      String drugAllergies) {

    private PatientSelfProfileUpdateCommand toCommand() {
      return new PatientSelfProfileUpdateCommand(phone.trim(), address, heightCm, weightKg, drugAllergies);
    }
  }
}
