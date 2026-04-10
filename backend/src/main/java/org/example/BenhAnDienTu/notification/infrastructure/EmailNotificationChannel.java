package org.example.BenhAnDienTu.notification.infrastructure;

import org.example.BenhAnDienTu.notification.domain.NotificationChannel;
import org.example.BenhAnDienTu.notification.domain.NotificationEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class EmailNotificationChannel implements NotificationChannel {

  private static final Logger LOGGER = LoggerFactory.getLogger(EmailNotificationChannel.class);
  private static final String CHANNEL_EMAIL = "EMAIL";

  private final NotificationOutboxAdapter outboxAdapter;
  private final JavaMailSender mailSender;

  @Value("${spring.mail.host:}")
  private String smtpHost;

  @Value("${app.mail.from:no-reply@example.com}")
  private String senderEmail;

  @Value("${app.mail.enabled:true}")
  private boolean mailEnabled;

  @Value("${app.mail.dev-fallback-enabled:true}")
  private boolean devFallbackEnabled;

  public EmailNotificationChannel(
      NotificationOutboxAdapter outboxAdapter, JavaMailSender mailSender) {
    this.outboxAdapter = outboxAdapter;
    this.mailSender = mailSender;
  }

  @Override
  public boolean supports(String channelType) {
    return CHANNEL_EMAIL.equals(NotificationChannel.normalizeChannel(channelType));
  }

  @Override
  public void dispatch(String messageId, NotificationEnvelope envelope) {
    outboxAdapter.enqueue(messageId, envelope);

    if (!StringUtils.hasText(envelope.recipient())) {
      LOGGER.warn("Email notification has no recipient. messageId={}", messageId);
      return;
    }

    if (!mailEnabled) {
      LOGGER.info("Mail is disabled by configuration. messageId={}", messageId);
      return;
    }

    if (!StringUtils.hasText(smtpHost)) {
      LOGGER.info(
          "SMTP is not configured. Email delivery skipped safely. messageId={}, recipient={}",
          messageId,
          maskRecipient(envelope.recipient()));
      return;
    }

    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(senderEmail);
    message.setTo(envelope.recipient());
    message.setSubject(envelope.subject());
    message.setText(envelope.body());

    try {
      mailSender.send(message);
      LOGGER.info(
          "Email delivered. messageId={}, recipient={}",
          messageId,
          maskRecipient(envelope.recipient()));
    } catch (MailException exception) {
      if (devFallbackEnabled) {
        LOGGER.warn(
            "Email delivery failed. Using safe dev fallback. messageId={}, recipient={}",
            messageId,
            maskRecipient(envelope.recipient()),
            exception);
        return;
      }
      throw exception;
    }
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
