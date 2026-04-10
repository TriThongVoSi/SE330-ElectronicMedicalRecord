package org.example.BenhAnDienTu.identity.infrastructure;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.example.BenhAnDienTu.identity.api.AuthUserView;
import org.springframework.stereotype.Component;

@Component
public class IdentitySessionStore {

  private final Map<String, AuthUserView> accessIndex = new ConcurrentHashMap<>();
  private final Map<String, AuthUserView> refreshIndex = new ConcurrentHashMap<>();
  private final Map<String, String> refreshToAccess = new ConcurrentHashMap<>();

  public SessionTokenBundle createSession(AuthUserView user) {
    String accessToken = token("atk");
    String refreshToken = token("rtk");

    accessIndex.put(accessToken, user);
    refreshIndex.put(refreshToken, user);
    refreshToAccess.put(refreshToken, accessToken);

    return new SessionTokenBundle(accessToken, refreshToken, user);
  }

  public Optional<AuthUserView> findByAccessToken(String accessToken) {
    return Optional.ofNullable(accessIndex.get(accessToken));
  }

  public Optional<SessionTokenBundle> refresh(String refreshToken) {
    AuthUserView user = refreshIndex.get(refreshToken);
    if (user == null) {
      return Optional.empty();
    }

    String previousAccessToken = refreshToAccess.remove(refreshToken);
    if (previousAccessToken != null) {
      accessIndex.remove(previousAccessToken);
    }
    refreshIndex.remove(refreshToken);

    return Optional.of(createSession(user));
  }

  public void revokeByRefreshToken(String refreshToken) {
    String accessToken = refreshToAccess.remove(refreshToken);
    if (accessToken != null) {
      accessIndex.remove(accessToken);
    }
    refreshIndex.remove(refreshToken);
  }

  private static String token(String prefix) {
    return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
  }

  public record SessionTokenBundle(String accessToken, String refreshToken, AuthUserView user) {}
}
