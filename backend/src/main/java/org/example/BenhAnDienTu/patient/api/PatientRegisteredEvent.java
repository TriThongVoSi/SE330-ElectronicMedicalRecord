package org.example.BenhAnDienTu.patient.api;

import java.time.Instant;

/** Event published after a patient has been registered. */
public record PatientRegisteredEvent(String patientId, Instant occurredAt) {}
