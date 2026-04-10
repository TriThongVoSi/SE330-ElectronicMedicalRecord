package org.example.BenhAnDienTu.identity.domain;

import java.util.Set;

/** Domain root placeholder for identity ownership. */
public record IdentityAccount(
    String actorId,
    String principal,
    String fullName,
    String email,
    String role,
    String password,
    boolean active,
    String status,
    boolean mustChangePassword,
    String linkedPatientId,
    Set<String> permissions) {}
