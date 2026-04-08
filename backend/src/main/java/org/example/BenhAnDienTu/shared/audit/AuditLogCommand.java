package org.example.BenhAnDienTu.shared.audit;

public record AuditLogCommand(
    String requestId,
    String actorUsername,
    String actorRole,
    String moduleName,
    String actionName,
    String targetType,
    String targetId,
    String outcome,
    String httpMethod,
    String requestPath,
    int statusCode,
    String ipAddress,
    String userAgent,
    String details) {}
