package org.example.BenhAnDienTu.staff.api;

/** Command for creating a doctor account with staff profile details. */
public record StaffDoctorCreateCommand(
    String username,
    String rawPassword,
    String email,
    String fullName,
    String gender,
    String phone,
    String address,
    String serviceId,
    boolean confirmed,
    boolean active) {}
