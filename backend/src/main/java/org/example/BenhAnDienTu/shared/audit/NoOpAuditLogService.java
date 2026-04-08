package org.example.BenhAnDienTu.shared.audit;

import org.springframework.stereotype.Component;

@Component
public class NoOpAuditLogService implements AuditLogService {

  @Override
  public void write(AuditLogCommand command) {
    // Intentionally no-op when audit persistence is not available.
  }
}
