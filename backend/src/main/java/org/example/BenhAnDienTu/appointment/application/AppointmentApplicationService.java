package org.example.BenhAnDienTu.appointment.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.example.BenhAnDienTu.appointment.api.AppointmentApi;
import org.example.BenhAnDienTu.appointment.api.AppointmentBookedEvent;
import org.example.BenhAnDienTu.appointment.api.AppointmentBookingCommand;
import org.example.BenhAnDienTu.appointment.api.AppointmentListQuery;
import org.example.BenhAnDienTu.appointment.api.AppointmentPageView;
import org.example.BenhAnDienTu.appointment.api.AppointmentView;
import org.example.BenhAnDienTu.appointment.infrastructure.AppointmentLedgerAdapter;
import org.example.BenhAnDienTu.patient.api.PatientApi;
import org.example.BenhAnDienTu.staff.api.StaffApi;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AppointmentApplicationService implements AppointmentApi {

  private final AppointmentLedgerAdapter ledgerAdapter;
  private final PatientApi patientApi;
  private final StaffApi staffApi;
  private final ApplicationEventPublisher eventPublisher;

  public AppointmentApplicationService(
      AppointmentLedgerAdapter ledgerAdapter,
      PatientApi patientApi,
      StaffApi staffApi,
      ApplicationEventPublisher eventPublisher) {
    this.ledgerAdapter = ledgerAdapter;
    this.patientApi = patientApi;
    this.staffApi = staffApi;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public AppointmentPageView listAppointments(AppointmentListQuery query) {
    return ledgerAdapter.list(query);
  }

  @Override
  public AppointmentView bookAppointment(AppointmentBookingCommand command) {
    AppointmentBookingCommand normalized = normalize(command);
    validateBoundaryReferences(normalized.patientId(), normalized.doctorId());
    validateServiceReferences(normalized.serviceIds());

    try {
      AppointmentView created = ledgerAdapter.create(normalized);
      eventPublisher.publishEvent(
          new AppointmentBookedEvent(created.id(), created.patientId(), Instant.now()));
      return created;
    } catch (DataIntegrityViolationException exception) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Appointment code or doctor/time already exists.", exception);
    }
  }

  @Override
  public AppointmentView updateAppointment(
      String appointmentId, AppointmentBookingCommand command) {
    AppointmentBookingCommand normalized = normalize(command);
    validateBoundaryReferences(normalized.patientId(), normalized.doctorId());
    validateServiceReferences(normalized.serviceIds());

    try {
      return ledgerAdapter
          .update(appointmentId, normalized)
          .orElseThrow(
              () ->
                  new ResponseStatusException(
                      HttpStatus.NOT_FOUND, "Appointment does not exist: " + appointmentId));
    } catch (DataIntegrityViolationException exception) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Appointment code or doctor/time already exists.", exception);
    }
  }

  @Override
  public Optional<AppointmentView> findAppointment(String appointmentId) {
    return ledgerAdapter.findById(appointmentId);
  }

  private void validateBoundaryReferences(String patientId, String doctorId) {
    if (patientApi.findPatient(patientId).isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Patient does not exist for appointment.");
    }
    if (staffApi.findStaffMember(doctorId).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Doctor does not exist.");
    }
  }

  private void validateServiceReferences(List<String> serviceIds) {
    if (!ledgerAdapter.hasAllServiceIds(serviceIds)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "One or more selected services do not exist or are inactive.");
    }
  }

  private static AppointmentBookingCommand normalize(AppointmentBookingCommand command) {
    String cancelReason = "CANCEL".equals(command.status()) ? command.cancelReason() : null;
    List<String> normalizedServiceIds = normalizeServiceIds(command.serviceIds());
    return new AppointmentBookingCommand(
        command.appointmentCode(),
        command.appointmentTime(),
        command.status(),
        cancelReason,
        command.doctorId(),
        command.patientId(),
        command.urgencyLevel(),
        command.prescriptionStatus(),
        command.isFollowup(),
        command.priorityScore(),
        normalizedServiceIds);
  }

  private static List<String> normalizeServiceIds(List<String> serviceIds) {
    if (serviceIds == null || serviceIds.isEmpty()) {
      return List.of();
    }

    LinkedHashSet<String> normalized = new LinkedHashSet<>();
    for (String rawServiceId : serviceIds) {
      if (rawServiceId == null) {
        continue;
      }
      String trimmed = rawServiceId.trim();
      if (!trimmed.isEmpty()) {
        normalized.add(trimmed);
      }
    }
    return new ArrayList<>(normalized);
  }
}
