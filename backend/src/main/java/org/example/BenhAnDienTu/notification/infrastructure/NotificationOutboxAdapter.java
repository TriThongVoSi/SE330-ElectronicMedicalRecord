package org.example.BenhAnDienTu.notification.infrastructure;

import org.example.BenhAnDienTu.notification.domain.NotificationEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NotificationOutboxAdapter {

  private static final Logger LOGGER = LoggerFactory.getLogger(NotificationOutboxAdapter.class);

  public void enqueue(String messageId, NotificationEnvelope envelope) {
    LOGGER.info(
        "Notification queued: id={}, channel={}, subject={}, recipient={}",
        messageId,
        envelope.channel(),
        envelope.subject(),
        maskRecipient(envelope.recipient()));
  }

  private static String maskRecipient(String recipient) {
    if (recipient == null || recipient.isBlank() || !recipient.contains("@")) {
      return "";
    }
    String[] parts = recipient.split("@", 2);
    if (parts[0].length() <= 2) {
      return "*@" + parts[1];
    }
    return parts[0].charAt(0) + "***" + parts[0].charAt(parts[0].length() - 1) + "@" + parts[1];
  }
}
