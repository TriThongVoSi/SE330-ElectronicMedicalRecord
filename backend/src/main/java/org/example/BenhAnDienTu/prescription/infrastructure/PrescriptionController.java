package org.example.BenhAnDienTu.prescription.infrastructure;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.example.BenhAnDienTu.prescription.api.PrescriptionApi;
import org.example.BenhAnDienTu.prescription.api.PrescriptionIssuanceCommand;
import org.example.BenhAnDienTu.prescription.api.PrescriptionItemCommand;
import org.example.BenhAnDienTu.prescription.api.PrescriptionListQuery;
import org.example.BenhAnDienTu.prescription.api.PrescriptionPageView;
import org.example.BenhAnDienTu.prescription.api.PrescriptionView;
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
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

  private final PrescriptionApi prescriptionApi;

  public PrescriptionController(PrescriptionApi prescriptionApi) {
    this.prescriptionApi = prescriptionApi;
  }

  @GetMapping
  public PrescriptionPageView listPrescriptions(
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "10") @Min(1) int size,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String patientId) {
    return prescriptionApi.listPrescriptions(
        new PrescriptionListQuery(page, size, search, status, patientId));
  }

  @GetMapping("/{id}")
  public PrescriptionView getPrescription(@PathVariable("id") String id) {
    return prescriptionApi
        .findPrescription(id)
        .orElseThrow(
            () -> new ResponseStatusException(NOT_FOUND, "Prescription does not exist: " + id));
  }

  @PostMapping
  public PrescriptionView createPrescription(
      @Valid @RequestBody PrescriptionUpsertRequest request) {
    return prescriptionApi.issuePrescription(request.toCommand());
  }

  @PutMapping("/{id}")
  public PrescriptionView updatePrescription(
      @PathVariable("id") String id, @Valid @RequestBody PrescriptionUpsertRequest request) {
    return prescriptionApi.updatePrescription(id, request.toCommand());
  }

  public record PrescriptionUpsertRequest(
      @NotBlank String prescriptionCode,
      @NotBlank String patientId,
      @NotBlank String doctorId,
      @NotBlank String appointmentId,
      @NotBlank String status,
      String diagnosis,
      String advice,
      @NotEmpty List<@Valid PrescriptionItemRequest> items) {

    private PrescriptionIssuanceCommand toCommand() {
      return new PrescriptionIssuanceCommand(
          prescriptionCode,
          patientId,
          doctorId,
          appointmentId,
          status,
          diagnosis,
          advice,
          items.stream().map(PrescriptionItemRequest::toCommand).toList());
    }
  }

  public record PrescriptionItemRequest(
      @NotBlank String drugId, @Min(1) int quantity, @NotBlank String instructions) {

    private PrescriptionItemCommand toCommand() {
      return new PrescriptionItemCommand(drugId, quantity, instructions);
    }
  }
}
