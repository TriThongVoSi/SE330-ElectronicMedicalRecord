package org.example.BenhAnDienTu.identity.api;

/** Provisioning command for internally created patient accounts. */
public record IdentityProvisionPatientCommand(
    String patientId, String patientCode, String email, String fullName, String gender) {}
