package org.example.BenhAnDienTu.staff.api;

/** Command for updating doctor-managed staff profile and active state. */
public record StaffDoctorUpdateCommand(
    String email,
    String fullName,
    String gender,
    String phone,
    String address,
    String serviceId,
    boolean confirmed,
    boolean active) {}
