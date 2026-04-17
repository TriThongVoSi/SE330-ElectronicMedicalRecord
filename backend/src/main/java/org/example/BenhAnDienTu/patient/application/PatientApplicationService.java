package org.example.BenhAnDienTu.patient.application;

import java.time.Instant;
import java.util.Optional;
import org.example.BenhAnDienTu.identity.api.IdentityProvisionPatientCommand;
import org.example.BenhAnDienTu.identity.api.IdentityProvisioningApi;
import org.example.BenhAnDienTu.patient.api.PatientApi;
import org.example.BenhAnDienTu.patient.api.PatientListQuery;
import org.example.BenhAnDienTu.patient.api.PatientPageView;
import org.example.BenhAnDienTu.patient.api.PatientRegisteredEvent;
import org.example.BenhAnDienTu.patient.api.PatientSelfProfileUpdateCommand;
import org.example.BenhAnDienTu.patient.api.PatientUpsertCommand;
import org.example.BenhAnDienTu.patient.api.PatientView;
import org.example.BenhAnDienTu.patient.infrastructure.PatientRegistryAdapter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PatientApplicationService implements PatientApi {

  private final PatientRegistryAdapter registryAdapter;
  private final ApplicationEventPublisher eventPublisher;
  private final IdentityProvisioningApi provisioningApi;

  public PatientApplicationService(
      PatientRegistryAdapter registryAdapter,
      ApplicationEventPublisher eventPublisher,
      IdentityProvisioningApi provisioningApi) {
    this.registryAdapter = registryAdapter;
    this.eventPublisher = eventPublisher;
    this.provisioningApi = provisioningApi;
  }

  @Override
  public PatientPageView listPatients(PatientListQuery query) {
    return registryAdapter.list(query);
  }

  @Override
  @Transactional
  public PatientView createPatient(PatientUpsertCommand command) {
    try {
      PatientView created = registryAdapter.create(command);
      provisioningApi.provisionPatientAccount(
          new IdentityProvisionPatientCommand(
              created.id(), created.patientCode(), created.email(), created.fullName(), created.gender()));
      eventPublisher.publishEvent(new PatientRegisteredEvent(created.id(), Instant.now()));
      return created;
    } catch (DataIntegrityViolationException exception) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Patient code must be unique.", exception);
    }
  }

  @Override
  public PatientView updatePatient(String patientId, PatientUpsertCommand command) {
    try {
      return registryAdapter
          .update(patientId, command)
          .orElseThrow(
              () ->
                  new ResponseStatusException(
                      HttpStatus.NOT_FOUND, "Patient does not exist: " + patientId));
    } catch (DataIntegrityViolationException exception) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Patient code must be unique.", exception);
    }
  }

  @Override
  public Optional<PatientView> findPatient(String patientId) {
    return registryAdapter.findById(patientId);
  }

  @Override
  public Optional<String> findPatientIdByActorId(String actorId) {
    return registryAdapter.findPatientIdByActorId(actorId);
  }

  @Override
  public Optional<PatientView> findPatientByActorId(String actorId) {
    return registryAdapter.findByActorId(actorId);
  }

  @Override
  public PatientView updateCurrentPatientProfile(
      String actorId, PatientSelfProfileUpdateCommand command) {
    return registryAdapter
        .updateCurrentPatientProfile(actorId, command)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Patient profile does not exist for actor: " + actorId));
  }
}
