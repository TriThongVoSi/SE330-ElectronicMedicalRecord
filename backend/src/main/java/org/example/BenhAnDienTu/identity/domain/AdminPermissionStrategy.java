package org.example.BenhAnDienTu.identity.domain;

import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AdminPermissionStrategy implements RolePermissionStrategy {

  private static final String ROLE_ADMIN = "ADMIN";
  private static final Set<String> ADMIN_PERMISSIONS =
      Set.of(
          "STAFF_READ",
          "STAFF_WRITE",
          "PATIENT_READ",
          "PATIENT_WRITE",
          "APPOINTMENT_READ",
          "APPOINTMENT_WRITE",
          "CATALOG_READ",
          "CATALOG_WRITE",
          "PRESCRIPTION_READ",
          "PRESCRIPTION_WRITE",
          "REPORTING_READ");

  @Override
  public boolean supports(String role) {
    return ROLE_ADMIN.equals(RolePermissionStrategy.normalizeRole(role));
  }

  @Override
  public Set<String> permissions() {
    return ADMIN_PERMISSIONS;
  }

  @Override
  public boolean isAllowed(String method, String path) {
    return true;
  }
}
