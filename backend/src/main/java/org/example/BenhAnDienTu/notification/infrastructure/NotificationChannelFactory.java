package org.example.BenhAnDienTu.notification.infrastructure;

import java.util.List;
import org.example.BenhAnDienTu.notification.domain.NotificationChannel;
import org.springframework.stereotype.Component;

@Component
public class NotificationChannelFactory {

  private final List<NotificationChannel> channels;

  public NotificationChannelFactory(List<NotificationChannel> channels) {
    this.channels = List.copyOf(channels);
  }

  public NotificationChannel resolve(String channelType) {
    String normalizedChannel = NotificationChannel.normalizeChannel(channelType);
    return channels.stream()
        .filter(channel -> channel.supports(normalizedChannel))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Unsupported notification channel: " + normalizedChannel));
  }
}
