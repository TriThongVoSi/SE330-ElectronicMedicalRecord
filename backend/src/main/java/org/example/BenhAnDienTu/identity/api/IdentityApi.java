package org.example.BenhAnDienTu.identity.api;

import java.util.Optional;

/** Public contract for identity-related lookups and authorization checks. */
public interface IdentityApi {

  AuthSessionView login(AuthLoginCommand command);

  AuthSessionView refresh(AuthRefreshCommand command);

  Optional<AuthUserView> findUserByAccessToken(String accessToken);

  void logout(String refreshToken);

  Optional<IdentityProfileView> findProfileByPrincipal(String principal);

  boolean hasPermission(String actorId, String permissionCode);
}
