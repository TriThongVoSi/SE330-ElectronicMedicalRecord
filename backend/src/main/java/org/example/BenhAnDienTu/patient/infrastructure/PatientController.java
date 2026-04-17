package org.example.BenhAnDienTu.patient.infrastructure;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.example.BenhAnDienTu.patient.api.PatientApi;
import org.example.BenhAnDienTu.patient.api.PatientListQuery;
import org.example.BenhAnDienTu.patient.api.PatientPageView;
import org.example.BenhAnDienTu.patient.api.PatientUpsertCommand;
import org.example.BenhAnDienTu.patient.api.PatientView;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/patients")
public class PatientController {

  private final PatientApi patientApi;

  public PatientController(PatientApi patientApi) {
    this.patientApi = patientApi;
  }

  @GetMapping
  public PatientPageView listPatients(
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "10") @Min(1) int size,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String phone,
      @RequestParam(required = false) String code) {
    return patientApi.listPatients(new PatientListQuery(page, size, search, phone, code));
  }

  @GetMapping("/{id}")
  public PatientView getPatient(@PathVariable("id") String id) {
    return patientApi
        .findPatient(id)
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Patient does not exist: " + id));
  }

  @PostMapping
  public PatientView createPatient(@Valid @RequestBody PatientUpsertRequest request) {
    return patientApi.createPatient(request.toCommand());
  }

  @PutMapping("/{id}")
  public PatientView updatePatient(
      @PathVariable("id") String id, @Valid @RequestBody PatientUpsertRequest request) {
    return patientApi.updatePatient(id, request.toCommand());
  }

  public record PatientUpsertRequest(
      @NotBlank String patientCode,
      @NotBlank String fullName,
      LocalDate dateOfBirth,
      String email,
      @NotBlank String gender,
      @NotBlank String phone,
      String address,
      String diagnosis,
      String drugAllergies,
      BigDecimal heightCm,
      BigDecimal weightKg) {

    private PatientUpsertCommand toCommand() {
      return new PatientUpsertCommand(
          patientCode,
          fullName,
          dateOfBirth,
          email,
          gender,
          phone,
          address,
          diagnosis,
          drugAllergies,
          heightCm,
          weightKg);
    }
  }
}
