package org.example.BenhAnDienTu.notification.application;

import java.time.Instant;
import java.util.UUID;
import org.example.BenhAnDienTu.notification.api.NotificationCommand;
import org.example.BenhAnDienTu.notification.api.NotificationReceipt;
import org.example.BenhAnDienTu.notification.domain.NotificationChannel;

/**
 * Demo service BEFORE Factory Pattern.
 *
 * <p>Point 1 - Before Factory: this service depends on if-else branching by channel type, so it gets
 * harder to maintain when channels increase.
 *
 * <p>Point 2 - After Factory (current real design): each channel becomes its own class implementing
 * NotificationChannel (for example SystemNotificationChannel).
 *
 * <p>Point 3 - With Factory: adding a new channel mostly means adding one new channel class, while
 * this legacy service must be modified directly.
 */
public class NotificationService {

  public NotificationReceipt send(NotificationCommand command) {
    String normalizedChannel = NotificationChannel.normalizeChannel(command.channel());
    String messageId = UUID.randomUUID().toString();

    // Before Factory: central if-else chooses implementation directly in service.
    if ("SYSTEM".equals(normalizedChannel)) {
      dispatchSystem(messageId, command);
    } else if ("EMAIL".equals(normalizedChannel)) {
      dispatchEmail(messageId, command);
    } else if ("SMS".equals(normalizedChannel)) {
      dispatchSms(messageId, command);
    } else {
      throw new IllegalArgumentException("Unsupported notification channel: " + normalizedChannel);
    }

    return new NotificationReceipt(messageId, "QUEUED", Instant.now());
  }

  private void dispatchSystem(String messageId, NotificationCommand command) {
    // Mock logic only: real project now delegates to SystemNotificationChannel.
  }

  private void dispatchEmail(String messageId, NotificationCommand command) {
    // Mock logic only: adding EMAIL required editing this service.
  }

  private void dispatchSms(String messageId, NotificationCommand command) {
    // Mock logic only: adding SMS required editing this service.
  }

  // Example extension pain (before Factory):
  // 1) add new else-if block (e.g., PUSH) in send(...)
  // 2) add new dispatchPush(...) method in this class
  // => existing service must be changed whenever a new channel is added.
}
