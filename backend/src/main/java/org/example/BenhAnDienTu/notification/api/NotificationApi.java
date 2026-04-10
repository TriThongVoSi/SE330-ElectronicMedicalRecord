package org.example.BenhAnDienTu.notification.api;

/** Public contract for outbound notification boundary. */
public interface NotificationApi {

  NotificationReceipt send(NotificationCommand command);
}
