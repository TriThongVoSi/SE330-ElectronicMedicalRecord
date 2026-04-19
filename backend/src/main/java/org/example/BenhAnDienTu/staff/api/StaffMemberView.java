package org.example.BenhAnDienTu.staff.api;

/** Read-only staff view exposed to other modules. */
public record StaffMemberView(
    String id, String fullName, String email, String phone, String role) {}
