package org.example.BenhAnDienTu.notification.api;

/** Boundary command for dispatching a notification. */
public record NotificationCommand(String channel, String subject, String body, String recipient) {}
