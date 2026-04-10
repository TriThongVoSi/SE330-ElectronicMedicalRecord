package org.example.BenhAnDienTu.identity.infrastructure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.example.BenhAnDienTu.identity.api.AuthLoginCommand;
import org.example.BenhAnDienTu.identity.api.AuthRefreshCommand;
import org.example.BenhAnDienTu.identity.api.AuthSessionView;
import org.example.BenhAnDienTu.identity.api.AuthUserView;
import org.example.BenhAnDienTu.identity.api.IdentityApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/auth")
public class IdentityController {

  private final IdentityApi identityApi;

  public IdentityController(IdentityApi identityApi) {
    this.identityApi = identityApi;
  }

  @PostMapping("/login")
  public AuthSessionView login(@Valid @RequestBody LoginRequest request) {
    return identityApi.login(new AuthLoginCommand(request.resolveIdentifier(), request.password()));
  }

  @PostMapping("/refresh")
  public AuthSessionView refresh(@Valid @RequestBody RefreshRequest request) {
    return identityApi.refresh(new AuthRefreshCommand(request.refreshToken()));
  }

  @GetMapping("/me")
  public AuthUserView me(
      @RequestHeader(name = "Authorization", required = false) String authorization) {
    String accessToken = extractBearerToken(authorization);
    return identityApi
        .findUserByAccessToken(accessToken)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access token is invalid."));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
    identityApi.logout(request.resolveToken());
    return ResponseEntity.noContent().build();
  }

  private static String extractBearerToken(String authorization) {
    if (authorization == null || authorization.isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Authorization header.");
    }
    if (!authorization.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Authorization header must use Bearer token.");
    }
    String token = authorization.substring("Bearer ".length()).trim();
    if (token.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access token is missing.");
    }
    return token;
  }

  public record LoginRequest(
      String identifier, String username, String email, @NotBlank String password) {

    String resolveIdentifier() {
      if (identifier != null && !identifier.isBlank()) {
        return identifier.trim();
      }
      if (username != null && !username.isBlank()) {
        return username.trim();
      }
      if (email != null && !email.isBlank()) {
        return email.trim();
      }
      return "";
    }
  }

  public record RefreshRequest(@NotBlank String refreshToken) {}

  public record LogoutRequest(String refreshToken, String token) {

    String resolveToken() {
      if (refreshToken != null && !refreshToken.isBlank()) {
        return refreshToken.trim();
      }
      if (token != null && !token.isBlank()) {
        return token.trim();
      }
      return "";
    }
  }
}
