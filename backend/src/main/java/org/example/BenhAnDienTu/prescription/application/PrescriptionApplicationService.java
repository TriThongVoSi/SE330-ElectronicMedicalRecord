package org.example.BenhAnDienTu.prescription.application;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.example.BenhAnDienTu.appointment.api.AppointmentApi;
import org.example.BenhAnDienTu.patient.api.PatientApi;
import org.example.BenhAnDienTu.prescription.api.PrescriptionApi;
import org.example.BenhAnDienTu.prescription.api.PrescriptionIssuanceCommand;
import org.example.BenhAnDienTu.prescription.api.PrescriptionIssuedEvent;
import org.example.BenhAnDienTu.prescription.api.PrescriptionListQuery;
import org.example.BenhAnDienTu.prescription.api.PrescriptionPageView;
import org.example.BenhAnDienTu.prescription.api.PrescriptionView;
import org.example.BenhAnDienTu.prescription.infrastructure.PrescriptionLedgerAdapter;
import org.example.BenhAnDienTu.staff.api.StaffApi;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PrescriptionApplicationService implements PrescriptionApi {

  private final PrescriptionLedgerAdapter ledgerAdapter;
  private final AppointmentApi appointmentApi;
  private final PatientApi patientApi;
  private final StaffApi staffApi;
  private final ApplicationEventPublisher eventPublisher;

  public PrescriptionApplicationService(
      PrescriptionLedgerAdapter ledgerAdapter,
      AppointmentApi appointmentApi,
      PatientApi patientApi,
      StaffApi staffApi,
      ApplicationEventPublisher eventPublisher) {
    this.ledgerAdapter = ledgerAdapter;
    this.appointmentApi = appointmentApi;
    this.patientApi = patientApi;
    this.staffApi = staffApi;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public PrescriptionPageView listPrescriptions(PrescriptionListQuery query) {
    return ledgerAdapter.list(query);
  }

  @Override
  @Transactional
  public PrescriptionView issuePrescription(PrescriptionIssuanceCommand command) {
    validateBoundaryReferences(command);

    try {
      PrescriptionView created = ledgerAdapter.create(command);
      eventPublisher.publishEvent(
          new PrescriptionIssuedEvent(created.id(), created.appointmentId(), Instant.now()));
      return created;
    } catch (DataIntegrityViolationException exception) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Prescription code or appointment already used.", exception);
    }
  }

  @Override
  @Transactional
  public PrescriptionView updatePrescription(
      String prescriptionId, PrescriptionIssuanceCommand command) {
    validateBoundaryReferences(command);

    try {
      return ledgerAdapter
          .update(prescriptionId, command)
          .orElseThrow(
              () ->
                  new ResponseStatusException(
                      HttpStatus.NOT_FOUND, "Prescription does not exist: " + prescriptionId));
    } catch (DataIntegrityViolationException exception) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Prescription code or appointment already used.", exception);
    }
  }

  @Override
  public Optional<PrescriptionView> findPrescription(String prescriptionId) {
    return ledgerAdapter.findById(prescriptionId);
  }

  private void validateBoundaryReferences(PrescriptionIssuanceCommand command) {
    if (command.items() == null || command.items().isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Prescription must contain at least one item.");
    }

    var appointment =
        appointmentApi
            .findAppointment(command.appointmentId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Appointment does not exist for prescription."));

    if (!Objects.equals(appointment.patientId(), command.patientId())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Prescription patient does not match appointment patient.");
    }

    if (!Objects.equals(appointment.doctorId(), command.doctorId())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Prescription doctor does not match appointment doctor.");
    }

    if (patientApi.findPatient(command.patientId()).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Patient does not exist.");
    }
    if (staffApi.findStaffMember(command.doctorId()).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Doctor does not exist.");
    }
  }
}
