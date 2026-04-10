package org.example.BenhAnDienTu.identity.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.example.BenhAnDienTu.identity.application.RolePermissionResolver;
import org.example.BenhAnDienTu.shared.logging.RequestCorrelationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiAuthorizationFilter extends OncePerRequestFilter {

  private final ObjectMapper objectMapper;
  private final RolePermissionResolver rolePermissionResolver;

  public ApiAuthorizationFilter(
      ObjectMapper objectMapper, RolePermissionResolver rolePermissionResolver) {
    this.objectMapper = objectMapper;
    this.rolePermissionResolver = rolePermissionResolver;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return !path.startsWith("/api/")
        || HttpMethod.OPTIONS.matches(request.getMethod())
        || path.startsWith("/api/ping")
        || path.startsWith("/api/auth/")
        || path.startsWith("/api/v1/auth/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String role = (String) request.getAttribute(ApiAuthenticationFilter.ACTOR_ROLE_ATTRIBUTE);
    if (!StringUtils.hasText(role)) {
      forbidden(response, request, "AUTH_FORBIDDEN", "User role is missing.");
      return;
    }

    if (!rolePermissionResolver.isAllowed(role, request.getMethod(), request.getRequestURI())) {
      forbidden(response, request, "AUTH_FORBIDDEN", "User role is not allowed for this endpoint.");
      return;
    }

    filterChain.doFilter(request, response);
  }

  private void forbidden(
      HttpServletResponse response, HttpServletRequest request, String code, String message)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    String requestId = response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER);
    if (!StringUtils.hasText(requestId)) {
      requestId = "n/a";
    }

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
}
