package org.example.BenhAnDienTu.appointment.api;

import java.time.Instant;

/** Read-only available slot projection used in patient booking screens. */
public record PatientPortalAvailableSlotView(
    String slotId, String doctorId, Instant slotTime, int durationMinutes, boolean booked) {}
