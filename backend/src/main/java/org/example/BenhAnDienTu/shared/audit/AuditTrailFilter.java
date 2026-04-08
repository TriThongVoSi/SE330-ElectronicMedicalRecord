package org.example.BenhAnDienTu.shared.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.Set;
import org.example.BenhAnDienTu.shared.logging.RequestCorrelationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class AuditTrailFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(AuditTrailFilter.class);
  private static final Set<String> TRACKED_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
  private static final String UNKNOWN = "unknown";
  private static final String ACTOR_USERNAME_ATTRIBUTE = "actorUsername";
  private static final String ACTOR_ROLE_ATTRIBUTE = "actorRole";

  private final AuditLogService auditLogService;

  public AuditTrailFilter(AuditLogService auditLogService) {
    this.auditLogService = auditLogService;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return !path.startsWith("/api/") || path.startsWith("/api/ping") || path.startsWith("/api/ops");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    int statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

    try {
      filterChain.doFilter(request, response);
      statusCode = response.getStatus();
    } catch (Exception exception) {
      statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
      throw exception;
    } finally {
      if (shouldCapture(request)) {
        writeAuditLog(request, response, statusCode);
      }
    }
  }

  private boolean shouldCapture(HttpServletRequest request) {
    return TRACKED_METHODS.contains(request.getMethod());
  }

  private void writeAuditLog(
      HttpServletRequest request, HttpServletResponse response, int statusCode) {
    String path = request.getRequestURI();
    String moduleName = resolveModuleName(path);
    String[] segments = splitPath(path);
    String targetType = segments.length >= 2 ? segments[1] : UNKNOWN;
    String targetId = segments.length >= 3 ? segments[2] : null;
    String requestId = response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER);

    Principal principal = request.getUserPrincipal();
    String actorUsername = (String) request.getAttribute(ACTOR_USERNAME_ATTRIBUTE);
    if (!StringUtils.hasText(actorUsername)) {
      actorUsername = request.getHeader("X-Actor-Username");
    }
    if (!StringUtils.hasText(actorUsername) && principal != null) {
      actorUsername = principal.getName();
    }

    String actorRole = (String) request.getAttribute(ACTOR_ROLE_ATTRIBUTE);
    if (!StringUtils.hasText(actorRole)) {
      actorRole = request.getHeader("X-Actor-Role");
    }
    String outcome = statusCode >= 400 ? "FAILURE" : "SUCCESS";
    String details = request.getQueryString() == null ? null : "query=" + request.getQueryString();

    AuditLogCommand command =
        new AuditLogCommand(
            requestId,
            actorUsername,
            actorRole,
            moduleName,
            request.getMethod() + " " + path,
            targetType,
            targetId,
            outcome,
            request.getMethod(),
            path,
            statusCode,
            resolveClientIp(request),
            request.getHeader("User-Agent"),
            details);

    try {
      auditLogService.write(command);
    } catch (Exception exception) {
      log.warn("Failed to persist audit log for {} {}", request.getMethod(), path, exception);
    }
  }

  private static String[] splitPath(String path) {
    String normalized = path.startsWith("/") ? path.substring(1) : path;
    return normalized.split("/");
  }

  private static String resolveClientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (StringUtils.hasText(forwardedFor)) {
      int separator = forwardedFor.indexOf(',');
      return separator >= 0 ? forwardedFor.substring(0, separator).trim() : forwardedFor.trim();
    }
    return request.getRemoteAddr();
  }

  private static String resolveModuleName(String path) {
    String[] segments = splitPath(path);
    if (segments.length < 2) {
      return "system";
    }

    String context = segments[1];
    return switch (context) {
      case "auth" -> "identity";
      case "staff" -> "staff";
      case "patients" -> "patient";
      case "services", "drugs", "catalog" -> "catalog";
      case "appointments" -> "appointment";
      case "prescriptions" -> "prescription";
      case "reporting", "dashboard" -> "reporting";
      case "notifications" -> "notification";
      default -> "system";
    };
  }
}
