package org.example.BenhAnDienTu.identity.api;

/** Access/refresh token bundle returned by identity APIs. */
public record AuthSessionView(String accessToken, String refreshToken, AuthUserView user) {}
