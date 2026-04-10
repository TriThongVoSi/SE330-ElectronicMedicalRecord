package org.example.BenhAnDienTu.identity.api;

/** Authenticated user payload exposed by identity API. */
public record AuthUserView(
    String id,
    String username,
    String fullName,
    String email,
    String role,
    boolean mustChangePassword) {}
