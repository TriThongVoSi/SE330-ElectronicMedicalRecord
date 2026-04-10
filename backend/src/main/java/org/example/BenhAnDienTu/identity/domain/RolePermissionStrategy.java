package org.example.BenhAnDienTu.identity.domain;

import java.util.Locale;
import java.util.Set;

/** Domain strategy for role-based permissions and API authorization checks. */
public interface RolePermissionStrategy {

  boolean supports(String role);

  Set<String> permissions();

  boolean isAllowed(String method, String path);

  static String normalizeRole(String role) {
    if (role == null) {
      return "";
    }
    return role.trim().toUpperCase(Locale.ROOT);
  }
}
