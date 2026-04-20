package org.example.BenhAnDienTu.staff.api;

/** Command for updating profile information of current actor. */
public record StaffProfileUpdateCommand(
    String fullName, String email, String gender, String phone, String address) {}
