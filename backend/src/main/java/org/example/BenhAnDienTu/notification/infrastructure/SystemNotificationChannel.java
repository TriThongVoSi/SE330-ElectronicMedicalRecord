package org.example.BenhAnDienTu.notification.infrastructure;

import org.example.BenhAnDienTu.notification.domain.NotificationChannel;
import org.example.BenhAnDienTu.notification.domain.NotificationEnvelope;
import org.springframework.stereotype.Component;

@Component
public class SystemNotificationChannel implements NotificationChannel {

  private static final String CHANNEL_SYSTEM = "SYSTEM";

  private final NotificationOutboxAdapter outboxAdapter;

  public SystemNotificationChannel(NotificationOutboxAdapter outboxAdapter) {
    this.outboxAdapter = outboxAdapter;
  }

  @Override
  public boolean supports(String channelType) {
    return CHANNEL_SYSTEM.equals(NotificationChannel.normalizeChannel(channelType));
  }

  @Override
  public void dispatch(String messageId, NotificationEnvelope envelope) {
    outboxAdapter.enqueue(messageId, envelope);
  }
}
