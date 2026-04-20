package org.example.BenhAnDienTu.staff.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.example.BenhAnDienTu.identity.api.IdentityProvisionedAccountView;
import org.example.BenhAnDienTu.identity.api.IdentityProvisioningApi;
import org.example.BenhAnDienTu.staff.api.StaffDoctorCreateCommand;
import org.example.BenhAnDienTu.staff.api.StaffDoctorUpdateCommand;
import org.example.BenhAnDienTu.staff.api.StaffProfileUpdateCommand;
import org.example.BenhAnDienTu.staff.api.StaffProfileView;
import org.example.BenhAnDienTu.staff.infrastructure.StaffDirectoryAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class StaffApplicationServiceTests {

  @Mock private StaffDirectoryAdapter directoryAdapter;
  @Mock private IdentityProvisioningApi provisioningApi;

  private StaffApplicationService service;

  @BeforeEach
  void setUp() {
    service = new StaffApplicationService(directoryAdapter, provisioningApi);
  }

  @Test
  void createDoctorShouldProvisionIdentityAccountBeforePersistingProfile() {
    StaffDoctorCreateCommand command = createCommand();
    StaffProfileView profile = profile("doctor-1");
    when(provisioningApi.provisionDoctorAccount(any()))
        .thenReturn(
            new IdentityProvisionedAccountView(
                "doctor-1", "doctor.local", "doctor.local@EMR.dev", "DOCTOR", true));
    when(directoryAdapter.updateDoctor(any(), any())).thenReturn(Optional.of(profile));

    StaffProfileView result = service.createDoctor(command);

    assertThat(result.id()).isEqualTo("doctor-1");
    verify(provisioningApi).provisionDoctorAccount(any());
    verify(directoryAdapter).updateDoctor(any(), any());
  }

  @Test
  void updateDoctorShouldThrowNotFoundWhenDoctorMissing() {
    StaffDoctorUpdateCommand command = updateCommand();
    when(directoryAdapter.updateDoctor("doctor-404", command)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.updateDoctor("doctor-404", command))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            throwable -> {
              ResponseStatusException exception = (ResponseStatusException) throwable;
              assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
              assertThat(exception.getReason()).isEqualTo("Doctor does not exist: doctor-404");
            });
  }

  @Test
  void deactivateDoctorShouldThrowNotFoundWhenDoctorMissing() {
    when(directoryAdapter.deactivateDoctor("doctor-404")).thenReturn(false);

    assertThatThrownBy(() -> service.deactivateDoctor("doctor-404"))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            throwable -> {
              ResponseStatusException exception = (ResponseStatusException) throwable;
              assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
              assertThat(exception.getReason()).isEqualTo("Doctor does not exist: doctor-404");
            });
  }

  @Test
  void updateMyProfileShouldThrowNotFoundWhenProfileMissing() {
    StaffProfileUpdateCommand command =
        new StaffProfileUpdateCommand(
            "Doctor Local", "doctor.local@EMR.dev", "Female", "0900", "HCMC");
    when(directoryAdapter.updateProfile("doctor-404", command)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.updateMyProfile("doctor-404", command))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            throwable -> {
              ResponseStatusException exception = (ResponseStatusException) throwable;
              assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
              assertThat(exception.getReason())
                  .isEqualTo("Doctor profile does not exist for actor: doctor-404");
            });
  }

  @Test
  void findMyProfileShouldReturnAdapterValue() {
    StaffProfileView profile = profile("doctor-1");
    when(directoryAdapter.findDoctorProfileByActorId("doctor-1")).thenReturn(Optional.of(profile));

    Optional<StaffProfileView> result = service.findMyProfile("doctor-1");

    assertThat(result).contains(profile);
  }

  private static StaffDoctorCreateCommand createCommand() {
    return new StaffDoctorCreateCommand(
        "doctor.local",
        "doctor123",
        "doctor.local@EMR.dev",
        "Doctor Local",
        "Female",
        "0900000000",
        "HCMC",
        null,
        true,
        true);
  }

  private static StaffDoctorUpdateCommand updateCommand() {
    return new StaffDoctorUpdateCommand(
        "doctor.local@EMR.dev",
        "Doctor Local Updated",
        "Female",
        "0900000001",
        "Hanoi",
        null,
        true,
        true);
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
