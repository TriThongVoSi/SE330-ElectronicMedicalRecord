package org.example.BenhAnDienTu.shared.audit;

import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Primary
@ConditionalOnBean(JdbcTemplate.class)
public class JdbcAuditLogService implements AuditLogService {

  private static final String INSERT_SQL =
      """
      INSERT INTO audit_logs (
          event_id,
          request_id,
          actor_username,
          actor_role,
          module_name,
          action_name,
          target_type,
          target_id,
          outcome,
          http_method,
          request_path,
          status_code,
          ip_address,
          user_agent,
          details
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """;

  private final JdbcTemplate jdbcTemplate;

  public JdbcAuditLogService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void write(AuditLogCommand command) {
    jdbcTemplate.update(
        INSERT_SQL,
        UUID.randomUUID().toString(),
        trimToNull(command.requestId(), 64),
        trimToNull(command.actorUsername(), 100),
        trimToNull(command.actorRole(), 40),
        trimToNull(command.moduleName(), 50),
        trimToNull(command.actionName(), 120),
        trimToNull(command.targetType(), 80),
        trimToNull(command.targetId(), 80),
        trimToNull(command.outcome(), 20),
        trimToNull(command.httpMethod(), 10),
        trimToNull(command.requestPath(), 255),
        command.statusCode(),
        trimToNull(command.ipAddress(), 45),
        trimToNull(command.userAgent(), 255),
        command.details());
  }

  private static String trimToNull(String value, int maxLength) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
  }
}
