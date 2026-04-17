package org.example.BenhAnDienTu.prescription.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.example.BenhAnDienTu.appointment.api.AppointmentApi;
import org.example.BenhAnDienTu.appointment.api.AppointmentView;
import org.example.BenhAnDienTu.patient.api.PatientApi;
import org.example.BenhAnDienTu.patient.api.PatientView;
import org.example.BenhAnDienTu.prescription.api.PrescriptionIssuanceCommand;
import org.example.BenhAnDienTu.prescription.api.PrescriptionIssuedEvent;
import org.example.BenhAnDienTu.prescription.api.PrescriptionItemCommand;
import org.example.BenhAnDienTu.prescription.api.PrescriptionItemView;
import org.example.BenhAnDienTu.prescription.api.PrescriptionView;
import org.example.BenhAnDienTu.prescription.infrastructure.PrescriptionLedgerAdapter;
import org.example.BenhAnDienTu.staff.api.StaffApi;
import org.example.BenhAnDienTu.staff.api.StaffMemberView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PrescriptionApplicationServiceTests {

  @Mock private PrescriptionLedgerAdapter ledgerAdapter;
  @Mock private AppointmentApi appointmentApi;
  @Mock private PatientApi patientApi;
  @Mock private StaffApi staffApi;
  @Mock private ApplicationEventPublisher eventPublisher;

  private PrescriptionApplicationService service;

  @BeforeEach
  void setUp() {
    service =
        new PrescriptionApplicationService(
            ledgerAdapter, appointmentApi, patientApi, staffApi, eventPublisher);
  }

  @Test
  void issuePrescriptionShouldRejectWhenAppointmentPatientDoesNotMatch() {
    PrescriptionIssuanceCommand command = command("patient-2", "doctor-1", "appointment-1");
    when(appointmentApi.findAppointment("appointment-1"))
        .thenReturn(Optional.of(appointment("appointment-1", "patient-1", "doctor-1")));

    assertThatThrownBy(() -> service.issuePrescription(command))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            throwable -> {
              ResponseStatusException exception = (ResponseStatusException) throwable;
              assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
              assertThat(exception.getReason())
                  .isEqualTo("Prescription patient does not match appointment patient.");
            });

    verify(ledgerAdapter, never()).create(any());
  }

  @Test
  void issuePrescriptionShouldRejectWhenAppointmentDoctorDoesNotMatch() {
    PrescriptionIssuanceCommand command = command("patient-1", "doctor-2", "appointment-1");
    when(appointmentApi.findAppointment("appointment-1"))
        .thenReturn(Optional.of(appointment("appointment-1", "patient-1", "doctor-1")));

    assertThatThrownBy(() -> service.issuePrescription(command))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            throwable -> {
              ResponseStatusException exception = (ResponseStatusException) throwable;
              assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
              assertThat(exception.getReason())
                  .isEqualTo("Prescription doctor does not match appointment doctor.");
            });

    verify(ledgerAdapter, never()).create(any());
  }

  @Test
  void updatePrescriptionShouldRejectWhenAppointmentDoctorDoesNotMatch() {
    PrescriptionIssuanceCommand command = command("patient-1", "doctor-2", "appointment-1");
    when(appointmentApi.findAppointment("appointment-1"))
        .thenReturn(Optional.of(appointment("appointment-1", "patient-1", "doctor-1")));

    assertThatThrownBy(() -> service.updatePrescription("rx-1", command))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            throwable -> {
              ResponseStatusException exception = (ResponseStatusException) throwable;
              assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
              assertThat(exception.getReason())
                  .isEqualTo("Prescription doctor does not match appointment doctor.");
            });

    verify(ledgerAdapter, never()).update(any(), any());
  }

  @Test
  void issuePrescriptionShouldSucceedWhenAppointmentPatientAndDoctorMatch() {
    PrescriptionIssuanceCommand command = command("patient-1", "doctor-1", "appointment-1");
    when(appointmentApi.findAppointment("appointment-1"))
        .thenReturn(Optional.of(appointment("appointment-1", "patient-1", "doctor-1")));
    when(patientApi.findPatient("patient-1")).thenReturn(Optional.of(patient("patient-1")));
    when(staffApi.findStaffMember("doctor-1")).thenReturn(Optional.of(doctor("doctor-1")));
    when(ledgerAdapter.create(command)).thenReturn(prescription("rx-1", command));

    PrescriptionView result = service.issuePrescription(command);

    assertThat(result.id()).isEqualTo("rx-1");
    verify(ledgerAdapter).create(command);
    verify(eventPublisher).publishEvent(any(PrescriptionIssuedEvent.class));
  }

  private static PrescriptionIssuanceCommand command(
      String patientId, String doctorId, String appointmentId) {
    return new PrescriptionIssuanceCommand(
        "RX-001",
        patientId,
        doctorId,
        appointmentId,
        "DRAFT",
        "Upper respiratory infection",
        "Drink warm water and rest",
        List.of(new PrescriptionItemCommand("drug-1", 2, "After meals")));
  }

  private static AppointmentView appointment(
      String appointmentId, String patientId, String doctorId) {
    Instant now = Instant.parse("2026-04-04T03:00:00Z");
    return new AppointmentView(
        appointmentId,
        "AP-001",
        now,
        "CONFIRMED",
        null,
        doctorId,
        "Doctor Local",
        patientId,
        "Patient Local",
        1,
        "NOT_ISSUED",
        false,
        5,
        List.of("service-1"),
        now,
        now);
  }

  private static PatientView patient(String patientId) {
    Instant now = Instant.parse("2026-04-04T03:00:00Z");
    return new PatientView(
        patientId,
        "PT-001",
        "Patient Local",
        null,
        null,
        null,
        null,
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
        doctorId, "Doctor Local", "doctor.local@EMR.dev", "0900000000", "DOCTOR");
  }

  private static PrescriptionView prescription(
      String prescriptionId, PrescriptionIssuanceCommand command) {
    return new PrescriptionView(
        prescriptionId,
        command.prescriptionCode(),
        command.patientId(),
        "Patient Local",
        command.doctorId(),
        "Doctor Local",
        command.appointmentId(),
        command.status(),
        command.diagnosis(),
        command.advice(),
        List.of(new PrescriptionItemView("drug-1", "Drug 1", 2, "After meals")),
        Instant.parse("2026-04-04T03:00:00Z"));
  }
}
