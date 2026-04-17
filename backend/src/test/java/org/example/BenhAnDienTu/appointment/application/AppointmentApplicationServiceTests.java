package org.example.BenhAnDienTu.appointment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.example.BenhAnDienTu.appointment.api.AppointmentBookedEvent;
import org.example.BenhAnDienTu.appointment.api.AppointmentBookingCommand;
import org.example.BenhAnDienTu.appointment.api.AppointmentView;
import org.example.BenhAnDienTu.appointment.infrastructure.AppointmentLedgerAdapter;
import org.example.BenhAnDienTu.patient.api.PatientApi;
import org.example.BenhAnDienTu.patient.api.PatientView;
import org.example.BenhAnDienTu.staff.api.StaffApi;
import org.example.BenhAnDienTu.staff.api.StaffMemberView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AppointmentApplicationServiceTests {

  @Mock private AppointmentLedgerAdapter ledgerAdapter;
  @Mock private PatientApi patientApi;
  @Mock private StaffApi staffApi;
  @Mock private ApplicationEventPublisher eventPublisher;

  private AppointmentApplicationService service;

  @BeforeEach
  void setUp() {
    service =
        new AppointmentApplicationService(ledgerAdapter, patientApi, staffApi, eventPublisher);
  }

  @Test
  void bookAppointmentShouldRejectWhenPatientMissing() {
    AppointmentBookingCommand command = command("COMING", "not-used");
    when(patientApi.findPatient("patient-1")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.bookAppointment(command))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            throwable -> {
              ResponseStatusException exception = (ResponseStatusException) throwable;
              assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
              assertThat(exception.getReason())
                  .isEqualTo("Patient does not exist for appointment.");
            });

    verify(ledgerAdapter, never()).create(any());
  }

  @Test
  void bookAppointmentShouldRejectWhenServiceSelectionInvalid() {
    AppointmentBookingCommand command = command("COMING", "not-used");
    when(patientApi.findPatient("patient-1")).thenReturn(Optional.of(patient("patient-1")));
    when(staffApi.findStaffMember("doctor-1")).thenReturn(Optional.of(doctor("doctor-1")));
    when(ledgerAdapter.hasAllServiceIds(any())).thenReturn(false);

    assertThatThrownBy(() -> service.bookAppointment(command))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            throwable -> {
              ResponseStatusException exception = (ResponseStatusException) throwable;
              assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
              assertThat(exception.getReason())
                  .isEqualTo("One or more selected services do not exist or are inactive.");
            });

    verify(ledgerAdapter, never()).create(any());
  }

  @Test
  void bookAppointmentShouldNormalizeCommandAndPublishEvent() {
    AppointmentBookingCommand command = command("COMING", "should-clear");
    AppointmentView created = appointment("appointment-1");
    when(patientApi.findPatient("patient-1")).thenReturn(Optional.of(patient("patient-1")));
    when(staffApi.findStaffMember("doctor-1")).thenReturn(Optional.of(doctor("doctor-1")));
    when(ledgerAdapter.hasAllServiceIds(any())).thenReturn(true);
    when(ledgerAdapter.create(any(AppointmentBookingCommand.class))).thenReturn(created);

    AppointmentView result = service.bookAppointment(command);

    assertThat(result.id()).isEqualTo("appointment-1");

    ArgumentCaptor<AppointmentBookingCommand> captor =
        ArgumentCaptor.forClass(AppointmentBookingCommand.class);
    verify(ledgerAdapter).create(captor.capture());
    AppointmentBookingCommand normalized = captor.getValue();
    assertThat(normalized.cancelReason()).isNull();
    assertThat(normalized.serviceIds()).containsExactly("service-1", "service-2");

    verify(eventPublisher).publishEvent(any(AppointmentBookedEvent.class));
  }

  @Test
  void updateAppointmentShouldThrowNotFoundWhenAdapterReturnsEmpty() {
    AppointmentBookingCommand command = command("CANCEL", "Patient unavailable");
    when(patientApi.findPatient("patient-1")).thenReturn(Optional.of(patient("patient-1")));
    when(staffApi.findStaffMember("doctor-1")).thenReturn(Optional.of(doctor("doctor-1")));
    when(ledgerAdapter.hasAllServiceIds(any())).thenReturn(true);
    when(ledgerAdapter.update(any(), any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.updateAppointment("appointment-404", command))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            throwable -> {
              ResponseStatusException exception = (ResponseStatusException) throwable;
              assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
              assertThat(exception.getReason())
                  .isEqualTo("Appointment does not exist: appointment-404");
            });
  }

  private static AppointmentBookingCommand command(String status, String cancelReason) {
    return new AppointmentBookingCommand(
        "AP-001",
        Instant.parse("2026-04-04T09:00:00Z"),
        status,
        cancelReason,
        "doctor-1",
        "patient-1",
        2,
        "NONE",
        false,
        6,
        List.of(" service-1 ", "service-2", "service-1", " "));
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

  private static PatientView patient(String patientId) {
    Instant now = Instant.parse("2026-04-04T04:00:00Z");
    return new PatientView(
        patientId,
        "PT-001",
        "Patient Local",
        null,
        null,
        "Male",
        "0900000000",
        null,
        null,
        null,
        null,
        null,
        now,
        now);
  }

  private static StaffMemberView doctor(String doctorId) {
    return new StaffMemberView(
        doctorId, "Doctor Local", "doctor.local@EMR.dev", "0900000001", "DOCTOR");
  }
}
