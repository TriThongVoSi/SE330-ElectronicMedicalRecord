package org.example.BenhAnDienTu.shared.audit;

public interface AuditLogService {

  void write(AuditLogCommand command);
}
