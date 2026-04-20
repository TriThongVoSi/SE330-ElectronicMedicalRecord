package org.example.BenhAnDienTu.staff.api;

/** Detailed staff profile view used by staff management endpoints. */
public record StaffProfileView(
    String id,
    String username,
    String fullName,
    String email,
    String phone,
    String gender,
    String address,
    String role,
    String status,
    boolean active,
    boolean confirmed,
    String serviceId) {}
