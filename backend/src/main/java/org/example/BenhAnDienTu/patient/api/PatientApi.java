package org.example.BenhAnDienTu.patient.api;

import java.util.Optional;

/** Public contract for patient registration and read access. */
public interface PatientApi {

  PatientPageView listPatients(PatientListQuery query);

  PatientView createPatient(PatientUpsertCommand command);

  PatientView updatePatient(String patientId, PatientUpsertCommand command);

  Optional<PatientView> findPatient(String patientId);

  Optional<String> findPatientIdByActorId(String actorId);

  Optional<PatientView> findPatientByActorId(String actorId);

  PatientView updateCurrentPatientProfile(String actorId, PatientSelfProfileUpdateCommand command);
}
