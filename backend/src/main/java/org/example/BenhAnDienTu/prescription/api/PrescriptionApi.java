package org.example.BenhAnDienTu.prescription.api;

import java.util.Optional;

/** Public contract for creating and retrieving prescriptions. */
public interface PrescriptionApi {

  PrescriptionPageView listPrescriptions(PrescriptionListQuery query);

  PrescriptionView issuePrescription(PrescriptionIssuanceCommand command);

  PrescriptionView updatePrescription(String prescriptionId, PrescriptionIssuanceCommand command);

  Optional<PrescriptionView> findPrescription(String prescriptionId);
}
