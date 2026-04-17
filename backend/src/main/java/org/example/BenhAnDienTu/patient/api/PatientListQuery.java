package org.example.BenhAnDienTu.patient.api;

/** Paged patient list query exposed via patient boundary. */
public record PatientListQuery(int page, int size, String search, String phone, String code) {}
