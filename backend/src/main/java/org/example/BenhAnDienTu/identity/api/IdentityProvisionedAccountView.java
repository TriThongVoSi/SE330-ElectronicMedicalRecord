package org.example.BenhAnDienTu.identity.api;

/** Result payload returned by internal account provisioning workflows. */
public record IdentityProvisionedAccountView(
    String actorId, String username, String email, String role, boolean mustChangePassword) {}
