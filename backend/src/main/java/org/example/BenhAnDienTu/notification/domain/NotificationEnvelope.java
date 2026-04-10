package org.example.BenhAnDienTu.notification.domain;

import java.time.Instant;

/** Domain root placeholder for notification payload ownership. */
public record NotificationEnvelope(
    String channel, String subject, String body, String recipient, Instant createdAt) {}
