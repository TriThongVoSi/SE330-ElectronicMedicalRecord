package org.example.BenhAnDienTu.identity.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.example.BenhAnDienTu.identity.api.AuthUserView;
import org.example.BenhAnDienTu.identity.api.IdentityApi;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiAuthenticationFilter extends OncePerRequestFilter {

  public static final String ACTOR_USERNAME_ATTRIBUTE = "actorUsername";
  public static final String ACTOR_ROLE_ATTRIBUTE = "actorRole";
  public static final String ACTOR_ID_ATTRIBUTE = "actorId";
  public static final String ACTOR_MUST_CHANGE_PASSWORD_ATTRIBUTE = "actorMustChangePassword";

  private final IdentityApi identityApi;
  private final ObjectMapper objectMapper;

  public ApiAuthenticationFilter(IdentityApi identityApi, ObjectMapper objectMapper) {
    this.identityApi = identityApi;
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    if (HttpMethod.OPTIONS.matches(request.getMethod())) {
      return true;
    }

    String path = request.getRequestURI();
    if (!path.startsWith("/api/")) {
      return true;
    }
    return path.startsWith("/api/ping")
        || path.startsWith("/api/auth/login")
        || path.startsWith("/api/auth/refresh")
        || path.startsWith("/api/auth/logout")
        || path.startsWith("/api/v1/auth/sign-in")
        || path.startsWith("/api/v1/auth/refresh")
        || path.startsWith("/api/v1/auth/sign-out")
        || path.startsWith("/api/v1/auth/sign-up")
        || path.startsWith("/api/v1/auth/forgot-password")
        || path.startsWith("/api/v1/auth/introspect");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    SecurityContextHolder.clearContext();

    String authorization = request.getHeader("Authorization");
    String accessToken = extractBearerToken(authorization);
    if (accessToken == null) {
      unauthorized(response, request, "AUTH_MISSING_TOKEN", "Missing or invalid Bearer token.");
      return;
    }

    AuthUserView user = identityApi.findUserByAccessToken(accessToken).orElse(null);
    if (user == null) {
      unauthorized(response, request, "AUTH_INVALID_TOKEN", "Access token is invalid or expired.");
      return;
    }

    request.setAttribute(ACTOR_ID_ATTRIBUTE, user.id());
    request.setAttribute(ACTOR_USERNAME_ATTRIBUTE, user.username());
    request.setAttribute(ACTOR_ROLE_ATTRIBUTE, user.role());
    request.setAttribute(ACTOR_MUST_CHANGE_PASSWORD_ATTRIBUTE, user.mustChangePassword());

    if (user.mustChangePassword() && shouldBlockBusinessRequest(request.getRequestURI())) {
      forbiddenMustChangePassword(
          response,
          request,
          "MUST_CHANGE_PASSWORD_REQUIRED",
          "You must change password before accessing business APIs.");
      return;
    }

    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(
            user.username(), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role()))));
    SecurityContextHolder.setContext(securityContext);

    try {
      filterChain.doFilter(request, response);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private static String extractBearerToken(String authorization) {
    if (authorization == null) {
      return null;
    }
    if (!authorization.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
      return null;
    }
    String token = authorization.substring("Bearer ".length()).trim();
    return token.isEmpty() ? null : token;
  }

  private void unauthorized(
      HttpServletResponse response, HttpServletRequest request, String code, String message)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    String requestId = resolveRequestId(response);

    Map<String, Object> body =
        Map.of(
            "timestamp",
            Instant.now().toString(),
            "status",
            HttpServletResponse.SC_UNAUTHORIZED,
            "error",
            "Unauthorized",
            "code",
            code,
            "message",
            message,
            "path",
            request.getRequestURI(),
            "requestId",
            requestId);
    response.getWriter().write(objectMapper.writeValueAsString(body));
  }

  private static boolean shouldBlockBusinessRequest(String path) {
    if (path == null || path.isBlank()) {
      return false;
    }

    if (path.startsWith("/api/auth/") || path.startsWith("/api/v1/auth/")) {
      return false;
    }

    return path.startsWith("/api/");
  }

  private void forbiddenMustChangePassword(
      HttpServletResponse response, HttpServletRequest request, String code, String message)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    String requestId = resolveRequestId(response);

    Map<String, Object> body =
        Map.of(
            "timestamp",
            Instant.now().toString(),
            "status",
            HttpServletResponse.SC_FORBIDDEN,
            "error",
            "Forbidden",
            "code",
            code,
            "message",
            message,
            "path",
            request.getRequestURI(),
            "requestId",
            requestId);
    response.getWriter().write(objectMapper.writeValueAsString(body));
  }

  private static String resolveRequestId(HttpServletResponse response) {
    String requestId = response.getHeader("X-Request-Id");
    return StringUtils.hasText(requestId) ? requestId : "n/a";
  }
}
