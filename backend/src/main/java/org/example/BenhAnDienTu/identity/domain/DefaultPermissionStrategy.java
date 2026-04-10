package org.example.BenhAnDienTu.identity.domain;

import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DefaultPermissionStrategy implements RolePermissionStrategy {

  private static final String ROLE_USER = "USER";

  @Override
  public boolean supports(String role) {
    return ROLE_USER.equals(RolePermissionStrategy.normalizeRole(role));
  }

  @Override
  public Set<String> permissions() {
    return Set.of();
  }

  @Override
  public boolean isAllowed(String method, String path) {
    return false;
  }
}
