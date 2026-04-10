package org.example.BenhAnDienTu.identity.application;

import java.util.List;
import java.util.Set;
import org.example.BenhAnDienTu.identity.domain.DefaultPermissionStrategy;
import org.example.BenhAnDienTu.identity.domain.RolePermissionStrategy;
import org.springframework.stereotype.Service;

@Service
public class RolePermissionResolver {

  private final List<RolePermissionStrategy> strategies;
  private final DefaultPermissionStrategy defaultPermissionStrategy;

  public RolePermissionResolver(
      List<RolePermissionStrategy> strategies,
      DefaultPermissionStrategy defaultPermissionStrategy) {
    this.strategies = List.copyOf(strategies);
    this.defaultPermissionStrategy = defaultPermissionStrategy;
  }

  public Set<String> permissionsForRole(String role) {
    return resolve(role).permissions();
  }

  public boolean isAllowed(String role, String method, String path) {
    return resolve(role).isAllowed(method, path);
  }

  private RolePermissionStrategy resolve(String role) {
    String normalizedRole = RolePermissionStrategy.normalizeRole(role);
    return strategies.stream()
        .filter(strategy -> strategy.supports(normalizedRole))
        .findFirst()
        .orElse(defaultPermissionStrategy);
  }
}
