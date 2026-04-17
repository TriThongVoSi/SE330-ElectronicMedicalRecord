package org.example.BenhAnDienTu.appointment.api;

/** Doctor option payload for patient self-booking. */
public record PatientPortalDoctorView(String id, String fullName, String email, String phone) {}
