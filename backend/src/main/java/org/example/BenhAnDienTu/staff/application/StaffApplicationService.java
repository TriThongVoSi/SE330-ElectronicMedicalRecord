package org.example.BenhAnDienTu.staff.application;

import java.util.List;
import java.util.Optional;
import org.example.BenhAnDienTu.identity.api.IdentityProvisionDoctorCommand;
import org.example.BenhAnDienTu.identity.api.IdentityProvisionedAccountView;
import org.example.BenhAnDienTu.identity.api.IdentityProvisioningApi;
import org.example.BenhAnDienTu.staff.api.StaffApi;
import org.example.BenhAnDienTu.staff.api.StaffDoctorCreateCommand;
import org.example.BenhAnDienTu.staff.api.StaffDoctorUpdateCommand;
import org.example.BenhAnDienTu.staff.api.StaffMemberView;
import org.example.BenhAnDienTu.staff.api.StaffProfileUpdateCommand;
import org.example.BenhAnDienTu.staff.api.StaffProfileView;
import org.example.BenhAnDienTu.staff.infrastructure.StaffDirectoryAdapter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StaffApplicationService implements StaffApi {

  private final StaffDirectoryAdapter directoryAdapter;
  private final IdentityProvisioningApi provisioningApi;

  public StaffApplicationService(
      StaffDirectoryAdapter directoryAdapter, IdentityProvisioningApi provisioningApi) {
    this.directoryAdapter = directoryAdapter;
    this.provisioningApi = provisioningApi;
  }

  @Override
  public Optional<StaffMemberView> findStaffMember(String staffId) {
    return directoryAdapter.findById(staffId);
  }

  @Override
  public List<StaffMemberView> listDoctors() {
    return directoryAdapter.findDoctors();
  }

  @Override
  public Optional<StaffProfileView> findDoctorProfile(String staffId) {
    return directoryAdapter.findDoctorProfileById(staffId);
  }

  @Override
  @Transactional
  public StaffProfileView createDoctor(StaffDoctorCreateCommand command) {
    try {
      IdentityProvisionedAccountView provisionedAccount =
          provisioningApi.provisionDoctorAccount(
              new IdentityProvisionDoctorCommand(
                  command.username(),
                  command.email(),
                  command.fullName(),
                  command.gender(),
                  command.rawPassword(),
                  command.active()));

      return directoryAdapter
          .updateDoctor(provisionedAccount.actorId(), toUpdateCommand(command))
          .orElseThrow(
              () ->
                  new ResponseStatusException(
                      HttpStatus.NOT_FOUND,
                      "Doctor profile does not exist for actor: " + provisionedAccount.actorId()));
    } catch (DataIntegrityViolationException exception) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Doctor username or email already exists, or linked service is invalid.",
          exception);
    }
  }

  @Override
  @Transactional
  public StaffProfileView updateDoctor(String staffId, StaffDoctorUpdateCommand command) {
    try {
      return directoryAdapter
          .updateDoctor(staffId, command)
          .orElseThrow(
              () ->
                  new ResponseStatusException(
                      HttpStatus.NOT_FOUND, "Doctor does not exist: " + staffId));
    } catch (DataIntegrityViolationException exception) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Doctor email already exists, or linked service is invalid.",
          exception);
    }
  }

  @Override
  @Transactional
  public void deactivateDoctor(String staffId) {
    boolean updated = directoryAdapter.deactivateDoctor(staffId);
    if (!updated) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor does not exist: " + staffId);
    }
  }

  @Override
  public Optional<StaffProfileView> findMyProfile(String actorId) {
    return directoryAdapter.findDoctorProfileByActorId(actorId);
  }

  @Override
  @Transactional
  public StaffProfileView updateMyProfile(String actorId, StaffProfileUpdateCommand command) {
    try {
      return directoryAdapter
          .updateProfile(actorId, command)
          .orElseThrow(
              () ->
                  new ResponseStatusException(
                      HttpStatus.NOT_FOUND, "Doctor profile does not exist for actor: " + actorId));
    } catch (DataIntegrityViolationException exception) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Profile email already exists.", exception);
    }
  }

  private static StaffDoctorUpdateCommand toUpdateCommand(StaffDoctorCreateCommand command) {
    return new StaffDoctorUpdateCommand(
        command.email(),
        command.fullName(),
        command.gender(),
        command.phone(),
        command.address(),
        command.serviceId(),
        command.confirmed(),
        command.active());
  }
}
