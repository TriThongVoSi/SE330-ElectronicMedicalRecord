package org.example.BenhAnDienTu.identity.api;

/** Refresh command used by the identity boundary. */
public record AuthRefreshCommand(String refreshToken) {}
