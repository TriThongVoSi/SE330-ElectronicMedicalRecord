package org.example.BenhAnDienTu.notification.api;

import java.time.Instant;

/** Response contract after notification dispatch is accepted. */
public record NotificationReceipt(String messageId, String status, Instant acceptedAt) {}
