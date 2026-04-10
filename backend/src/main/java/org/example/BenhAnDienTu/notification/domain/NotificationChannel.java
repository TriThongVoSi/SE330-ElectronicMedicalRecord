package org.example.BenhAnDienTu.notification.domain;

import java.util.Locale;

/** Domain contract for notification delivery channels. */
public interface NotificationChannel {

  boolean supports(String channelType);

  void dispatch(String messageId, NotificationEnvelope envelope);

  static String normalizeChannel(String channelType) {
    if (channelType == null) {
      return "";
    }
    return channelType.trim().toUpperCase(Locale.ROOT);
  }
}
