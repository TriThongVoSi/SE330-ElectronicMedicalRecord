package org.example.BenhAnDienTu.identity.api;

/** Provisioning command for internally created doctor accounts. */
public record IdentityProvisionDoctorCommand(
    String username,
    String email,
    String fullName,
    String gender,
    String preferredTemporaryPassword,
    boolean active) {}
