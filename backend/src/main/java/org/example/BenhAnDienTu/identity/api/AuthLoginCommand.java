package org.example.BenhAnDienTu.identity.api;

/** Login command used by the identity boundary. */
public record AuthLoginCommand(String identifier, String password) {}
