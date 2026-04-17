package org.example.BenhAnDienTu.patient.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.example.BenhAnDienTu.identity.api.IdentityProvisionedAccountView;
import org.example.BenhAnDienTu.identity.api.IdentityProvisioningApi;
import org.example.BenhAnDienTu.patient.api.PatientPageView;
import org.example.BenhAnDienTu.patient.api.PatientRegisteredEvent;
import org.example.BenhAnDienTu.patient.api.PatientUpsertCommand;
import org.example.BenhAnDienTu.patient.api.PatientView;
import org.example.BenhAnDienTu.patient.infrastructure.PatientRegistryAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PatientApplicationServiceTests {

  @Mock private PatientRegistryAdapter registryAdapter;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private IdentityProvisioningApi provisioningApi;

  private PatientApplicationService service;

  @BeforeEach
  void setUp() {
    service = new PatientApplicationService(registryAdapter, eventPublisher, provisioningApi);
  }

  @Test
  void createPatientShouldPublishEventWhenSuccess() {
    PatientUpsertCommand command = command("PT-001");
    PatientView created = patient("patient-1", "PT-001");
    when(registryAdapter.create(command)).thenReturn(created);
    when(provisioningApi.provisionPatientAccount(any()))
        .thenReturn(
            new IdentityProvisionedAccountView(
                "patient-actor-1", "patient-pt-001", "john.smith@EMR.dev", "PATIENT", true));

    PatientView result = service.createPatient(command);

    assertThat(result.id()).isEqualTo("patient-1");
    verify(registryAdapter).create(command);
    verify(provisioningApi).provisionPatientAccount(any());
    verify(eventPublisher).publishEvent(any(PatientRegisteredEvent.class));
  }

  @Test
  void createPatientShouldThrowConflictWhenCodeExists() {
    PatientUpsertCommand command = command("PT-001");
    when(registryAdapter.create(command))
        .thenThrow(new DataIntegrityViolationException("duplicate patient code"));

    assertThatThrownBy(() -> service.createPatient(command))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            throwable -> {
              ResponseStatusException exception = (ResponseStatusException) throwable;
              assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
              assertThat(exception.getReason()).isEqualTo("Patient code must be unique.");
            });

    verify(eventPublisher, never()).publishEvent(any(PatientRegisteredEvent.class));
  }

  @Test
  void updatePatientShouldThrowNotFoundWhenPatientDoesNotExist() {
    PatientUpsertCommand command = command("PT-001");
    when(registryAdapter.update("patient-404", command)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.updatePatient("patient-404", command))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            throwable -> {
              ResponseStatusException exception = (ResponseStatusException) throwable;
              assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
              assertThat(exception.getReason()).isEqualTo("Patient does not exist: patient-404");
            });
  }

  @Test
  void findPatientShouldDelegateToRegistryAdapter() {
    PatientView view = patient("patient-1", "PT-001");
    when(registryAdapter.findById("patient-1")).thenReturn(Optional.of(view));

    Optional<PatientView> result = service.findPatient("patient-1");

    assertThat(result).contains(view);
  }

  @Test
  void listPatientsShouldDelegateToRegistryAdapter() {
    PatientPageView page =
        new PatientPageView(List.of(patient("patient-1", "PT-001")), 1, 10, 1, 1);
    when(registryAdapter.list(any())).thenReturn(page);

    PatientPageView result =
        service.listPatients(
            new org.example.BenhAnDienTu.patient.api.PatientListQuery(1, 10, "john", null, null));

    assertThat(result.totalItems()).isEqualTo(1);
    verify(registryAdapter).list(any());
  }

  private static PatientUpsertCommand command(String patientCode) {
    return new PatientUpsertCommand(
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
        BigDecimal.valueOf(70));
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
