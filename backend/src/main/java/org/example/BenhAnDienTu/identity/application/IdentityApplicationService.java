package org.example.BenhAnDienTu.identity.application;

import java.util.Optional;
import org.example.BenhAnDienTu.identity.api.AuthLoginCommand;
import org.example.BenhAnDienTu.identity.api.AuthRefreshCommand;
import org.example.BenhAnDienTu.identity.api.AuthSessionView;
import org.example.BenhAnDienTu.identity.api.AuthUserView;
import org.example.BenhAnDienTu.identity.api.IdentityApi;
import org.example.BenhAnDienTu.identity.api.IdentityProfileView;
import org.example.BenhAnDienTu.identity.domain.IdentityAccount;
import org.example.BenhAnDienTu.identity.infrastructure.IdentityDirectoryAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IdentityApplicationService implements IdentityApi {

  private final IdentityDirectoryAdapter directoryAdapter;
  private final IdentityJwtTokenService jwtTokenService;
  private final PasswordEncoder passwordEncoder;

  public IdentityApplicationService(
      IdentityDirectoryAdapter directoryAdapter,
      IdentityJwtTokenService jwtTokenService,
      PasswordEncoder passwordEncoder) {
    this.directoryAdapter = directoryAdapter;
    this.jwtTokenService = jwtTokenService;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public AuthSessionView login(AuthLoginCommand command) {
    String identifier = command.identifier() == null ? "" : command.identifier().trim();
    if (identifier.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username or email is required.");
    }

    IdentityAccount account =
        directoryAdapter
            .findByIdentifier(identifier)
            .filter(this::isActiveAccount)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid username or password."));

    if (account.password() == null || account.password().isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
    }

    boolean passwordMatches = passwordEncoder.matches(command.password(), account.password());
    if (!passwordMatches) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
    }

    AuthUserView user = toAuthUser(account);
    IdentityJwtTokenService.TokenPair tokenPair = jwtTokenService.issueTokenPair(user);
    return new AuthSessionView(tokenPair.accessToken(), tokenPair.refreshToken(), user);
  }

  @Override
  public AuthSessionView refresh(AuthRefreshCommand command) {
    IdentityJwtTokenService.TokenClaims claims =
        jwtTokenService.verifyRefreshToken(command.refreshToken());

    IdentityAccount account =
        directoryAdapter
            .findByActorId(claims.actorId())
            .filter(this::isActiveAccount)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Refresh token is invalid."));

    jwtTokenService.invalidateToken(claims);

    AuthUserView user = toAuthUser(account);
    IdentityJwtTokenService.TokenPair tokenPair = jwtTokenService.issueTokenPair(user);
    return new AuthSessionView(tokenPair.accessToken(), tokenPair.refreshToken(), user);
  }

  @Override
  public Optional<AuthUserView> findUserByAccessToken(String accessToken) {
    try {
      IdentityJwtTokenService.TokenClaims claims = jwtTokenService.verifyAccessToken(accessToken);
      return directoryAdapter
          .findByActorId(claims.actorId())
          .filter(this::isActiveAccount)
          .map(IdentityApplicationService::toAuthUser);
    } catch (RuntimeException exception) {
      return Optional.empty();
    }
  }

  @Override
  public void logout(String refreshToken) {
    try {
      IdentityJwtTokenService.TokenClaims claims = jwtTokenService.verifyRefreshToken(refreshToken);
      jwtTokenService.invalidateToken(claims);
    } catch (RuntimeException ignored) {
      // Keep logout idempotent.
    }
  }

  @Override
  public Optional<IdentityProfileView> findProfileByPrincipal(String principal) {
    return directoryAdapter
        .findByPrincipal(principal)
        .map(
            account ->
                new IdentityProfileView(
                    account.actorId(), account.principal(), account.permissions()));
  }

  @Override
  public boolean hasPermission(String actorId, String permissionCode) {
    return directoryAdapter
        .findByActorId(actorId)
        .map(account -> account.permissions().contains(permissionCode))
        .orElse(false);
  }

  private boolean isActiveAccount(IdentityAccount account) {
    return account.active() && "ACTIVE".equalsIgnoreCase(account.status());
  }

  private static AuthUserView toAuthUser(IdentityAccount account) {
    return new AuthUserView(
        account.actorId(),
        account.principal(),
        account.fullName(),
        account.email(),
        account.role(),
        account.mustChangePassword());
  }
}
